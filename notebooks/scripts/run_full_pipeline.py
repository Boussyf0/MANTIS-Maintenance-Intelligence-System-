import pandas as pd
import sys
import os
from pathlib import Path

# Force output to file because stdout is broken
OUTPUT_FILE = Path("final_results.txt")


def log(msg):
    with open(OUTPUT_FILE, "a") as f:
        f.write(msg + "\n")
    print(msg)


# Clear file (or create it)
with open(OUTPUT_FILE, "w") as f:
    f.write("=== EXECUTION STARTED ===\n")
    f.write(f"Python Executable: {sys.executable}\n")

try:
    try:
        from pyod.models.iforest import IForest
        from sklearn.metrics import classification_report, roc_auc_score

        log("Libraries imported successfully.")
    except ImportError as e:
        log(f"IMPORT ERROR: {e}")
        sys.exit(1)

    PROCESSED_PATH = Path("../../data/processed")
    if not PROCESSED_PATH.exists():
        PROCESSED_PATH.mkdir(parents=True, exist_ok=True)

    # 1. GENERATE DATA
    DATA_PATH = Path("../../data/raw/NASA_CMAPSS")
    cols = (
        ["unit_number", "time_cycles"] + ["setting_1", "setting_2", "setting_3"] + [f"sensor_{i}" for i in range(1, 22)]
    )

    log(f"Loading data from {DATA_PATH}...")
    train = pd.read_csv(DATA_PATH / "train_FD001.txt", sep=r"\s+", header=None, names=cols)

    # RUL Calculation
    max_cycles = train.groupby("unit_number")["time_cycles"].transform("max")
    train["RUL"] = max_cycles - train["time_cycles"]

    # Rolled Features
    log("Computing features...")
    sensor_cols = [c for c in cols if "sensor" in c]

    # Ensure train index is sorted by unit/time for correct alignment
    train = train.sort_values(["unit_number", "time_cycles"])

    # Rolling adds 'unit_number' to index. We need to drop it to join back to original DF
    rolled_mean = train.groupby("unit_number")[sensor_cols].rolling(window=20, min_periods=1).mean()
    rolled_mean = rolled_mean.reset_index(level=0, drop=True)

    # Now indices match (both are the original row indices)
    train_feat = train.join(rolled_mean.add_suffix("_mean")).fillna(0)

    # 2. ANOMALY MODEL
    features = [c for c in train_feat.columns if "mean" in c]
    log(f"Training Isolation Forest on {len(features)} features...")

    # Label: RUL <= 30 is Anomaly
    train_feat["label"] = (train_feat["RUL"] <= 30).astype(int)

    # Train on Healthy
    X_train = train_feat[train_feat["label"] == 0][features]
    clf = IForest(contamination=0.1, random_state=42)
    clf.fit(X_train)

    # Eval on All
    y_pred = clf.predict(train_feat[features])
    y_scores = clf.decision_function(train_feat[features])

    # Metrics
    report = classification_report(train_feat["label"], y_pred, target_names=["Normal", "Anomaly"])
    roc = roc_auc_score(train_feat["label"], y_scores)

    log("\n--- CLASSIFICATION REPORT ---")
    log(report)
    log(f"ROC-AUC Score: {roc:.4f}")
    log("=== EXECUTION COMPLETED ===")

except Exception as e:
    import traceback

    log(f"CRITICAL ERROR: {str(e)}")
    log(traceback.format_exc())
