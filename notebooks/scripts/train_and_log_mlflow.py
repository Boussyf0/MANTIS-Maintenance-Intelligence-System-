import pandas as pd
import sys
import os
import mlflow
import mlflow.sklearn
from pathlib import Path
from pyod.models.iforest import IForest
from sklearn.metrics import classification_report, roc_auc_score

# Configure MLflow
MLFLOW_TRACKING_URI = "http://localhost:5002"
mlflow.set_tracking_uri(MLFLOW_TRACKING_URI)
experiment_name = "MANTIS_Anomaly_Detection"
mlflow.set_experiment(experiment_name)


def log(msg):
    print(msg)


try:
    log(f"Connecting to MLflow at {MLFLOW_TRACKING_URI}...")

    # 1. LOAD DATA
    DATA_PATH = Path("data/raw/NASA_CMAPSS")
    cols = (
        ["unit_number", "time_cycles"] + ["setting_1", "setting_2", "setting_3"] + [f"sensor_{i}" for i in range(1, 22)]
    )

    log(f"Loading data from {DATA_PATH}...")
    if not (DATA_PATH / "train_FD001.txt").exists():
        log(f"Data file not found at {DATA_PATH / 'train_FD001.txt'}. Please ensure data is present.")
        sys.exit(1)

    train = pd.read_csv(DATA_PATH / "train_FD001.txt", sep=r"\s+", header=None, names=cols)

    # RUL Calculation
    max_cycles = train.groupby("unit_number")["time_cycles"].transform("max")
    train["RUL"] = max_cycles - train["time_cycles"]

    # 2. FEATURE SELECTION
    USEFUL_SENSORS = [
        "sensor_2",
        "sensor_3",
        "sensor_4",
        "sensor_7",
        "sensor_8",
        "sensor_9",
        "sensor_11",
        "sensor_12",
        "sensor_13",
        "sensor_14",
        "sensor_15",
        "sensor_17",
        "sensor_20",
        "sensor_21",
    ]

    # 3. FEATURE ENGINEERING
    train = train.sort_values(["unit_number", "time_cycles"])
    rolled = train.groupby("unit_number")[USEFUL_SENSORS].rolling(window=15, min_periods=1)

    feat_mean = rolled.mean().reset_index(level=0, drop=True).add_suffix("_mean")
    feat_std = rolled.std().reset_index(level=0, drop=True).add_suffix("_std")

    train_feat = train.join(feat_mean).join(feat_std).fillna(0)

    features = [c for c in train_feat.columns if ("_mean" in c) or ("_std" in c)]
    train_feat["label"] = (train_feat["RUL"] <= 30).astype(int)

    X_train_healthy = train_feat[train_feat["label"] == 0][features]

    # START MLFLOW RUN
    with mlflow.start_run(run_name="Isolation_Forest_Optimization"):
        # Parameters
        contamination = 0.05
        n_estimators = 200
        random_state = 42

        mlflow.log_param("contamination", contamination)
        mlflow.log_param("n_estimators", n_estimators)
        mlflow.log_param("random_state", random_state)
        mlflow.log_param("features_count", len(features))

        log(f"Training Isolation Forest (n_estimators={n_estimators}, contamination={contamination})...")
        clf_iso = IForest(
            contamination=contamination,
            n_estimators=n_estimators,
            random_state=random_state,
        )
        clf_iso.fit(X_train_healthy)

        # Predictions
        y_scores = clf_iso.decision_function(train_feat[features])
        roc_auc = roc_auc_score(train_feat["label"], y_scores)

        # Log Metrics
        mlflow.log_metric("roc_auc", roc_auc)
        log(f"Logged ROC_AUC: {roc_auc:.4f}")

        # Log Model
        log("Logging model to MLflow and registering it...")
        mlflow.sklearn.log_model(
            sk_model=clf_iso,
            artifact_path="isolation_forest_model",
            registered_model_name="MANTIS_Anomaly_Detector",
        )

        log("Run completed successfully. Check localhost:5002")

except Exception as e:
    import traceback

    log(f"ERROR: {e}")
    log(traceback.format_exc())
