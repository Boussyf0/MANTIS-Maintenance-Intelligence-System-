import logging
from prometheus_client import start_http_server, Counter, Histogram, Gauge
from consumer import Consumer
from producer import Producer
from predictor import Predictor

# Configure logging
logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(name)s - %(levelname)s - %(message)s")
logger = logging.getLogger(__name__)

# Prometheus metrics
MESSAGES_PROCESSED = Counter("rul_prediction_messages_processed_total", "Total messages processed")
PREDICTIONS_MADE = Counter("rul_prediction_predictions_made_total", "Total predictions made")
PROCESSING_TIME = Histogram("rul_prediction_processing_seconds", "Time spent processing messages")
PREDICTED_RUL = Gauge("rul_prediction_last_rul", "Last predicted RUL value", ["machine_id"])
SERVICE_UP = Gauge("rul_prediction_up", "Service is running")


def main():
    logger.info("Starting RUL Prediction Service...")

    # Start Prometheus metrics server on port 8005
    start_http_server(8005)
    logger.info("Prometheus metrics server started on port 8005")

    SERVICE_UP.set(1)

    consumer = Consumer()
    producer = Producer()
    predictor = Predictor()

    logger.info("Service initialized. Waiting for feature data...")

    try:
        for message in consumer.consume():
            with PROCESSING_TIME.time():
                data = message.value
                logger.debug(f"Received features for machine {data.get('machine_id')}")

                prediction = predictor.predict(data)

                if prediction:
                    producer.send(prediction)
                    PREDICTIONS_MADE.inc()
                    PREDICTED_RUL.labels(machine_id=data.get("machine_id", "unknown")).set(prediction["predicted_rul"])
                    logger.info(
                        f"Predicted RUL for machine {data.get('machine_id')}: {prediction['predicted_rul']:.2f}"
                    )

                MESSAGES_PROCESSED.inc()

    except KeyboardInterrupt:
        logger.info("Stopping service...")
    except Exception as e:
        logger.error(f"Error: {e}")
    finally:
        SERVICE_UP.set(0)


if __name__ == "__main__":
    main()
