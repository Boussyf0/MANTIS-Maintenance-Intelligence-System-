
## Project Overview

MANTIS (MAiNtenance prédictive Temps-réel pour usines Intelligentes) is a real-time predictive maintenance platform for smart factories. It's a microservices-based system that ingests IIoT sensor data, processes it through ML pipelines, detects anomalies, predicts Remaining Useful Life (RUL), and generates optimized maintenance actions.

## Architecture

MANTIS uses a **polyglot microservices architecture** with 7 core services:

1. **IngestionIIoT** (Java/Spring Boot) - Collects sensor data from OPC UA, MQTT, Modbus
2. **Preprocessing** (Java/Spring Boot) - Data cleaning, resampling, windowing
3. **FeatureExtraction** (Python/FastAPI) - Time/frequency domain features
4. **AnomalyDetection** (Python/FastAPI) - Isolation Forest, Autoencoders, One-Class SVM
5. **RULPrediction** (Python/FastAPI) - LSTM/GRU/TCN models with transfer learning
6. **MaintenanceOrchestrator** (Java/Spring Boot) - Business rules (Drools) and optimization (OR-Tools)
7. **Dashboard** (React/Next.js) - Real-time monitoring and visualization

**Communication**: Services communicate asynchronously via Apache Kafka topics (`sensor.raw`, `sensor.preprocessed`, `features.computed`, `anomalies.detected`, `rul.predictions`, `maintenance.actions`).

**Data Stores**:
- PostgreSQL: Metadata (assets, sensors, work orders)
- TimescaleDB: Time series data (hypertables with compression)
- InfluxDB: High-frequency metrics (>100Hz)
- Redis: Feature Store online cache (Feast)
- MinIO: Object storage (ML models, raw data archives)

## Technology Stack

### Java Services
- **Framework**: Spring Boot 3.2.1, Java 17
- **Build**: Maven 3.9
- **Key Libraries**: Eclipse Milo (OPC UA), Eclipse Paho (MQTT), Spring Kafka, Resilience4j
- **Testing**: JUnit 5, Testcontainers

### Python Services
- **Runtime**: Python 3.11
- **Framework**: FastAPI
- **ML/DL**: PyTorch, XGBoost, PyOD, tsfresh
- **MLOps**: MLflow, Feast
- **Testing**: pytest

### Infrastructure
- **Streaming**: Apache Kafka 3.6
- **Monitoring**: Prometheus, Grafana, Jaeger (OpenTelemetry)
- **Deployment**: Docker Compose (dev), Kubernetes (production)

## Common Commands

### Build & Test

**Java service** (e.g., ingestion-iiot):
```bash
cd services/ingestion-iiot
mvn clean install                    # Build
mvn test                              # Run tests
mvn test -Dtest=ClassName#methodName  # Run single test
mvn jacoco:report                     # Generate coverage report
```

**Python service** (when available):
```bash
cd services/<service-name>
python -m venv venv && source venv/bin/activate
pip install -r requirements.txt
pytest tests/                         # All tests
pytest tests/test_file.py::test_name  # Single test
pytest --cov=src tests/               # With coverage
```

### Infrastructure

```bash
# Start infrastructure (Kafka, DBs, MLflow, etc.)
make docker-up
# or
cd infrastructure/docker
docker-compose -f docker-compose.infrastructure.yml up -d

# Stop all services
make docker-down

# View logs
docker logs -f mantis-kafka
docker logs -f mantis-ingestion-iiot

# Database access
make db-shell-postgres     # PostgreSQL
make db-shell-timescale    # TimescaleDB
```

### Development Workflow

```bash
# Install all dependencies
make install

# Format code (Python)
make format  # Uses black & isort

# Lint code
make lint    # flake8, pylint

# Run all tests
make test
make test-unit
make test-integration
make test-coverage

# Download NASA C-MAPSS dataset
make dataset
```

### Service URLs (when running)

- Grafana: http://localhost:3001 (admin/admin)
- MLflow: http://localhost:5000
- Kafka UI: http://localhost:8080
- MinIO Console: http://localhost:9001 (minioadmin/minioadmin)
- Prometheus: http://localhost:9090
- Jaeger: http://localhost:16686
- Ingestion API: http://localhost:8001/docs

## Key Design Patterns

### Service Communication
All inter-service communication is **asynchronous via Kafka**. Services publish events to topics and subscribe to topics they need. This ensures loose coupling and resilience.

**Example flow**:
```
Ingestion → sensor.raw → Preprocessing → sensor.preprocessed →
Feature Extraction → features.computed → [Anomaly Detection, RUL Prediction] →
[anomalies.detected, rul.predictions] → Orchestrator → maintenance.actions
```

