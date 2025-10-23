# ✅ Phase 3: Prometheus Monitoring - Completion Summary

**Date**: 2025-10-23
**Status**: COMPLETED
**Tutor**: Claude Code

---

## 📚 What You Learned in Phase 3

### 1. **What is Prometheus?**

**Prometheus** is an open-source monitoring and alerting toolkit designed for reliability and scalability.

**Key Concepts**:
- **Metrics**: Numerical measurements over time (CPU, memory, requests/sec)
- **Time-series**: Data points indexed by timestamp
- **Scraping**: Prometheus pulls metrics from targets (pull model)
- **Labels**: Key-value pairs for dimensional data

### 2. **Why Monitoring Matters**

In production systems, monitoring answers:
- ❓ **Availability**: Is my service up?
- 📊 **Performance**: How fast is it responding?
- 🐛 **Errors**: What's failing and why?
- 📈 **Capacity**: Do I need to scale?
- 💰 **Cost**: Am I using resources efficiently?

### 3. **Prometheus Architecture in MANTIS**

```
┌─────────────────────────────────────────────────────────────┐
│                   MANTIS Monitoring Stack                    │
└─────────────────────────────────────────────────────────────┘

┌──────────────┐   scrape    ┌───────────────┐   query
│  Exporters   │─────────────>│  Prometheus   │<──────────┐
│              │   /metrics   │               │           │
│ • Postgres   │              │ • Collects    │           │
│ • Redis      │              │ • Stores      │           │
│ • Node       │              │ • Evaluates   │           │
│ • cAdvisor   │              │ • Alerts      │           │
└──────────────┘              └───────────────┘           │
                                     │                    │
                              stores │                    │
                                     ▼                    │
                              ┌───────────────┐           │
┌──────────────┐              │   TSDB        │           │
│   Services   │─────scrape──>│  (Time-series │           │
│              │   /metrics   │   Database)   │           │
│ • Ingestion  │              └───────────────┘           │
│ • ML Models  │                     ▲                    │
│ • APIs       │                     │ queries            │
└──────────────┘                     │                    │
                              ┌───────────────┐           │
                              │    Grafana    │───────────┘
                              │  (Visualize)  │
                              └───────────────┘
```

---

## ✅ What Was Completed

### 1. **Prometheus Configuration** (`prometheus.yml`)

**Updated to monitor**:

#### Application Services (Ready for deployment)
- `ingestion-iiot` (port 9091)
- `preprocessing` (port 9092)
- `feature-extraction` (port 9093)
- `anomaly-detection` (port 9094)
- `rul-prediction` (port 9095)
- `maintenance-orchestrator` (port 9096)

#### Infrastructure Exporters (Configured)
- **PostgreSQL Exporter** (port 9187) - Database metrics
- **Redis Exporter** (port 9121) - Cache metrics
- **Node Exporter** (port 9100) - Host system metrics
- **cAdvisor** (port 8084) - Container metrics

**Configuration Features**:
- ✅ Scrape interval: 15s (application), 30s (infrastructure)
- ✅ External labels for cluster identification
- ✅ Rules file loading enabled
- ✅ Lifecycle API enabled for hot-reload

---

### 2. **Infrastructure Exporters** (`docker-compose.exporters.yml`)

Created separate compose file for exporters:

#### Postgres Exporter
```yaml
Purpose: Expose PostgreSQL metrics
Metrics: Connections, queries, locks, replication
Port: 9187
```

**Key Metrics**:
- `pg_stat_database_numbackends` - Active connections
- `pg_stat_database_xact_commit` - Transaction commits
- `pg_stat_database_conflicts` - Query conflicts
- `pg_locks_count` - Lock count by type

#### Redis Exporter
```yaml
Purpose: Expose Redis metrics
Metrics: Memory, keys, commands, clients
Port: 9121
```

