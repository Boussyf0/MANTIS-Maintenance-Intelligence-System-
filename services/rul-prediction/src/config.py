import os
from dotenv import load_dotenv

load_dotenv()


class Config:
    KAFKA_BROKER = os.getenv("KAFKA_BROKER", "localhost:9092")
    INPUT_TOPIC = os.getenv("INPUT_TOPIC", "features-data")
    OUTPUT_TOPIC = os.getenv("OUTPUT_TOPIC", "rul-predictions")
    CONSUMER_GROUP = os.getenv("CONSUMER_GROUP", "rul-prediction-group")

    # Model settings
    INPUT_SIZE = int(os.getenv("INPUT_SIZE", "4"))  # skewness, kurtosis, energy, snr
    HIDDEN_SIZE = int(os.getenv("HIDDEN_SIZE", "64"))
    NUM_LAYERS = int(os.getenv("NUM_LAYERS", "2"))
    MODEL_PATH = os.getenv("MODEL_PATH", "model.pth")
