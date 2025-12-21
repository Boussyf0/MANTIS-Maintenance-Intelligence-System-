import logging

from prometheus_client import start_http_server, Counter, Histogram, Gauge
from consumer import Consumer
from producer import Producer
from extractor import Extractor

# Configure logging
logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(name)s - %(levelname)s - %(message)s")
logger = logging.getLogger(__name__)

# Prometheus metrics
MESSAGES_PROCESSED = Counter("feature_extraction_messages_processed_total", "Total messages processed")
PROCESSING_TIME = Histogram("feature_extraction_processing_seconds", "Time spent processing messages")
SERVICE_UP = Gauge("feature_extraction_up", "Service is running")


def main():
    logger.info("Starting Feature Extraction Service...")

    # Start Prometheus metrics server on port 8003
    start_http_server(8003)
    logger.info("Prometheus metrics server started on port 8003")

    SERVICE_UP.set(1)

    consumer = Consumer()
    producer = Producer()
    extractor = Extractor()

    logger.info("Service initialized. Waiting for preprocessed data...")

    try:
        for message in consumer.consume():
            with PROCESSING_TIME.time():
                data = message.value
                logger.debug(f"Received data for machine {data.get('machine_id')}")

                extracted_data = extractor.process(data)

                if extracted_data:
                    producer.send(extracted_data)
                    logger.info(f"Extracted features for machine {data.get('machine_id')}, cycle {data.get('cycle')}")

                MESSAGES_PROCESSED.inc()

    except KeyboardInterrupt:
        logger.info("Stopping service...")
    except Exception as e:
        logger.error(f"Error: {e}")
    finally:
        SERVICE_UP.set(0)


if __name__ == "__main__":
    main()
