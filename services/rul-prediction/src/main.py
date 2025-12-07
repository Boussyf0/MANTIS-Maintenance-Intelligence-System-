import logging
from .consumer import Consumer
from .producer import Producer
from .predictor import Predictor

# Configure logging
logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(name)s - %(levelname)s - %(message)s")
logger = logging.getLogger(__name__)


def main():
    logger.info("Starting RUL Prediction Service...")

    consumer = Consumer()
    producer = Producer()
    predictor = Predictor()

    logger.info("Service initialized. Waiting for feature data...")

    try:
        for message in consumer.consume():
            data = message.value
            logger.debug(f"Received features for machine {data.get('machine_id')}")

            prediction = predictor.predict(data)

            if prediction:
                producer.send(prediction)
                logger.info(f"Predicted RUL for machine {data.get('machine_id')}: {prediction['predicted_rul']:.2f}")

    except KeyboardInterrupt:
        logger.info("Stopping service...")
    except Exception as e:
        logger.error(f"Error: {e}")


if __name__ == "__main__":
    main()