**Key Metrics**:
- `redis_memory_used_bytes` - Memory usage
- `redis_connected_clients` - Active clients
- `redis_commands_processed_total` - Commands executed
- `redis_keyspace_hits_total` - Cache hit rate

#### Node Exporter
```yaml
Purpose: Expose host system metrics
Metrics: CPU, memory, disk, network
Port: 9100
```

**Key Metrics**:
- `node_cpu_seconds_total` - CPU usage
- `node_memory_MemAvailable_bytes` - Available memory
- `node_filesystem_avail_bytes` - Disk space
- `node_network_receive_bytes_total` - Network I/O

#### cAdvisor
```yaml
Purpose: Expose container metrics
Metrics: Per-container resource usage
Port: 8084
```

**Key Metrics**:
- `container_cpu_usage_seconds_total` - CPU per container
- `container_memory_usage_bytes` - Memory per container
- `container_network_receive_bytes_total` - Network per container
- `container_fs_usage_bytes` - Disk per container

---

### 3. **Recording Rules** (`rules/recording_rules.yml`)

**What are Recording Rules?**
Pre-computed queries that run periodically to speed up dashboards and reduce query load.

#### Infrastructure Rules (30s interval)

**PostgreSQL Connection Pool Usage**:
```promql
mantis:postgres:connection_pool_usage:ratio
= pg_stat_database_numbackends / pg_settings_max_connections
```
*Tracks: How full is the connection pool? (0-1)*

**Redis Memory Usage**:
```promql
mantis:redis:memory_usage:ratio
= redis_memory_used_bytes / redis_memory_max_bytes
```
*Tracks: Memory pressure on Redis (0-1)*

**Container CPU Usage** (5min rate):
```promql
mantis:container:cpu_usage:rate5m
= rate(container_cpu_usage_seconds_total[5m])
```
*Tracks: CPU usage trend per service*

#### Application Rules (15s interval)

**HTTP Request Rate**:
```promql
mantis:http:requests:rate1m
= rate(http_requests_total[1m])
```
*Tracks: Requests per second per service*

**HTTP Error Rate**:
```promql
mantis:http:errors:rate1m
= rate(http_requests_total{status=~"4..|5.."}[1m])
```
*Tracks: Error requests per second*

**Kafka Message Processing Rate**:
```promql
mantis:kafka:messages:rate1m
= rate(kafka_consumer_records_consumed_total[1m])
```
*Tracks: Messages processed per second*

**Data Processing Latency (P95)**:
```promql
mantis:processing:latency:p95
= histogram_quantile(0.95, rate(processing_duration_seconds_bucket[5m]))
```
*Tracks: 95th percentile response time*

#### ML Metrics Rules (60s interval)

**Model Inference Rate**:
```promql
mantis:ml:inference:rate5m
= rate(ml_model_predictions_total[5m])
```
*Tracks: Predictions per second*

**Inference Latency (P99)**:
```promql
mantis:ml:inference_latency:p99
= histogram_quantile(0.99, rate(ml_inference_duration_seconds_bucket[5m]))
```
*Tracks: 99th percentile inference time*

---

### 4. **Alert Rules** (`rules/alerts.yml`)

**What are Alert Rules?**
Conditions that trigger notifications when system health degrades.

#### Infrastructure Alerts

**1. ServiceDown** (Critical)
```yaml
Condition: up{job=~"mantis-services"} == 0
Duration: 2 minutes
Action: Page on-call engineer
```
*Fires when: Any MANTIS service is unreachable*

**2. HighCPUUsage** (Warning)
```yaml
Condition: CPU > 80%
Duration: 5 minutes
Action: Notify ops team
```
*Fires when: Container using >80% CPU for 5min*

**3. HighMemoryUsage** (Warning)
```yaml
Condition: Memory > 85% of limit
Duration: 5 minutes
Action: Notify ops team
```
*Fires when: Container approaching memory limit*

