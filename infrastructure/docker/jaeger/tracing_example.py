"""
MANTIS - Distributed Tracing Example with OpenTelemetry and Jaeger

This example demonstrates how to instrument your MANTIS microservices
with OpenTelemetry for distributed tracing sent to Jaeger.

Installation:
    pip install opentelemetry-api opentelemetry-sdk opentelemetry-exporter-jaeger
    pip install opentelemetry-instrumentation-fastapi
    pip install opentelemetry-instrumentation-requests
    pip install opentelemetry-instrumentation-kafka-python
"""

from opentelemetry import trace
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from opentelemetry.exporter.jaeger.thrift import JaegerExporter
from opentelemetry.sdk.resources import Resource, SERVICE_NAME
from opentelemetry.instrumentation.fastapi import FastAPIInstrumentor
from opentelemetry.instrumentation.requests import RequestsInstrumentor
from opentelemetry.trace import Status, StatusCode
from contextlib import contextmanager
import logging

logger = logging.getLogger(__name__)


def setup_tracing(service_name: str, jaeger_host: str = "localhost", jaeger_port: int = 6831):
    """
    Initialize OpenTelemetry tracing for a MANTIS service.

    Args:
        service_name: Name of the service (e.g., "ingestion-iiot")
        jaeger_host: Jaeger agent hostname
        jaeger_port: Jaeger agent port (UDP)

    Returns:
        Tracer instance

    Example:
        tracer = setup_tracing("ingestion-iiot")
    """
    # Create resource with service name
    resource = Resource(
        attributes={
            SERVICE_NAME: service_name,
            "service.version": "1.0.0",
            "deployment.environment": "development",
            "service.namespace": "mantis",
        }
    )

    # Configure Jaeger exporter
    jaeger_exporter = JaegerExporter(
        agent_host_name=jaeger_host,
        agent_port=jaeger_port,
    )

    # Create tracer provider
    provider = TracerProvider(resource=resource)

    # Add Jaeger exporter with batch processor
    provider.add_span_processor(BatchSpanProcessor(jaeger_exporter))

    # Set as global tracer provider
    trace.set_tracer_provider(provider)

    # Auto-instrument HTTP requests
    RequestsInstrumentor().instrument()

    logger.info(f"Tracing initialized for service: {service_name}")

    return trace.get_tracer(__name__)


def instrument_fastapi_app(app):
    """
    Automatically instrument a FastAPI application.

    Args:
        app: FastAPI application instance

    Example:
        from fastapi import FastAPI
        app = FastAPI()
        instrument_fastapi_app(app)
    """
    FastAPIInstrumentor.instrument_app(app)
    logger.info("FastAPI instrumentation enabled")


@contextmanager
def trace_span(tracer, span_name: str, attributes: dict = None):
    """
    Context manager for creating manual spans.

    Args:
        tracer: OpenTelemetry tracer instance
        span_name: Name of the span
        attributes: Optional span attributes

    Example:
        with trace_span(tracer, "process_sensor_data", {"sensor_id": "VIB-001"}):
            # Your code here
            process_data()
    """
    with tracer.start_as_current_span(span_name) as span:
        if attributes:
            for key, value in attributes.items():
                span.set_attribute(key, str(value))
        try:
            yield span
        except Exception as e:
            span.set_status(Status(StatusCode.ERROR, str(e)))
            span.record_exception(e)
            raise


# ==================== EXAMPLE 1: Ingestion Service ====================


def example_ingestion_service():
    """
    Example: Instrument the IIoT data ingestion service
    """
    from fastapi import FastAPI, Request

    # Initialize tracing
    tracer = setup_tracing("ingestion-iiot")

    # Create FastAPI app
    app = FastAPI(title="MANTIS Ingestion Service")

    # Auto-instrument FastAPI
    instrument_fastapi_app(app)

    @app.post("/api/v1/ingest")
    async def ingest_sensor_data(request: Request, data: dict):
        """Ingest sensor data with tracing"""

        # Manual span for business logic
        with trace_span(
            tracer,
            "validate_sensor_data",
            {
                "sensor_id": data.get("sensor_id"),
                "sensor_type": data.get("type"),
                "data_points": len(data.get("readings", [])),
            },
        ) as span:
            # Validate data
            if not data.get("sensor_id"):
                span.set_status(Status(StatusCode.ERROR, "Missing sensor_id"))
                return {"error": "Missing sensor_id"}, 400

            span.set_attribute("validation.passed", True)

        # Span for Kafka publishing
        with trace_span(tracer, "publish_to_kafka", {"topic": "sensor.raw", "partition": 0}):
            # Publish to Kafka (pseudo-code)
            # kafka_producer.send("sensor.raw", data)
            pass

        # Span for database write
        with trace_span(
            tracer,
            "write_to_influxdb",
            {"database": "sensors", "measurement": "raw_data"},
        ):
            # Write to InfluxDB (pseudo-code)
            # influx_client.write_points([data])
            pass

        return {"status": "ingested", "sensor_id": data.get("sensor_id")}

    return app


