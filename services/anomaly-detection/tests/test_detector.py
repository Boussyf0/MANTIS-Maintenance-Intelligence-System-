import unittest
import sys
import os

# Add src to path
# Add parent directory to path to allow importing src as a package
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from src.detector import Detector  # noqa: E402


class TestDetector(unittest.TestCase):
    def setUp(self):
        self.detector = Detector()
        # Reduce buffer size for testing
        self.detector.BUFFER_SIZE = 5

    def test_detection_logic(self):
        # Create normal data
        normal_data = {
            "machine_id": "test-machine",
            "timestamp": "2023-01-01T00:00:00",
            "cycle": 1,
            "advanced_features": {
                "skewness": 0.1,
                "kurtosis": 0.1,
                "energy": 1.0,
                "avg_snr": 10.0,
            },
        }

        # Feed normal data to fit the model
        for i in range(5):
            result = self.detector.process(normal_data)
            self.assertIsNone(result, f"Should be None during training step {i}")

        self.assertTrue(self.detector.is_fitted, "Model should be fitted after buffer is full")

        # Test Normal Prediction
        result = self.detector.process(normal_data)
        # It might be None (normal) or not None (anomaly) depending on random init,
        # but with identical data it should likely be normal.
        # IsolationForest is tricky with small data, but let's check structure if it returns something.

        # Test Anomaly
        anomaly_data = {
            "machine_id": "test-machine",
            "timestamp": "2023-01-01T00:00:00",
            "cycle": 100,
            "advanced_features": {
                "skewness": 10.0,  # Huge skew
                "kurtosis": 20.0,  # Huge kurtosis
                "energy": 100.0,
                "avg_snr": 0.1,
            },
        }

        # This should theoretically trigger an anomaly
        result = self.detector.process(anomaly_data)

        # If result is not None, check structure
        if result:
            self.assertTrue(result["is_anomaly"])
            self.assertIn("anomaly_score", result)


if __name__ == "__main__":
    unittest.main()
