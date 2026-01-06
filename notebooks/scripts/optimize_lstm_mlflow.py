import pandas as pd
import numpy as np
import torch
import torch.nn as nn
import torch.optim as optim
import mlflow
import mlflow.pytorch
from pathlib import Path
from sklearn.preprocessing import MinMaxScaler
import math
import itertools

# Configure MLflow
MLFLOW_TRACKING_URI = "http://localhost:5002"
mlflow.set_tracking_uri(MLFLOW_TRACKING_URI)
# Use the same experiment so we can compare with the previous run
experiment_name = "MANTIS_RUL_Prediction"
mlflow.set_experiment(experiment_name)


def log(msg):
    print(msg)


# --- MODEL DEFINITION ---
class RULModel(nn.Module):
    def __init__(self, input_size, hidden_size, num_layers, output_size=1):
        super(RULModel, self).__init__()
        self.hidden_size = hidden_size
        self.num_layers = num_layers
        self.lstm = nn.LSTM(input_size, hidden_size, num_layers, batch_first=True)
        self.fc = nn.Linear(hidden_size, output_size)

    def forward(self, x):
        h0 = torch.zeros(self.num_layers, x.size(0), self.hidden_size).to(x.device)
        c0 = torch.zeros(self.num_layers, x.size(0), self.hidden_size).to(x.device)
        out, _ = self.lstm(x, (h0, c0))
        out = self.fc(out[:, -1, :])
        return out


def prepare_data(data_path, sequence_length=30):
    cols = (
        ["unit_number", "time_cycles"] + ["setting_1", "setting_2", "setting_3"] + [f"sensor_{i}" for i in range(1, 22)]
    )
    df = pd.read_csv(data_path, sep=r"\s+", header=None, names=cols)

    max_cycles = df.groupby("unit_number")["time_cycles"].transform("max")
    df["RUL"] = max_cycles - df["time_cycles"]

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

    scaler = MinMaxScaler()
    df[USEFUL_SENSORS] = scaler.fit_transform(df[USEFUL_SENSORS])

    sequences = []
    labels = []

    for unit in df["unit_number"].unique():
        unit_data = df[df["unit_number"] == unit]
        if len(unit_data) < sequence_length:
            continue

        data_array = unit_data[USEFUL_SENSORS].values
        rul_array = unit_data["RUL"].values

        for i in range(len(unit_data) - sequence_length):
            sequences.append(data_array[i : i + sequence_length])
            labels.append(rul_array[i + sequence_length])

    return np.array(sequences), np.array(labels), len(USEFUL_SENSORS)


def train_and_evaluate(params, X_train, y_train, X_val, y_val, input_size):
    hidden_size = params["hidden_size"]
    num_layers = params["num_layers"]
    lr = params["lr"]
    epochs = 4  # Keep fast
    batch_size = 64

    run_name = f"LSTM_H{hidden_size}_L{num_layers}_LR{lr}"

    with mlflow.start_run(run_name=run_name):
        log(f"--- Starting Run: {run_name} ---")
        # Log params
        mlflow.log_param("hidden_size", hidden_size)
        mlflow.log_param("num_layers", num_layers)
        mlflow.log_param("learning_rate", lr)
        mlflow.log_param("epochs", epochs)
        mlflow.log_param("batch_size", batch_size)

        model = RULModel(input_size, hidden_size, num_layers)
        criterion = nn.MSELoss()
        optimizer = optim.Adam(model.parameters(), lr=lr)

        best_rmse = float("inf")

        for epoch in range(epochs):
            model.train()
            permutation = torch.randperm(X_train.size()[0])
            for i in range(0, X_train.size()[0], batch_size):
                indices = permutation[i : i + batch_size]
                batch_x, batch_y = X_train[indices], y_train[indices]

                optimizer.zero_grad()
                outputs = model(batch_x)
                loss = criterion(outputs, batch_y)
                loss.backward()
                optimizer.step()

            # Validation
            model.eval()
            with torch.no_grad():
                val_preds = model(X_val)
                val_loss = criterion(val_preds, y_val)
                rmse = math.sqrt(val_loss.item())
                if rmse < best_rmse:
                    best_rmse = rmse

                mlflow.log_metric("rmse", rmse, step=epoch)

        log(f"Run Finished. Best RMSE: {best_rmse:.4f}")
        mlflow.log_metric("best_rmse", best_rmse)

        # Log model only if it's decent (or just log all for comparison)
        mlflow.pytorch.log_model(model, "lstm_model")


try:
    log(f"Connecting to MLflow at {MLFLOW_TRACKING_URI}...")
    DATA_PATH = Path("data/raw/NASA_CMAPSS/train_FD001.txt")

    log("Loading and preprocessing data...")
    X, y, input_size = prepare_data(DATA_PATH)

    X_tensor = torch.tensor(X, dtype=torch.float32)
    y_tensor = torch.tensor(y, dtype=torch.float32).view(-1, 1)

    train_size = int(len(X) * 0.8)
    X_train, X_val = X_tensor[:train_size], X_tensor[train_size:]
    y_train, y_val = y_tensor[:train_size], y_tensor[train_size:]

    # HYPERPARAMETER GRID
    param_grid = {"hidden_size": [50, 100], "num_layers": [1, 2], "lr": [0.001, 0.01]}

    keys, values = zip(*param_grid.items())
    combinations = [dict(zip(keys, v)) for v in itertools.product(*values)]

    log(f"Starting Grid Search with {len(combinations)} combinations...")

    for i, params in enumerate(combinations):
        log(f"\nProcessing combination {i+1}/{len(combinations)}: {params}")
        train_and_evaluate(params, X_train, y_train, X_val, y_val, input_size)

    log("\nGrid Search Complete. Check MLflow for comparison.")

except Exception as e:
    import traceback

    log(f"ERROR: {e}")
    log(traceback.format_exc())