**4. PostgreSQLConnectionPoolHigh** (Warning)
```yaml
Condition: Connection pool > 80%
Duration: 3 minutes
Action: Check for connection leaks
```
*Fires when: Running out of DB connections*

**5. RedisMemoryHigh** (Warning)
```yaml
Condition: Redis memory > 85%
Duration: 5 minutes
Action: Check cache eviction policy
```
*Fires when: Redis approaching memory limit*

**6. DiskSpaceLow** (Warning)
```yaml
Condition: Disk space < 15%
Duration: 10 minutes
Action: Clean up or expand storage
```
*Fires when: Running out of disk space*

#### Application Alerts

**7. HighErrorRate** (Critical)
```yaml
Condition: 5xx errors > 5%
Duration: 5 minutes
Action: Page on-call, investigate logs
```
*Fires when: Service error rate exceeds threshold*

**8. SlowResponseTime** (Warning)
```yaml
Condition: P95 latency > 5 seconds
Duration: 10 minutes
Action: Check for bottlenecks
```
*Fires when: Service becoming slow*

**9. KafkaConsumerLag** (Warning)
```yaml
Condition: Consumer lag > 10,000 messages
Duration: 5 minutes
Action: Scale consumers or investigate processing
```
*Fires when: Not keeping up with message stream*

**10. DataIngestionStopped** (Critical)
```yaml
Condition: No data ingested in 10 minutes
Duration: 10 minutes
Action: Check sensors and connectivity
```
*Fires when: Data pipeline stops*

#### ML-Specific Alerts

**11. MLInferenceSlow** (Warning)
```yaml
Condition: P99 inference > 1 second
Duration: 10 minutes
Action: Check model complexity or resources
```
*Fires when: Predictions taking too long*

**12. HighAnomalyRate** (Info)
```yaml
Condition: >100 anomalies/hour
Duration: 30 minutes
Action: Investigate sensor or equipment
```
*Fires when: Unusual spike in detected anomalies*

**13. ModelPerformanceDegraded** (Warning)
```yaml
Condition: MAE > 50
Duration: 30 minutes
Action: Consider model retraining
```
*Fires when: Model predictions becoming inaccurate*

#### Business-Critical Alerts

**14. CriticalAssetOffline** (Critical)
```yaml
Condition: Critical asset not operational
Duration: 5 minutes
Action: Immediate maintenance dispatch
```
*Fires when: High-priority equipment fails*

**15. MaintenanceOverdue** (Warning)
```yaml
Condition: High-priority work orders overdue
Duration: 1 hour
Action: Notify maintenance supervisor
```
*Fires when: Missing maintenance schedules*

---

## 🎓 Key Learning Points

### 1. **Monitoring Best Practices**

**The Four Golden Signals** (from Google SRE):
1. **Latency**: How long do requests take?
2. **Traffic**: How many requests per second?
3. **Errors**: What's the error rate?
4. **Saturation**: How full are my resources?

### 2. **PromQL Fundamentals**

**Rate Functions**:
```promql
rate(metric[5m])        # Per-second rate over 5 minutes
irate(metric[5m])       # Instant rate (last 2 points)
increase(metric[1h])    # Total increase over 1 hour
```

**Aggregations**:
```promql
sum(metric) by (label)     # Sum grouped by label
avg(metric)                # Average across all series
max(metric)                # Maximum value
count(metric)              # Number of time series
```

**Quantiles** (for latency):
```promql
histogram_quantile(0.95, rate(duration_bucket[5m]))
# P95 latency = 95% of requests faster than this
```

### 3. **When to Alert vs Monitor**

**Alert** (pages someone):
- Service down
- Error rate spike
- Data loss
- Security breach

**Monitor** (dashboard only):
- Gradual trends
- Capacity planning
- Performance optimization
- Business metrics

### 4. **Alert Fatigue Prevention**

Good alerts are:
- **Actionable**: Clear what to do
- **Meaningful**: Affects users or business
- **Rare**: Not firing constantly
- **Specific**: Easy to diagnose

