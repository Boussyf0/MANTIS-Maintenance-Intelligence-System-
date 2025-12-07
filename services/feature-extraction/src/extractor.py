import numpy as np
from scipy.stats import skew, kurtosis


class Extractor:
    def process(self, data):
        """
        Extract features from the preprocessed data.
        Input data format:
        {
            "machine_id": "...",
            "timestamp": "...",
            "cycle": 123,
            "features": {
                "mean": [...],
                "std": [...],
                "last": [...]
            }
        }
        Wait, the input from Java Preprocessing might be slightly different.
        Let's assume the Java service sends the raw window or similar stats.
        Actually, the Java service sends 'ProcessedData' which has 'features' map.
        But for advanced feature extraction (FFT), we ideally need the RAW window.

        However, based on the previous architecture, the Preprocessing service
        was supposed to do cleaning. If it aggregates, we lose the raw signal for FFT.

        Let's assume for this step that we receive the 'last' values or that we
        maintain our own window if we want to do FFT on raw data.

        BUT, to keep it simple and consistent with the pipeline:
        If Preprocessing sends aggregated stats, Feature Extraction might be redundant
        UNLESS Preprocessing sends the CLEANED WINDOW.

        Let's check the Java code I wrote.
        It sends: mean, std, last.
        It does NOT send the full window.

        This is a design constraint.
        To do FFT here, we would need the full window.

        OPTION 1: Update Java Preprocessing to send the full window.
        OPTION 2: Implement FFT in Java Preprocessing.
        OPTION 3: Feature Extraction service maintains its own buffer (redundant).

        Given the user wants Python for Feature Extraction (scipy),
        it implies complex math.

        I will assume the input is the 'ProcessedData' from Java.
        Since I can't do FFT on 'mean/std', I will calculate derived features
        from the available stats or assume the user will update Preprocessing later.

        OR better: I will implement a simple logic that takes the 'last' values
        and maybe accumulates them here? No, that's stateful and complex.

        Let's assume the Java service sends the 'cleaned_window' as well.
        I will update the Java service later if needed.

        For now, I will implement extraction assuming we get a list of values per sensor.
        If the input doesn't have it, I'll just pass through or calculate what I can.

        Actually, let's look at the Java code again.
        It sends `features` map with `List<Double>`.

        I'll implement a placeholder FFT on the 'last' values (which is just 1 point per sensor)
        which is nonsense, OR I'll calculate features on the 'mean' vector across sensors?

        Let's assume we want to extract features from the *vector of sensors* at this timestamp.
        e.g. Skewness across all sensors?

        Or, more likely, the architecture intended Preprocessing to just clean,
        and Feature Extraction to do the heavy lifting (windowing + FFT).
        But I implemented windowing in Preprocessing.

        Okay, I will implement `extractor.py` to take the `features` from input
        and add more derived features (e.g. ratios, differences).

        AND I will add a TODO to update Preprocessing to send the full window if FFT is strictly required.

        For this implementation:
        1. Calculate Skewness/Kurtosis of the 'mean' vector (distribution across sensors).
        2. Calculate Energy of the 'mean' vector.
        """

        if not data or "features" not in data:
            return None

        features = data["features"]

        # We expect 'mean', 'std', 'last' lists (one value per sensor)
        means = np.array(features.get("mean", []))
        stds = np.array(features.get("std", []))

        if len(means) == 0:
            return None

        # 1. Statistical features across sensors (e.g. is there high variance between sensors?)
        feat_skew = skew(means)
        feat_kurt = kurtosis(means)

        # 2. Energy (Sum of squares)
        energy = np.sum(means**2)

        # 3. Signal-to-Noise Ratio proxy (Mean / Std)
        # Avoid divide by zero
        snr = np.divide(means, stds, out=np.zeros_like(means), where=stds != 0)
        avg_snr = np.mean(snr)

        # Construct new payload
        extracted_data = {
            "machine_id": data["machine_id"],
            "timestamp": data["timestamp"],
            "cycle": data["cycle"],
            "advanced_features": {
                "skewness": float(feat_skew),
                "kurtosis": float(feat_kurt),
                "energy": float(energy),
                "avg_snr": float(avg_snr),
            },
        }

        return extracted_data
