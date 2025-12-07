import unittest
import sys
import os


# Add src to path
# Add parent directory to path to allow importing src as a package
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from src.extractor import Extractor  # noqa: E402


class TestExtractor(unittest.TestCase):
    def setUp(self):
        self.extractor = Extractor()

    def test_extraction_logic(self):
        # Create dummy input data (ProcessedData format)
        # 3 sensors, with some variance
        input_data = {
            "machine_id": "test-machine",
            "timestamp": "2023-01-01T00:00:00",
            "cycle": 10,
            "features": {
                "mean": [0.1, 0.5, 0.9],
                "std": [0.01, 0.05, 0.01],
                "last": [0.11, 0.51, 0.91],
            },
        }

        result = self.extractor.process(input_data)

        self.assertIsNotNone(result)
        self.assertIn("advanced_features", result)

        features = result["advanced_features"]
        self.assertIn("skewness", features)
        self.assertIn("kurtosis", features)
        self.assertIn("energy", features)
        self.assertIn("avg_snr", features)

        # Check energy calculation: sum(0.1^2 + 0.5^2 + 0.9^2) = 0.01 + 0.25 + 0.81 = 1.07
        self.assertAlmostEqual(features["energy"], 1.07)

        # Check SNR: mean/std -> [10, 10, 90] -> avg = 36.66...
        self.assertAlmostEqual(features["avg_snr"], 36.666666666666664)


if __name__ == "__main__":
    unittest.main()