---

## 📊 Prometheus Query Examples

### Infrastructure Queries

**CPU Usage per Container**:
```promql
rate(container_cpu_usage_seconds_total{container_label_com_docker_compose_project="mantis"}[5m]) * 100
```

**Memory Usage per Service**:
```promql
container_memory_usage_bytes{container_label_com_docker_compose_project="mantis"} / 1024 / 1024
```

**PostgreSQL Active Connections**:
```promql
pg_stat_database_numbackends{datname="mantis"}
```

**Redis Hit Rate**:
```promql
rate(redis_keyspace_hits_total[5m]) /
(rate(redis_keyspace_hits_total[5m]) + rate(redis_keyspace_misses_total[5m]))
```

### Application Queries

**Request Rate (per service)**:
```promql
sum(rate(http_requests_total[1m])) by (job)
```

**Error Percentage**:
```promql
sum(rate(http_requests_total{status=~"5.."}[5m])) by (job)
/
sum(rate(http_requests_total[5m])) by (job) * 100
```

**P99 Latency**:
```promql
histogram_quantile(0.99,
  sum(rate(http_request_duration_seconds_bucket[5m])) by (le, job)
)
```

### ML-Specific Queries

**Predictions per Second**:
```promql
rate(ml_model_predictions_total[1m])
```

**Model Accuracy Over Time**:
```promql
ml_model_accuracy{model="rul_predictor"}
```

**Anomalies Detected (hourly)**:
```promql
increase(anomalies_detected_total[1h])
```

---

## 🔧 Useful Commands

### Check Prometheus Status
```bash
# Is Prometheus running?
curl http://localhost:9091/-/healthy

# Configuration valid?
curl http://localhost:9091/api/v1/status/config

# What targets are being scraped?
curl http://localhost:9091/api/v1/targets
```

### Reload Configuration
```bash
# Hot-reload (no restart needed)
curl -X POST http://localhost:9091/-/reload

# Or restart container
docker-compose -f docker-compose.infrastructure.yml restart prometheus
```

### Query from CLI
```bash
# Simple query
curl 'http://localhost:9091/api/v1/query?query=up'

# Query with time range
curl 'http://localhost:9091/api/v1/query_range?query=up&start=2025-10-23T00:00:00Z&end=2025-10-23T12:00:00Z&step=15s'
```

### Check Alert Status
```bash
# Active alerts
curl http://localhost:9091/api/v1/alerts

# Alert rules
curl http://localhost:9091/api/v1/rules
```

---

## 📈 Next Steps

### To Deploy Exporters:
```bash
cd /Users/abderrahim_boussyf/MANTIS/infrastructure/docker
docker-compose -f docker-compose.exporters.yml up -d
```

### To View Metrics:
1. **Prometheus UI**: http://localhost:9091
2. **Grafana Dashboards**: http://localhost:3001
3. **cAdvisor UI**: http://localhost:8084

### When Services are Deployed:
The recording rules and alerts will automatically start working once your MANTIS microservices expose `/metrics` endpoints.

---

## ✅ Phase 3 Checklist

- [x] Prometheus configured and running
- [x] Infrastructure exporters defined
- [x] Recording rules created (3 groups, 11 rules)
- [x] Alert rules created (15 alerts)
- [x] Rules mounted in Prometheus container
- [x] Configuration validated
- [x] Documentation completed

**Phase 3 Status**: ✅ **COMPLETE**

---

## 🎉 Summary

You now have a **production-grade monitoring system** that will:

1. ✅ **Collect metrics** from all infrastructure and services
2. ✅ **Pre-compute** common queries for fast dashboards
3. ✅ **Alert** when things go wrong
4. ✅ **Scale** with your system
5. ✅ **Provide visibility** into system health

**Congratulations!** Your MANTIS system is now observable! 🔍

---

**Next Phase**: Phase 4 - Service Implementation (Ingestion, Processing, ML)
