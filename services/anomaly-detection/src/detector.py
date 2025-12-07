from sklearn.ensemble import IsolationForest
from .config import Config


class Detector:
    def __init__(self):
        # Initialize Isolation Forest
        # In a real scenario, we would load a pre-trained model.
        # Here, we initialize it and will fit it on the fly (which is not ideal for IF, but works for demo)
        # OR we assume it's pre-trained.
        # Let's make it simple: We buffer some data to fit, then predict.
        self.model = IsolationForest(contamination=Config.CONTAMINATION, random_state=42)
        self.is_fitted = False
        self.buffer = []
        self.BUFFER_SIZE = 100  # Need some data to fit

    def process(self, data):
        """
        Detect anomalies in the feature vector.
        Input data format:
        {
            "machine_id": "...",
            "timestamp": "...",
            "cycle": 123,
            "advanced_features": {
                "skewness": 0.1,
                "kurtosis": 0.5,
                "energy": 1.0,
                "avg_snr": 10.0
            }
        }
        """
        if not data or "advanced_features" not in data:
            return None

        features = data["advanced_features"]
        # Create feature vector
        vector = [
            features.get("skewness", 0),
            features.get("kurtosis", 0),
            features.get("energy", 0),
            features.get("avg_snr", 0),
        ]

        # Online learning simulation
        if not self.is_fitted:
            self.buffer.append(vector)
            if len(self.buffer) >= self.BUFFER_SIZE:
                self.model.fit(self.buffer)
                self.is_fitted = True
                self.buffer = []  # Clear buffer
            return None  # Can't predict yet

        # Predict
        # Isolation Forest returns -1 for anomaly, 1 for normal
        prediction = self.model.predict([vector])[0]
        score = self.model.decision_function([vector])[0]

        if prediction == -1:
            return {
                "machine_id": data["machine_id"],
                "timestamp": data["timestamp"],
                "cycle": data["cycle"],
                "anomaly_score": float(score),
                "is_anomaly": True,
                "details": "Anomaly detected by Isolation Forest",
            }

        return None
