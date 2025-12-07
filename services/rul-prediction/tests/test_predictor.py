import unittest
import sys
import os
import torch

# Add src to path
# Add parent directory to path to allow importing src as a package
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from src.predictor import Predictor  # noqa: E402
from src.config import Config  # noqa: E402


class TestPredictor(unittest.TestCase):
    def setUp(self):
        # Mock config to use CPU and small model
        Config.INPUT_SIZE = 4
        Config.HIDDEN_SIZE = 10
        Config.NUM_LAYERS = 1
        Config.MODEL_PATH = "dummy_path.pth"  # Won't exist, will use random weights

        self.predictor = Predictor()

    def test_prediction_logic(self):
        # Create dummy input data
        data = {
            "machine_id": "test-machine",
            "timestamp": "2023-01-01T00:00:00",
            "cycle": 50,
            "advanced_features": {
                "skewness": 0.1,
                "kurtosis": 0.5,
                "energy": 1.0,
                "avg_snr": 10.0,
            },
        }

        result = self.predictor.predict(data)

        self.assertIsNotNone(result)
        self.assertIn("predicted_rul", result)
        self.assertIsInstance(result["predicted_rul"], float)

        # Since weights are random, we just check it returns a number
        print(f"Predicted RUL: {result['predicted_rul']}")

    def test_model_structure(self):
        # Check if model output shape is correct
        dummy_input = torch.randn(1, 1, 4)  # Batch=1, Seq=1, Feat=4
        output = self.predictor.model(dummy_input)
        self.assertEqual(output.shape, (1, 1))


if __name__ == "__main__":
    unittest.main()
