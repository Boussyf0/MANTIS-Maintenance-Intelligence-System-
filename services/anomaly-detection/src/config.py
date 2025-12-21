import os
from dotenv import load_dotenv

load_dotenv()


class Config:
    KAFKA_BROKER = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
    INPUT_TOPIC = os.getenv("INPUT_TOPIC", "features-data")
    OUTPUT_TOPIC = os.getenv("OUTPUT_TOPIC", "anomaly-events")
    CONSUMER_GROUP = os.getenv("CONSUMER_GROUP", "anomaly-detection-group")
    CONTAMINATION = float(os.getenv("CONTAMINATION", "0.05"))
