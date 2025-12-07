import logging
from .consumer import Consumer
from .producer import Producer
from .extractor import Extractor

# Configure logging
logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(name)s - %(levelname)s - %(message)s")
logger = logging.getLogger(__name__)


def main():
    logger.info("Starting Feature Extraction Service...")

    consumer = Consumer()
    producer = Producer()
    extractor = Extractor()

    logger.info("Service initialized. Waiting for preprocessed data...")

    try:
        for message in consumer.consume():
            data = message.value
            logger.debug(f"Received data for machine {data.get('machine_id')}")

            extracted_data = extractor.process(data)

            if extracted_data:
                producer.send(extracted_data)
                logger.info(f"Extracted features for machine {data.get('machine_id')}, cycle {data.get('cycle')}")

    except KeyboardInterrupt:
        logger.info("Stopping service...")
    except Exception as e:
        logger.error(f"Error: {e}")


if __name__ == "__main__":
    main()
