# MANTIS Infrastructure - Port Mapping

## Overview
This document provides the complete port mapping for all MANTIS infrastructure services. Ports have been adjusted to avoid conflicts with other services running on the host machine.

## Port Changes (Modified from Defaults)

| Service | Original Port | New Port | Reason |
|---------|---------------|----------|---------|
| **Prometheus** | 9090 | **9091** | Conflict with braintech-prometheus |
| **Redis** | 6379 | **6380** | Conflict with braintech-redis |
| **Kafka UI** | 8080 | **8082** | Conflict with braintech-laravel |
| **MLflow** | 5000 | **5002** | Conflict with macOS Control Center (AirPlay) |

## Complete Service Port Mapping

### Data Streaming
| Service | Internal Port | Host Port | Description |
|---------|---------------|-----------|-------------|
| **Zookeeper** | 2181 | 2181 | Zookeeper client port |
| **Kafka** | 9092 | 9092 | Kafka broker (internal) |
| **Kafka** | 9093 | 9093 | Kafka broker (external/localhost) |
| **Kafka UI** | 8080 | **8082** | Kafka management UI |

### Databases
| Service | Internal Port | Host Port | Description |
|---------|---------------|-----------|-------------|
| **PostgreSQL** | 5432 | 5432 | Metadata database |
| **TimescaleDB** | 5432 | 5433 | Time-series database |
| **InfluxDB** | 8086 | 8086 | High-frequency metrics |
| **Redis** | 6379 | **6380** | Cache & Feature Store |

### Object Storage
| Service | Internal Port | Host Port | Description |
|---------|---------------|-----------|-------------|
| **MinIO API** | 9000 | 9000 | S3-compatible object storage |
| **MinIO Console** | 9001 | 9001 | MinIO web console |

### ML/AI Platforms
| Service | Internal Port | Host Port | Description |
|---------|---------------|-----------|-------------|
| **MLflow** | 5000 | **5002** | ML tracking & model registry |

### Monitoring & Observability
| Service | Internal Port | Host Port | Description |
|---------|---------------|-----------|-------------|
| **Prometheus** | 9090 | **9091** | Metrics collection |
| **Grafana** | 3000 | 3001 | Visualization dashboards |
| **Jaeger UI** | 16686 | 16686 | Distributed tracing UI |
| **Jaeger Collector** | 14268 | 14268 | Trace collector HTTP |
| **Jaeger Collector** | 14250 | 14250 | Trace collector gRPC |
| **Jaeger Agent** | 5775/udp | 5775/udp | Zipkin compact thrift |
| **Jaeger Agent** | 6831/udp | 6831/udp | Jaeger compact thrift |
| **Jaeger Agent** | 6832/udp | 6832/udp | Jaeger binary thrift |
| **Jaeger Query** | 5778 | 5778 | Config server |
| **Jaeger Zipkin** | 9411 | 9411 | Zipkin compatible endpoint |

## Access URLs

### Web Interfaces
- **Kafka UI**: http://localhost:8082
- **MinIO Console**: http://localhost:9001
- **MLflow**: http://localhost:5002
- **Grafana**: http://localhost:3001 (admin/admin)
- **Prometheus**: http://localhost:9091
- **Jaeger**: http://localhost:16686

### API Endpoints
- **Kafka**: localhost:9093 (from host), kafka:9092 (from containers)
- **PostgreSQL**: localhost:5432
- **TimescaleDB**: localhost:5433
- **InfluxDB**: http://localhost:8086
- **Redis**: localhost:6380
- **MinIO API**: http://localhost:9000

## Environment Variable Updates

All environment files have been updated with the new ports:

### Application Configuration
```bash
# Redis
REDIS_PORT=6380
REDIS_HOST=redis

# MLflow
MLFLOW_TRACKING_URI=http://mlflow:5002

# Prometheus
PROMETHEUS_URL=http://prometheus:9091

# Kafka UI
# Access via http://localhost:8082
```

### Docker Compose
The following files have been updated:
- `/infrastructure/docker/docker-compose.infrastructure.yml`
- `/infrastructure/docker/.env.example`
- `/infrastructure/docker/.env`

### Environment Files
- `/.env.example`
- `/environments/development/.env.example`
- `/environments/staging/.env.example`
- `/environments/production/.env.template`

### Configuration Files
- `/infrastructure/docker/grafana/provisioning/datasources/datasources.yml`

## Conflict Resolution

### Identified Conflicts
The following services were found to be using the same ports:

1. **Port 9090**: braintech-prometheus → MANTIS Prometheus moved to 9091
2. **Port 6379**: braintech-redis → MANTIS Redis moved to 6380
3. **Port 8080**: braintech-laravel → MANTIS Kafka UI moved to 8082
4. **Port 5000**: macOS Control Center (AirPlay) → MANTIS MLflow moved to 5002

### Resolution Strategy
- Modified MANTIS infrastructure ports to avoid conflicts
- Kept braintech services on original ports (already running)
- Updated all references across the repository

## Verification

To verify all services are running on correct ports:

```bash
# Check all MANTIS containers
docker ps --filter "name=mantis-" --format "table {{.Names}}\t{{.Ports}}"

# Check specific ports are not in use
lsof -i :9091  # Should show mantis-prometheus
lsof -i :6380  # Should show mantis-redis
lsof -i :8082  # Should show mantis-kafka-ui
lsof -i :5002  # Should show mantis-mlflow
```

## Notes

1. **Internal vs External Ports**: Services communicate internally using service names and internal ports. External ports (host ports) are only needed for local development access.

2. **Production Deployment**: In production (Kubernetes), these port conflicts won't exist as each namespace is isolated. Current changes are specific to local Docker Compose development.

3. **Firewall Rules**: If using a firewall, ensure the new ports (9091, 6380, 8082, 5002) are allowed for local access.

4. **Documentation Updates**: Always refer to this document for the latest port assignments when configuring clients or updating documentation.

---

**Last Updated**: 2025-10-22
**Maintained By**: MANTIS DevOps Team