### Data Processing Pipeline
- **Windowing**: Preprocessing creates sliding windows (512 samples, 50% overlap)
- **Feature Store**: Feast manages features with online (Redis) and offline (Parquet/MinIO) stores
- **MLflow**: Tracks experiments, registers models, and serves predictions

### Transfer Learning
RUL models are pre-trained on NASA C-MAPSS turbofan degradation dataset (21 sensors, 4 scenarios), then fine-tuned on factory-specific data.

### Error Handling
- Java services use Resilience4j for circuit breakers and retries
- Kafka consumer groups ensure at-least-once delivery
- Dead letter queues for failed messages

## Dataset: NASA C-MAPSS

The NASA Turbofan Engine Degradation dataset is the reference dataset for RUL prediction:
- **Location**: `data/raw/cmapss/`
- **Download**: `./scripts/download-cmapss.sh`
- **4 sub-datasets**: FD001-FD004 (single/multi fault modes)
- **21 sensors**: Temperature, pressure, speed, flow, etc.
- **3 operational settings**: Altitude, Mach number, throttle

## Development Status

**Completed**:
- ✅ Infrastructure setup (Docker Compose)
- ✅ Ingestion IIoT service (Java/Spring Boot) with OPC UA, MQTT, Modbus connectors
- ✅ Database schemas (PostgreSQL, TimescaleDB)
- ✅ Monitoring stack (Prometheus, Grafana, Jaeger)

**In Progress/Planned**:
- 🚧 Preprocessing service
- 🚧 Feature Extraction service
- 🚧 Anomaly Detection service
- 🚧 RUL Prediction service
- 🚧 Maintenance Orchestrator
- 🚧 Dashboard (React)

## Code Organization

```
services/<service-name>/
  src/
    main/
      java/com/mantis/         # Java source (Spring Boot services)
      resources/
        application.yml         # Spring configuration
        schema.sql             # DB initialization
  pom.xml                      # Maven dependencies
  Dockerfile                   # Service containerization

services/<service-name>/       # Python services (future)
  src/
    main.py                    # FastAPI entrypoint
    config.py                  # Configuration
    models/                    # ML models
    api/                       # API routes
  requirements.txt
  Dockerfile

infrastructure/docker/
  docker-compose.infrastructure.yml  # Core infrastructure
  init-scripts/                      # DB initialization SQL

scripts/
  start-services.sh           # Start all infrastructure
  stop-services.sh            # Stop all services
  download-cmapss.sh          # Download dataset
  init-sample-data.sh         # Populate sample data
```

## Important Notes

### Kafka Topics Naming
- Use dot notation: `sensor.raw`, not `sensor_raw`
- Schema: `<domain>.<status>` (e.g., `features.computed`, `anomalies.detected`)

### Database Conventions
- PostgreSQL: Metadata, UUID primary keys
- TimescaleDB: Hypertables on `time` column, retention policies (90-365 days)
- Table names: `snake_case`
- Use continuous aggregates for hourly/daily views

### Service Ports
- 8001: Ingestion IIoT
- 8002: Preprocessing
- 8003: Feature Extraction
- 8004: Anomaly Detection
- 8005: RUL Prediction
- 8006: Maintenance Orchestrator
- 3000: Dashboard

### ML Model Registry
- All models tracked in MLflow (http://localhost:5000)
- Artifacts stored in MinIO bucket `models/`
- Model versioning: Use MLflow model registry stages (Staging, Production)

### Testing Strategy
- Unit tests: Mock Kafka, databases
- Integration tests: Use Testcontainers for real Kafka, PostgreSQL
- E2E tests: Full pipeline from ingestion to maintenance action

## When Working on This Codebase

1. **Infrastructure first**: Always ensure Docker infrastructure is running before testing services
2. **Check Kafka topics**: Use Kafka UI (port 8080) to verify message flow between services
3. **Database schemas**: Schema initialization scripts are in `infrastructure/docker/init-scripts/`
4. **Configuration**: Java services use `application.yml`, Python services use `.env` files
5. **Metrics**: All services expose `/actuator/prometheus` (Java) or `/metrics` (Python)
6. **Health checks**: `/actuator/health` (Java), `/health` (Python)

## References

- Architecture details: [ARCHITECTURE.md](ARCHITECTURE.md)
- Quick start guide: [QUICKSTART.md](QUICKSTART.md)
- Tech stack analysis: [TECH_STACK.md](TECH_STACK.md)
- Contributing guidelines: [CONTRIBUTING.md](CONTRIBUTING.md)
