import pandas as pd
import sys
import numpy as np
from pathlib import Path

OUTPUT_FILE = Path('final_results.txt')

def log(msg):
    with open(OUTPUT_FILE, 'a') as f:
        f.write(msg + '\n')
    print(msg)

with open(OUTPUT_FILE, 'w') as f:
    f.write("=== OPTIMIZATION STARTED ===\n")

try:
    from pyod.models.iforest import IForest
    from sklearn.metrics import classification_report, roc_auc_score
    from sklearn.ensemble import RandomForestClassifier

    # 1. LOAD DATA
    DATA_PATH = Path('../data/raw/NASA_CMAPSS')
    cols = ['unit_number', 'time_cycles'] + ['setting_1', 'setting_2', 'setting_3'] + [f'sensor_{i}' for i in range(1, 22)]
    
    log(f"Loading data...")
    train = pd.read_csv(DATA_PATH / 'train_FD001.txt', sep=r'\s+', header=None, names=cols)
    
    # RUL Calculation
    max_cycles = train.groupby('unit_number')['time_cycles'].transform('max')
    train['RUL'] = max_cycles - train['time_cycles']
    
    # 2. DATA MINING: FEATURE SELECTION
    # Literature (and my analysis) shows these sensors are NOT useful (constant or noise): 1, 5, 6, 10, 16, 18, 19
    # We keep only the informative ones.
    USEFUL_SENSORS = ['sensor_2', 'sensor_3', 'sensor_4', 'sensor_7', 'sensor_8', 
                      'sensor_9', 'sensor_11', 'sensor_12', 'sensor_13', 'sensor_14', 
                      'sensor_15', 'sensor_17', 'sensor_20', 'sensor_21']
    
    log(f"Applying Feature Selection: Keeping {len(USEFUL_SENSORS)} relevant sensors.")
    
    # 3. FEATURE ENGINEERING: Mean + Deviation (Volatility)
    train = train.sort_values(['unit_number', 'time_cycles'])
    rolled = train.groupby('unit_number')[USEFUL_SENSORS].rolling(window=15, min_periods=1)
    
    # Drop index to align
    feat_mean = rolled.mean().reset_index(level=0, drop=True).add_suffix('_mean')
    feat_std = rolled.std().reset_index(level=0, drop=True).add_suffix('_std')
    
    train_feat = train.join(feat_mean).join(feat_std).fillna(0)
    
    # Select only the engineered features for the model
    features = [c for c in train_feat.columns if ('_mean' in c) or ('_std' in c)]
    log(f"Modeling with {len(features)} features (Mean + Std).")
    
    # Label
    train_feat['label'] = (train_feat['RUL'] <= 30).astype(int)
    
    import matplotlib.pyplot as plt
    from sklearn.metrics import roc_curve, auc

    # Define Healthy Training Data (Common for both Unsupervised Models)
    X_train_healthy = train_feat[train_feat['label'] == 0][features]

    # --- MODEL 0: Default Isolation Forest (Baseline) ---
    # Using defaults: contamination=0.1 (standard default if auto fails)
    clf_iso_default = IForest(contamination=0.1, random_state=42)
    clf_iso_default.fit(X_train_healthy)
    y_scores_iso_default = clf_iso_default.decision_function(train_feat[features])
    
    log("\n--- [Unsupervised] Default Isolation Forest Results ---")
    log(f"ROC-AUC: {roc_auc_score(train_feat['label'], y_scores_iso_default):.4f}")

    # --- MODEL 1: Optimized Isolation Forest (Unsupervised) ---
    # Decreasing contamination slightly to improve Precision (be more selective)
    clf_iso = IForest(contamination=0.05, n_estimators=200, random_state=42)
    clf_iso.fit(X_train_healthy)
    
    y_pred_iso = clf_iso.predict(train_feat[features])
    y_scores_iso = clf_iso.decision_function(train_feat[features])
    
    log("\n--- [Unsupervised] Optimized Isolation Forest Results ---")
    log(classification_report(train_feat['label'], y_pred_iso, target_names=['Normal', 'Anomaly']))
    log(f"ROC-AUC: {roc_auc_score(train_feat['label'], y_scores_iso):.4f}")

    # --- MODEL 2: Random Forest (Supervised Benchmark) ---
    # Using supervised learning because we HAVE labels. This usually maximizes precision.
    clf_rf = RandomForestClassifier(n_estimators=100, max_depth=10, random_state=42)
    # Split Train/Val to be fair (grouped by Unit)
    units = train_feat['unit_number'].unique()
    train_units = units[:80]
    val_units = units[80:]
    
    X_tr = train_feat[train_feat['unit_number'].isin(train_units)][features]
    y_tr = train_feat[train_feat['unit_number'].isin(train_units)]['label']
    X_val = train_feat[train_feat['unit_number'].isin(val_units)][features]
    y_val = train_feat[train_feat['unit_number'].isin(val_units)]['label']
    
    clf_rf.fit(X_tr, y_tr)
    y_pred_rf = clf_rf.predict(X_val)
    y_probs_rf = clf_rf.predict_proba(X_val)[:, 1]
    
    log("\n--- [Supervised] Random Forest Results (Validation Set) ---")
    log(classification_report(y_val, y_pred_rf, target_names=['Normal', 'Anomaly']))
    log(f"ROC-AUC: {roc_auc_score(y_val, y_probs_rf):.4f}")

    # --- PLOT COMPARISON ---
    plt.figure(figsize=(10, 6))
    
    # Default IF
    fpr_def, tpr_def, _ = roc_curve(train_feat['label'], y_scores_iso_default)
    plt.plot(fpr_def, tpr_def, label=f'Default Isolation Forest (AUC = {roc_auc_score(train_feat["label"], y_scores_iso_default):.2f})', linestyle='--')
    
    # Optimized IF
    fpr_opt, tpr_opt, _ = roc_curve(train_feat['label'], y_scores_iso)
    plt.plot(fpr_opt, tpr_opt, label=f'Optimized Isolation Forest (AUC = {roc_auc_score(train_feat["label"], y_scores_iso):.2f})', linewidth=2)
    
    # Random Forest
    fpr_rf, tpr_rf, _ = roc_curve(y_val, y_probs_rf)
    plt.plot(fpr_rf, tpr_rf, label=f'Random Forest (Supervised) (AUC = {roc_auc_score(y_val, y_probs_rf):.2f})')
    
    plt.plot([0, 1], [0, 1], 'k--', alpha=0.5)
    plt.xlabel('False Positive Rate')
    plt.ylabel('True Positive Rate')
    plt.title('ROC Curve Comparison: Anomaly Detection Models')
    plt.legend(loc="lower right")
    plt.grid(True, alpha=0.3)
    
    plt.savefig('roc_comparison.png')
    log("\nSaved comparison plot to 'roc_comparison.png'")

    log("=== EXECUTION COMPLETED ===")

except Exception as e:
    import traceback
    log(f"ERROR: {e}")
    log(traceback.format_exc())
