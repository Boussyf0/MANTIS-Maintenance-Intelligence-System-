# MANTIS Project Overview for Gemini

This document provides a concise overview of the MANTIS project, intended to serve as instructional context for the Gemini CLI agent.

## 1. Project Overview

MANTIS (MAiNtenance prédictive Temps-réel pour usines Intelligentes) is a real-time predictive maintenance platform designed for smart factories. Its primary goal is to detect anomalies early, estimate the Remaining Useful Life (RUL) of equipment, and plan optimal interventions to prevent costly unplanned downtime.

The system is built on a **microservices architecture** consisting of 7 key services:
1.  **IngestionIIoT**: Collects sensor data streams from PLC/SCADA/edge devices.
2.  **Prétraitement**: Cleans, aligns, and windows data.
3.  **ExtractionFeatures**: Calculates time/frequency descriptors.
4.  **DétectionAnomalies**: Detects deviations from nominal operation.
5.  **PrédictionRUL**: Estimates Remaining Useful Life.
6.  **OrchestrateurMaintenance**: Applies policies and generates actions.
7.  **DashboardUsine**: Provides real-time visualization of factory line status.

**Key Technologies:**

*   **Backend**: Apache Kafka, Kafka Streams, TimescaleDB, PostgreSQL, InfluxDB, MinIO, PyTorch, XGBoost, PyOD, tsfresh, MLflow, Feast, OPC UA, MQTT, Telegraf.
*   **Frontend**: React.js, Grafana, Plotly, REST/gRPC, WebSockets.
*   **Infrastructure**: Docker, Docker Compose, Kubernetes, OpenTelemetry, Prometheus, Grafana.

## 2. Building and Running

### Prerequisites

*   Docker >= 20.10
*   Docker Compose >= 2.0
*   Kubernetes >= 1.24 (optional, for production)
*   Python >= 3.10
*   Node.js >= 18

### Quick Start

1.  **Clone the repository:**
    ```bash
    git clone <repo-url>
    cd MANTIS
    ```
2.  **Launch core infrastructure services (Kafka, Databases, MLflow, Feast):**
    ```bash
    cd infrastructure/docker
    docker-compose -f docker-compose.infrastructure.yml up -d
    ```
3.  **Launch microservices:**
    ```bash
    docker-compose -f docker-compose.services.yml up -d
    ```

### Accessing Interfaces

*   **Dashboard Usine**: `http://localhost:3000`
*   **Grafana**: `http://localhost:3001` (admin/admin)
*   **MLflow**: `http://localhost:5000`
*   **Kafka UI**: `http://localhost:8080`
*   **MinIO Console**: `http://localhost:9001`

## 3. Development Conventions

### Environment Setup

1.  **Create a virtual environment:**
    ```bash
    python -m venv venv
    source venv/bin/activate # Linux/Mac
    # or venv\Scripts\activate # Windows
    ```
2.  **Install dependencies:**
    ```bash
    pip install -r requirements-dev.txt
    ```
3.  **Install pre-commit hooks:**
    ```bash
    pre-commit install
    ```

### Testing

*   **Unit tests:**
    ```bash
    pytest tests/unit
    ```
*   **Integration tests:**
    ```bash
    pytest tests/integration
    ```
*   **Test coverage:**
    ```bash
    pytest --cov=mantis tests/
    ```

### Code Style and Linting

The presence of `.flake8` and `pre-commit` hooks suggests adherence to specific Python code style guidelines (likely PEP 8) and automated linting/formatting checks before commits. Developers should ensure their code conforms to these standards.

### Contribution Guidelines

The `CONTRIBUTING.md` file (not yet read, but present in the directory listing) likely contains detailed contribution guidelines. Developers should consult this file for information on submitting changes, pull request processes, and other contribution-related policies.
