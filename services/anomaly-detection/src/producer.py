import json
from kafka import KafkaProducer
from config import Config


class Producer:
    def __init__(self):
        self.producer = KafkaProducer(
            bootstrap_servers=Config.KAFKA_BROKER,
            value_serializer=lambda v: json.dumps(v).encode("utf-8"),
        )

    def send(self, data):
        self.producer.send(Config.OUTPUT_TOPIC, data)
        self.producer.flush()
