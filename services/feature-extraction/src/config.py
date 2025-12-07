import os
from dotenv import load_dotenv

load_dotenv()


class Config:
    KAFKA_BROKER = os.getenv("KAFKA_BROKER", "localhost:9092")
    INPUT_TOPIC = os.getenv("INPUT_TOPIC", "preprocessed-data")
    OUTPUT_TOPIC = os.getenv("OUTPUT_TOPIC", "features-data")
    CONSUMER_GROUP = os.getenv("CONSUMER_GROUP", "feature-extraction-group")