# ==================== EXAMPLE 2: ML Service ====================


def example_ml_service():
    """
    Example: Instrument the anomaly detection ML service
    """
    from fastapi import FastAPI
    import numpy as np

    # Initialize tracing
    tracer = setup_tracing("anomaly-detection")

    app = FastAPI(title="MANTIS Anomaly Detection")
    instrument_fastapi_app(app)

    @app.post("/predict")
    async def predict_anomaly(features: dict):
        """Detect anomalies with detailed tracing"""

        # Span for feature preparation
        with trace_span(
            tracer,
            "prepare_features",
            {
                "feature_count": len(features),
                "feature_names": list(features.keys()),
            },
        ) as span:
            # Convert features to array
            feature_array = np.array(list(features.values()))
            span.set_attribute("feature_vector.size", len(feature_array))

        # Span for model inference
        with trace_span(
            tracer,
            "model_inference",
            {"model_name": "isolation_forest", "model_version": "v1.2.0"},
        ) as span:
            # Load model (pseudo-code)
            # model = load_model()
            # prediction = model.predict(feature_array)
            prediction = 0  # Normal

            span.set_attribute("prediction.result", "normal" if prediction == 0 else "anomaly")
            span.set_attribute("prediction.confidence", 0.95)

        # Span for result storage
        if prediction == 1:  # Anomaly detected
            with trace_span(
                tracer,
                "store_anomaly",
                {"severity": "high", "asset_id": features.get("asset_id")},
            ):
                # Store in database (pseudo-code)
                # db.insert_anomaly(features)
                pass

        return {
            "prediction": "normal" if prediction == 0 else "anomaly",
            "confidence": 0.95,
        }

    return app


# ==================== EXAMPLE 3: Cross-Service Call ====================


def example_cross_service_call():
    """
    Example: Trace propagation across microservices
    """
    import requests
    from opentelemetry.propagate import inject

    tracer = setup_tracing("maintenance-orchestrator")

    def create_work_order(asset_id: str, anomaly_data: dict):
        """
        Create a work order by calling multiple services.
        Demonstrates trace context propagation.
        """

        with trace_span(
            tracer,
            "create_work_order",
            {"asset_id": asset_id, "triggered_by": "anomaly_detection"},
        ) as span:

            # Step 1: Get asset details
            with trace_span(tracer, "fetch_asset_details") as asset_span:
                headers = {}
                inject(headers)  # Inject trace context into headers

                response = requests.get(
                    f"http://asset-service/api/v1/assets/{asset_id}",
                    headers=headers,
                )
                asset_data = response.json()
                asset_span.set_attribute("asset.criticality", asset_data.get("criticality"))

            # Step 2: Calculate RUL
            with trace_span(tracer, "calculate_rul") as rul_span:
                headers = {}
                inject(headers)

                response = requests.post(
                    "http://rul-prediction/predict",
                    json={"asset_id": asset_id, "features": anomaly_data},
                    headers=headers,
                )
                rul_data = response.json()
                rul_span.set_attribute("rul.days_remaining", rul_data.get("days"))

            # Step 3: Create work order
            with trace_span(tracer, "insert_work_order") as wo_span:
                work_order = {
                    "asset_id": asset_id,
                    "priority": ("high" if rul_data.get("days") < 7 else "medium"),
                    "description": f"Anomaly detected: {anomaly_data.get('type')}",
                    "estimated_rul": rul_data.get("days"),
                }
                # Insert into database (pseudo-code)
                # db.insert_work_order(work_order)
                wo_span.set_attribute("work_order.priority", work_order["priority"])

            span.set_attribute("work_order.created", True)
            return work_order

    return create_work_order


