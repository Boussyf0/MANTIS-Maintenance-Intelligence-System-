import logging
from .consumer import Consumer
from .producer import Producer
from .detector import Detector

# Configure logging
logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(name)s - %(levelname)s - %(message)s")
logger = logging.getLogger(__name__)


def main():
    logger.info("Starting Anomaly Detection Service...")

    consumer = Consumer()
    producer = Producer()
    detector = Detector()

    logger.info("Service initialized. Waiting for feature data...")

    try:
        for message in consumer.consume():
            data = message.value
            logger.debug(f"Received features for machine {data.get('machine_id')}")

            anomaly_event = detector.process(data)

            if anomaly_event:
                producer.send(anomaly_event)
                logger.warning(f"ANOMALY DETECTED for machine {data.get('machine_id')}, cycle {data.get('cycle')}")

    except KeyboardInterrupt:
        logger.info("Stopping service...")
    except Exception as e:
        logger.error(f"Error: {e}")


if __name__ == "__main__":
    main()
