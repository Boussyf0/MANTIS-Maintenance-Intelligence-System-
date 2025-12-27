import pandas as pd
import numpy as np
import torch
import torch.nn as nn
import torch.optim as optim
import mlflow
import mlflow.pytorch
from pathlib import Path
from sklearn.preprocessing import MinMaxScaler
from sklearn.metrics import mean_squared_error
import math

# Configure MLflow
MLFLOW_TRACKING_URI = "http://localhost:5002"
mlflow.set_tracking_uri(MLFLOW_TRACKING_URI)
experiment_name = "MANTIS_RUL_Prediction"
mlflow.set_experiment(experiment_name)

def log(msg):
    print(msg)

# --- MODEL DEFINITION (Copied from services/rul-prediction/src/model.py) ---
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
    cols = ['unit_number', 'time_cycles'] + ['setting_1', 'setting_2', 'setting_3'] + [f'sensor_{i}' for i in range(1, 22)]
    df = pd.read_csv(data_path, sep=r'\s+', header=None, names=cols)
    
    # RUL Calculation
    max_cycles = df.groupby('unit_number')['time_cycles'].transform('max')
    df['RUL'] = max_cycles - df['time_cycles']
    
    # Feature Selection
    USEFUL_SENSORS = ['sensor_2', 'sensor_3', 'sensor_4', 'sensor_7', 'sensor_8', 
                      'sensor_9', 'sensor_11', 'sensor_12', 'sensor_13', 'sensor_14', 
                      'sensor_15', 'sensor_17', 'sensor_20', 'sensor_21']
    
    # MinMax Scaling
    scaler = MinMaxScaler()
    df[USEFUL_SENSORS] = scaler.fit_transform(df[USEFUL_SENSORS])
    
    # Generate Sequences
    sequences = []
    labels = []
    
    for unit in df['unit_number'].unique():
        unit_data = df[df['unit_number'] == unit]
        if len(unit_data) < sequence_length:
            continue
            
        data_array = unit_data[USEFUL_SENSORS].values
        rul_array = unit_data['RUL'].values
        
        for i in range(len(unit_data) - sequence_length):
            sequences.append(data_array[i:i+sequence_length])
            labels.append(rul_array[i+sequence_length])
            
    return np.array(sequences), np.array(labels), len(USEFUL_SENSORS)

try:
    log(f"Connecting to MLflow at {MLFLOW_TRACKING_URI}...")
    DATA_PATH = Path('data/raw/NASA_CMAPSS/train_FD001.txt')
    
    # Hyperparameters
    SEQUENCE_LENGTH = 30
    HIDDEN_SIZE = 50
    NUM_LAYERS = 2
    BATCH_SIZE = 64
    EPOCHS = 2 # Keep it short for demo
    LR = 0.001
    
    log("Loading and preprocessing data...")
    X, y, input_size = prepare_data(DATA_PATH, SEQUENCE_LENGTH)
    
    # Convert to Tensors
    X_tensor = torch.tensor(X, dtype=torch.float32)
    y_tensor = torch.tensor(y, dtype=torch.float32).view(-1, 1)
    
    # Train/Val Split
    train_size = int(len(X) * 0.8)
    X_train, X_val = X_tensor[:train_size], X_tensor[train_size:]
    y_train, y_val = y_tensor[:train_size], y_tensor[train_size:]
    
    with mlflow.start_run(run_name="LSTM_RUL_Training"):
        mlflow.log_param("sequence_length", SEQUENCE_LENGTH)
        mlflow.log_param("hidden_size", HIDDEN_SIZE)
        mlflow.log_param("num_layers", NUM_LAYERS)
        mlflow.log_param("epochs", EPOCHS)
        mlflow.log_param("batch_size", BATCH_SIZE)
        
        model = RULModel(input_size, HIDDEN_SIZE, NUM_LAYERS)
        criterion = nn.MSELoss()
        optimizer = optim.Adam(model.parameters(), lr=LR)
        
        log(f"Training LSTM (Input: {input_size}, Hidden: {HIDDEN_SIZE}, Layers: {NUM_LAYERS})...")
        
        for epoch in range(EPOCHS):
            model.train()
            # Mini-batch training (manual loop for simplicity without DataLoader)
            permutation = torch.randperm(X_train.size()[0])
            for i in range(0, X_train.size()[0], BATCH_SIZE):
                indices = permutation[i:i+BATCH_SIZE]
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
                log(f"Epoch {epoch+1}/{EPOCHS}, Val RMSE: {rmse:.4f}")
                mlflow.log_metric("rmse", rmse, step=epoch)
                
        # Log Model
        log("Logging model to MLflow and registering it...")
        mlflow.pytorch.log_model(
            pytorch_model=model, 
            artifact_path="lstm_model",
            registered_model_name="MANTIS_RUL_Predictor"
        )
        log("Logging Done.")

except Exception as e:
    import traceback
    log(f"ERROR: {e}")
    log(traceback.format_exc())
