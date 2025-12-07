import torch
import os
import logging
from .model import RULModel
from .config import Config

logger = logging.getLogger(__name__)


class Predictor:
    def __init__(self):
        self.model = RULModel(Config.INPUT_SIZE, Config.HIDDEN_SIZE, Config.NUM_LAYERS)
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.model.to(self.device)

        if os.path.exists(Config.MODEL_PATH):
            logger.info(f"Loading model from {Config.MODEL_PATH}")
            self.model.load_state_dict(torch.load(Config.MODEL_PATH, map_location=self.device))
        else:
            logger.warning(f"Model file not found at {Config.MODEL_PATH}. Using random weights.")

        self.model.eval()

    def predict(self, data):
        """
        Predict RUL from feature vector.
        Input data format:
        {
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
        vector = [
            features.get("skewness", 0),
            features.get("kurtosis", 0),
            features.get("energy", 0),
            features.get("avg_snr", 0),
        ]

        # Prepare input tensor
        # LSTM expects (batch_size, seq_len, input_size)
        # Since we are doing point-wise prediction here (based on aggregated features),
        # we treat seq_len as 1. Ideally, we should have a sequence of feature vectors.
        # But given the architecture, we are predicting from the current window's features.

        input_tensor = torch.tensor([vector], dtype=torch.float32).unsqueeze(0).to(self.device)

        with torch.no_grad():
            prediction = self.model(input_tensor)
            rul = prediction.item()

        return {
            "machine_id": data["machine_id"],
            "timestamp": data["timestamp"],
            "cycle": data["cycle"],
            "predicted_rul": float(rul),
        }
