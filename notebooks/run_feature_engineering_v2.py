import pandas as pd
import numpy as np
from pathlib import Path
from sklearn.preprocessing import MinMaxScaler

DATA_PATH = Path('/Users/abderrahim_boussyf/MANTIS/data/raw/NASA_CMAPSS')
PROCESSED_PATH = Path('/Users/abderrahim_boussyf/MANTIS/data/processed')
PROCESSED_PATH.mkdir(parents=True, exist_ok=True)

cols = ['unit_number', 'time_cycles'] + ['setting_1', 'setting_2', 'setting_3'] + [f'sensor_{i}' for i in range(1, 22)]

try:
    print(f"Reading from {DATA_PATH}")
    train = pd.read_csv(DATA_PATH / 'train_FD001.txt', sep='\s+', header=None, names=cols)
    test = pd.read_csv(DATA_PATH / 'test_FD001.txt', sep='\s+', header=None, names=cols)
    
    print(f"Train/Test loaded: {train.shape} / {test.shape}")
    
    # RUL calculation
    max_cycles = train.groupby('unit_number')['time_cycles'].transform('max')
    train['RUL'] = max_cycles - train['time_cycles']
    
    # Normalization
    scaler = MinMaxScaler()
    sensor_cols = [c for c in cols if 'sensor' in c]
    train[sensor_cols] = scaler.fit_transform(train[sensor_cols])
    test[sensor_cols] = scaler.transform(test[sensor_cols])
    
    # Rolling features
    print("Computing features...")
    rolled = train.groupby('unit_number')[sensor_cols].rolling(window=20, min_periods=1)
    train_feat = train.join(rolled.mean().add_suffix('_mean')).join(rolled.std().add_suffix('_std')).fillna(0)
    
    rolled_test = test.groupby('unit_number')[sensor_cols].rolling(window=20, min_periods=1)
    test_feat = test.join(rolled_test.mean().add_suffix('_mean')).join(rolled_test.std().add_suffix('_std')).fillna(0)
    
    # Save
    train_feat.to_csv(PROCESSED_PATH / 'train_FD001_features.csv', index=False)
    test_feat.to_csv(PROCESSED_PATH / 'test_FD001_features.csv', index=False)
    print(f"Saved to {PROCESSED_PATH}")

except Exception as e:
    print(f"Error: {e}")
