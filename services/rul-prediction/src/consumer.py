import json
from kafka import KafkaConsumer
from config import Config


class Consumer:
    def __init__(self):
        self.consumer = KafkaConsumer(
            Config.INPUT_TOPIC,
            bootstrap_servers=Config.KAFKA_BROKER,
            group_id=Config.CONSUMER_GROUP,
            value_deserializer=lambda x: json.loads(x.decode("utf-8")),
        )

    def consume(self):
        return self.consumer
