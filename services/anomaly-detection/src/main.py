import logging
from prometheus_client import start_http_server, Counter, Histogram, Gauge
from consumer import Consumer
from producer import Producer
from detector import Detector

# Configure logging
logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(name)s - %(levelname)s - %(message)s")
logger = logging.getLogger(__name__)

# Prometheus metrics
MESSAGES_PROCESSED = Counter("anomaly_detection_messages_processed_total", "Total messages processed")
ANOMALIES_DETECTED = Counter("anomaly_detection_anomalies_detected_total", "Total anomalies detected")
PROCESSING_TIME = Histogram("anomaly_detection_processing_seconds", "Time spent processing messages")
SERVICE_UP = Gauge("anomaly_detection_up", "Service is running")


def main():
    logger.info("Starting Anomaly Detection Service...")

    # Start Prometheus metrics server on port 8004
    start_http_server(8004)
    logger.info("Prometheus metrics server started on port 8004")

    SERVICE_UP.set(1)

    consumer = Consumer()
    producer = Producer()
    detector = Detector()

    logger.info("Service initialized. Waiting for feature data...")

    try:
        for message in consumer.consume():
            with PROCESSING_TIME.time():
                data = message.value
                logger.debug(f"Received features for machine {data.get('machine_id')}")

                anomaly_event = detector.process(data)

                if anomaly_event:
                    producer.send(anomaly_event)
                    ANOMALIES_DETECTED.inc()
                    logger.warning(f"ANOMALY DETECTED for machine {data.get('machine_id')}, cycle {data.get('cycle')}")

                MESSAGES_PROCESSED.inc()

    except KeyboardInterrupt:
        logger.info("Stopping service...")
    except Exception as e:
        logger.error(f"Error: {e}")
    finally:
        SERVICE_UP.set(0)


if __name__ == "__main__":
    main()
