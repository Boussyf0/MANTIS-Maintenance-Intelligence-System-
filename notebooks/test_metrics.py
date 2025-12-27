import pandas as pd
from pathlib import Path
from pyod.models.iforest import IForest
from sklearn.metrics import classification_report, roc_auc_score

PROCESSED_PATH = Path('/Users/abderrahim_boussyf/MANTIS/data/processed')
OUTPUT_FILE = Path('final_metrics.txt')

try:
    with open(OUTPUT_FILE, 'w') as f:
        f.write("Starting metrics calculation...\n")
        
        train = pd.read_csv(PROCESSED_PATH / 'train_FD001_features.csv')
        test = pd.read_csv(PROCESSED_PATH / 'test_FD001_features.csv')
        
        features = [c for c in train.columns if 'mean' in c]
        f.write(f"Features: {len(features)}\n")
        
        # Labels: RUL <= 50 -> Anomaly (1)
        train['label'] = (train['RUL'] <= 50).astype(int)
        
        X_train_normal = train[train['label'] == 0][features]
        X_eval = train[features]
        y_eval = train['label']
        
        clf = IForest(contamination=0.1, random_state=42)
        clf.fit(X_train_normal)
        
        y_pred = clf.predict(X_eval)
        y_scores = clf.decision_function(X_eval)
        
        report = classification_report(y_eval, y_pred, target_names=['Normal', 'Anomaly'])
        roc = roc_auc_score(y_eval, y_scores)
        
        f.write("\nClassification Report:\n")
        f.write(report)
        f.write(f"\nROC-AUC Score: {roc:.4f}\n")
        print("Done.")
except Exception as e:
    with open(OUTPUT_FILE, 'a') as f:
        f.write(f"Error: {str(e)}")
