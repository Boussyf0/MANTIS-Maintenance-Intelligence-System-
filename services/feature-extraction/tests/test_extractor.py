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

    def test_sensor_reduction_21_to_14(self):
        """
        Test that when 21 sensors are provided, only the 14 useful sensors are used.
        Useful indices: 1, 2, 3, 6, 7, 8, 10, 11, 12, 13, 14, 16, 19, 20
        """
        # Create input with 21 sensors (0.0 to 20.0 to make tracking easy)
        # s0=0.0, s1=1.0, ... s20=20.0
        features_mean = [float(i) for i in range(21)]
        features_std = [1.0] * 21  # Stds all 1.0 to simplify SNR check (SNR = Mean)

        input_data = {
            "machine_id": "test-machine-21",
            "timestamp": "2023-01-01T00:00:00",
            "cycle": 100,
            "features": {
                "mean": features_mean,
                "std": features_std,
                "last": features_mean,
            },
        }

        result = self.extractor.process(input_data)

        self.assertIsNotNone(result)
        features = result["advanced_features"]
        model_inputs = result["model_features"]

        # 1. Verify that "model_features" (means/stds) contain exactly 14 items
        self.assertEqual(len(model_inputs["mean"]), 14)
        self.assertEqual(len(model_inputs["std"]), 14)

        # 2. Verify the CONTENT of those 14 items match the useful indices
        # Indices: [1, 2, 3, 6, 7, 8, 10, 11, 12, 13, 14, 16, 19, 20]
        # Since means[i] = i, acceptable values are exactly these integers
        expected_values = [
            1.0,
            2.0,
            3.0,
            6.0,
            7.0,
            8.0,
            10.0,
            11.0,
            12.0,
            13.0,
            14.0,
            16.0,
            19.0,
            20.0,
        ]
        self.assertEqual(model_inputs["mean"], expected_values)

        # 3. Verify Energy calculation
        # Energy = sum(x^2) for x in expected_values
        expected_energy = sum([x**2 for x in expected_values])
        # 1+4+9 + 36+49+64 + 100+121+144+169+196 + 256 + 361 + 400
        # = 14 + 149 + 730 + 1017 = 1910
        self.assertAlmostEqual(features["energy"], expected_energy)

        # 4. Verify SNR (since std=1, SNR = mean)
        # Average SNR = sum(expected_values) / 14
        expected_avg_snr = sum(expected_values) / 14
        self.assertAlmostEqual(features["avg_snr"], expected_avg_snr)


if __name__ == "__main__":
    unittest.main()