# ==================== EXAMPLE 4: Kafka Consumer Tracing ====================


def example_kafka_consumer():
    """
    Example: Trace Kafka message consumption
    """
    from kafka import KafkaConsumer
    from opentelemetry.propagate import extract
    import json

    tracer = setup_tracing("preprocessing")

    consumer = KafkaConsumer(
        "sensor.raw",
        bootstrap_servers=["kafka:9092"],
        value_deserializer=lambda m: json.loads(m.decode("utf-8")),
    )

    for message in consumer:
        # Extract trace context from Kafka message headers
        context = extract(message.headers)

        # Create span with parent context
        with tracer.start_as_current_span("process_kafka_message", context=context) as span:
            span.set_attribute("kafka.topic", message.topic)
            span.set_attribute("kafka.partition", message.partition)
            span.set_attribute("kafka.offset", message.offset)

            # Process data with child spans
            with trace_span(tracer, "clean_data"):
                # Data cleaning logic
                _ = message.value  # Use the data

            with trace_span(tracer, "validate_data"):
                # Validation logic
                is_valid = True

            if is_valid:
                with trace_span(tracer, "forward_to_next_stage"):
                    # Forward to next service
                    pass

            span.set_attribute("processing.success", is_valid)


# ==================== EXAMPLE 5: Database Query Tracing ====================


def example_database_tracing():
    """
    Example: Trace database queries for performance analysis
    """
    from sqlalchemy import create_engine
    from opentelemetry.instrumentation.sqlalchemy import SQLAlchemyInstrumentor

    tracer = setup_tracing("maintenance-orchestrator")

    # Auto-instrument SQLAlchemy
    engine = create_engine("postgresql://mantis:mantis_password@postgres:5432/mantis")
    SQLAlchemyInstrumentor().instrument(engine=engine)

    def get_critical_assets():
        """Get critical assets with traced queries"""

        with trace_span(
            tracer,
            "query_critical_assets",
            {"query.type": "select", "query.table": "assets"},
        ) as span:
            # SQLAlchemy automatically creates child spans for each query
            from sqlalchemy.orm import Session

            with Session(engine) as session:
                # This query will be automatically traced
                result = session.execute(
                    "SELECT * FROM assets WHERE criticality = 'critical' AND status != 'operational'"
                )
                assets = result.fetchall()

                span.set_attribute("query.result_count", len(assets))
                return assets


# ==================== EXAMPLE 6: Error Tracking ====================


def example_error_tracking():
    """
    Example: Trace errors and exceptions
    """
    tracer = setup_tracing("feature-extraction")

    def extract_features(sensor_data: dict):
        """Extract features with error tracking"""

        with trace_span(
            tracer,
            "extract_features",
            {"sensor_type": sensor_data.get("type")},
        ) as span:
            try:
                # Simulate feature extraction
                if not sensor_data.get("readings"):
                    raise ValueError("No readings provided")

                # FFT processing
                with trace_span(tracer, "compute_fft") as fft_span:
                    # Simulate FFT computation
                    fft_span.set_attribute("fft.window_size", 1024)

                # Statistical features
                with trace_span(tracer, "compute_statistics") as stats_span:
                    stats_span.set_attribute("stats.computed", "mean,std,rms")

                span.set_status(Status(StatusCode.OK))
                return {"features": [1.0, 2.0, 3.0]}

            except ValueError as e:
                # Span automatically records the error
                span.set_status(Status(StatusCode.ERROR, str(e)))
                span.record_exception(e)
                raise
            except Exception as e:
                span.set_status(Status(StatusCode.ERROR, "Unexpected error"))
                span.record_exception(e)
                logger.error(f"Feature extraction failed: {e}")
                raise


if __name__ == "__main__":
    """
    To use this in your MANTIS services:

    1. Install dependencies:
       pip install opentelemetry-api opentelemetry-sdk opentelemetry-exporter-jaeger

    2. In your service main.py:
       from tracing_example import setup_tracing, instrument_fastapi_app

       tracer = setup_tracing("your-service-name")
       app = FastAPI()
       instrument_fastapi_app(app)

    3. For manual spans:
       from tracing_example import trace_span

       with trace_span(tracer, "my_operation", {"key": "value"}):
           # Your code
           pass

    4. View traces:
       Open http://localhost:16686 (Jaeger UI)
    """
    print("✅ Tracing examples loaded. See docstrings for usage.")
