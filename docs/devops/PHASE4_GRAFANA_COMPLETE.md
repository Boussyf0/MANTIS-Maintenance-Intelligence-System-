# ✅ Phase 4: Grafana Dashboards - Completion Summary

**Date**: 2025-10-23
**Status**: COMPLETED
**Tutor**: Claude Code

---

## 📚 What You Learned in Phase 4

### 1. **What is Grafana?**

**Grafana** is an open-source analytics and interactive visualization platform. It connects to various data sources and transforms metrics into beautiful, meaningful dashboards.

**Key Concepts**:
- **Datasources**: Connections to data stores (Prometheus, PostgreSQL, InfluxDB, etc.)
- **Dashboards**: Collections of panels displaying visualizations
- **Panels**: Individual visualizations (graphs, gauges, tables, stats)
- **Provisioning**: Automatic configuration through YAML/JSON files
- **Queries**: Language-specific queries to fetch data (PromQL, SQL, Flux)

### 2. **Why Grafana for MANTIS?**

In predictive maintenance systems, Grafana provides:
- 📊 **Real-time Monitoring**: Live view of system health
- 🔍 **Root Cause Analysis**: Correlate metrics across different systems
- 📈 **Trend Analysis**: Identify patterns over time
- 🚨 **Alert Visualization**: See what's firing and why
- 👥 **Stakeholder Communication**: Share dashboards with teams
- 📱 **Unified View**: Single pane of glass for all metrics

### 3. **Grafana Architecture in MANTIS**

```
┌──────────────────────────────────────────────────────────┐
│                    MANTIS Grafana Stack                   │
└──────────────────────────────────────────────────────────┘

┌────────────────┐
│   Datasources  │
│                │
│ • Prometheus   │────────┐
│ • PostgreSQL   │        │
│ • TimescaleDB  │        │   ┌─────────────────┐
│ • InfluxDB     │────────┼──>│     Grafana     │
│ • Jaeger       │        │   │                 │
└────────────────┘        │   │ • Provisioning  │
                          │   │ • Dashboards    │
                          │   │ • Alerting      │
                          │   │ • Users/Teams   │
                          │   └─────────────────┘
                          │           │
                          │           │ Render
                          │           ▼
                          │   ┌─────────────────┐
                          └──>│  Web UI         │
                              │  (Port 3001)    │
                              └─────────────────┘
                                      │
                                      │ Access
                                      ▼
                              ┌─────────────────┐
                              │  Users/Browsers │
                              │  • Ops Team     │
                              │  • Engineers    │
                              │  • Management   │
                              └─────────────────┘
```

---

## ✅ What Was Completed

### 1. **Dashboard Provisioning** (`grafana/provisioning/dashboards/dashboards.yml`)

Created automatic dashboard loading configuration:

```yaml
apiVersion: 1

providers:
  - name: 'MANTIS Dashboards'
    orgId: 1
    folder: 'MANTIS'
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    allowUiUpdates: true
    options:
      path: /var/lib/grafana/dashboards
      foldersFromFilesStructure: true
```

**What this does**:
- ✅ Automatically loads all JSON files from `/var/lib/grafana/dashboards`
- ✅ Creates a "MANTIS" folder in Grafana
- ✅ Allows editing dashboards through UI
- ✅ Checks for updates every 10 seconds

---

### 2. **Dashboard 1: Infrastructure Monitoring** (`01-infrastructure-monitoring.json`)

**Purpose**: Monitor system health, resource usage, and infrastructure components

#### Panels Included:

**Service Status (Row 1)**
- **Prometheus Status**: Is monitoring system up?
- **PostgreSQL Status**: Is database exporter responding?
- **Redis Status**: Is cache exporter responding?

**Resource Monitoring (Rows 2-3)**
- **Container CPU Usage Gauge**: Visual indicator of CPU usage per service
- **CPU Usage Over Time**: Historical trend (thresholds: 70% yellow, 85% red)
- **Memory Usage Graph**: Per-container memory consumption
- **PostgreSQL Connection Pool Gauge**: Database connection utilization
- **Redis Memory Usage Gauge**: Cache memory pressure

**System Resources (Rows 4-5)**
- **Disk Space**: Available vs. total storage
- **Network I/O**: Receive/transmit rates per network interface

**Key Metrics**:
```promql
# CPU Usage
mantis:container:cpu_usage:rate5m

# Memory Usage
mantis:container:memory_usage:bytes

# Database Connection Pool
mantis:postgres:connection_pool_usage:ratio

# Redis Memory
mantis:redis:memory_usage:ratio

# Disk Space
node_filesystem_avail_bytes{mountpoint="/"}
node_filesystem_size_bytes{mountpoint="/"}

# Network I/O
rate(node_network_receive_bytes_total[5m])
rate(node_network_transmit_bytes_total[5m])
```

**Refresh**: Every 10 seconds
**Time Range**: Last 1 hour

---

### 3. **Dashboard 2: Application Performance** (`02-application-performance.json`)

**Purpose**: Monitor MANTIS microservices performance and health

#### Panels Included:

**Request Metrics (Row 1)**
- **HTTP Request Rate**: Requests per second per service
- **HTTP Error Rate**: 4xx and 5xx errors per second

**Performance Analysis (Row 2)**
- **Error Rate Percentage**: Bar gauge showing 5xx error percentage (threshold: 2% yellow, 5% red)
- **Processing Latency**: P50, P95, P99 latency percentiles (threshold: 5s)

**Data Pipeline (Rows 3-4)**
- **Kafka Message Processing Rate**: Messages consumed per second
- **Kafka Consumer Lag**: Gauge showing message backlog (threshold: 5000 yellow, 10000 red)
- **Data Ingestion Rate**: Sensor data points ingested per second per type
- **Service Health Status**: Bar gauge showing up/down status for all services

**Key Metrics**:
```promql
# Request Rate
mantis:http:requests:rate1m

# Error Rate
mantis:http:errors:rate1m

# Error Percentage
sum(rate(http_requests_total{status=~"5.."}[5m])) by (job)
/ sum(rate(http_requests_total[5m])) by (job)

# Latency (P95)
mantis:processing:latency:p95

# Kafka Processing
mantis:kafka:messages:rate1m

# Consumer Lag
kafka_consumer_lag

# Data Ingestion
rate(sensor_data_ingested_total[1m])

# Service Health
up{job=~"ingestion-iiot|preprocessing|..."}
```

**Refresh**: Every 10 seconds
**Time Range**: Last 1 hour

---

### 4. **Dashboard 3: ML Metrics** (`03-ml-metrics.json`)

**Purpose**: Monitor machine learning model performance and predictions

#### Panels Included:

**Model Performance (Row 1)**
- **Model Inference Rate**: Predictions per second per model
- **Model Inference Latency**: P50, P95, P99 latency (threshold: 1 second)

**Detection Metrics (Row 2)**
- **Anomaly Detection Rate**: Anomalies detected per hour (threshold: 50 yellow, 100 red)
- **RUL Prediction Error (MAE)**: Gauge showing Mean Absolute Error (threshold: 30 yellow, 50 red)

**Model Quality (Rows 3-4)**
- **Model Accuracy**: Accuracy percentage over time per model
- **Model F1 Score**: F1 score over time per model

**Summary Stats (Row 5)**
- **Total Predictions**: Cumulative counter
- **Total Anomalies Detected**: Cumulative counter
- **Active Models**: Count of deployed models

**Key Metrics**:
```promql
# Inference Rate
mantis:ml:inference:rate5m

# Inference Latency
mantis:ml:inference_latency:p99
histogram_quantile(0.95, rate(ml_inference_duration_seconds_bucket[5m]))
histogram_quantile(0.50, rate(ml_inference_duration_seconds_bucket[5m]))

# Anomaly Rate
mantis:ml:anomalies_detected:rate1h * 3600

# RUL Error
mantis:ml:rul_mae:avg5m

# Model Quality
ml_model_accuracy
ml_model_f1_score

# Totals
sum(ml_model_predictions_total)
sum(anomalies_detected_total)
count(ml_model_info)
```

**Refresh**: Every 10 seconds
**Time Range**: Last 1 hour

---

### 5. **Dashboard 4: Business Metrics** (`04-business-metrics.json`)

**Purpose**: Monitor business KPIs, asset health, and maintenance operations

**Datasource**: PostgreSQL (business data, not metrics)

#### Panels Included:

**Key Performance Indicators (Row 1)**
- **Assets Operational**: Count of working assets
- **Assets Down**: Count of non-operational assets (red if > 0)
- **Open Work Orders**: Count of new/assigned work orders
- **In Progress Work Orders**: Count of active maintenance
- **Overdue Maintenance**: Count of past-due work orders (yellow if > 0, red if > 5)

**Critical Views (Rows 2-3)**
- **Non-Operational Assets Table**: List of down assets sorted by criticality
  - Shows: Asset code, name, type, status, criticality, location
  - Color-coded criticality (red=critical, orange=high, yellow=medium, green=low)
- **Overdue Work Orders Table**: List of past-due maintenance sorted by priority
  - Shows: Title, asset code, priority, scheduled date, status
  - Color-coded priority

**Distribution Analysis (Rows 4-5)**
- **Asset Status Distribution**: Donut chart (operational, maintenance, failed, etc.)
- **Asset Criticality Distribution**: Donut chart (critical, high, medium, low)
- **Work Order Status Distribution**: Donut chart (open, assigned, in progress, completed, etc.)

**Trends (Row 6)**
- **Work Orders Created (Last 30 Days)**: Bar chart showing daily creation rate
- **Work Orders Completed (Last 30 Days)**: Bar chart showing daily completion rate

**Key Queries**:
```sql
-- Operational Assets
SELECT COUNT(*) FROM assets WHERE status = 'operational'

-- Assets Down
SELECT COUNT(*) FROM assets WHERE status != 'operational'

-- Open Work Orders
SELECT COUNT(*) FROM work_orders
WHERE status IN ('open', 'assigned')

-- Overdue Maintenance
SELECT COUNT(*) FROM work_orders
WHERE scheduled_start < NOW()
  AND status NOT IN ('completed', 'cancelled')

-- Non-Operational Assets
SELECT asset_code, name, type, status, criticality, location_line
FROM assets
WHERE status != 'operational'
ORDER BY CASE criticality
  WHEN 'critical' THEN 1
  WHEN 'high' THEN 2
  WHEN 'medium' THEN 3
  WHEN 'low' THEN 4
END
LIMIT 10

-- Work Orders Over Time
SELECT
  DATE_TRUNC('day', created_at) as time,
  COUNT(*) as "Work Orders Created"
FROM work_orders
WHERE created_at > NOW() - INTERVAL '30 days'
GROUP BY DATE_TRUNC('day', created_at)
ORDER BY time
```

**Refresh**: Every 30 seconds
**Time Range**: Last 24 hours (with 30-day trends)

---

## 🎓 Key Learning Points

### 1. **Dashboard Design Best Practices**

**The 5-Second Rule**: User should understand dashboard purpose in 5 seconds
- Clear title and purpose
- Logical grouping of related metrics
- Color-coding for severity (green=good, yellow=warning, red=critical)
- Most important metrics at the top

**Panel Types and Use Cases**:
- **Stat**: Single value, current status (e.g., "Assets Operational: 42")
- **Gauge**: Value with threshold (e.g., CPU usage 0-100%)
- **Graph/Timeseries**: Trends over time
- **Table**: Detailed lists with multiple columns
- **Bar Chart**: Comparisons or distributions
- **Pie/Donut**: Proportions and percentages

### 2. **Choosing the Right Visualization**

**For Status**: Use Stat panels with color mappings
```
1 = UP (green)
0 = DOWN (red)
```

**For Resource Usage**: Use Gauge with thresholds
```
0-70%: Green (healthy)
70-85%: Yellow (warning)
85-100%: Red (critical)
```

**For Trends**: Use Timeseries graphs
- Show historical data
- Multiple series for comparison
- Threshold lines for targets

**For Distributions**: Use Pie/Donut charts
- Show proportions
- Identify outliers
- Quick visual summary

**For Details**: Use Tables
- When you need specifics
- Multiple columns of data
- Sortable/filterable information

### 3. **Query Optimization**

**Use Recording Rules**: Instead of complex queries, use pre-computed metrics
```promql
# Instead of this (slow):
rate(container_cpu_usage_seconds_total{...}[5m])

# Use this (fast):
mantis:container:cpu_usage:rate5m
```

**Limit Result Sets**: Always use LIMIT in SQL queries
```sql
SELECT * FROM assets WHERE status != 'operational'
ORDER BY criticality
LIMIT 10  -- Don't fetch thousands of rows!
```

**Use Appropriate Time Ranges**:
- Infrastructure dashboards: Last 1 hour (fast changes)
- Business dashboards: Last 24 hours (slower changes)
- Trend analysis: Last 30 days (with aggregation)

### 4. **Dashboard Organization**

**By Audience**:
1. **Infrastructure Dashboard**: For DevOps/SRE teams
2. **Application Dashboard**: For Software Engineers
3. **ML Dashboard**: For Data Scientists
4. **Business Dashboard**: For Operations/Management

**By Purpose**:
- **Monitoring**: Is everything working?
- **Troubleshooting**: What's wrong and why?
- **Analysis**: What patterns exist?
- **Planning**: Do we need to scale?

### 5. **Refresh Intervals**

Choose based on data volatility:
- **10 seconds**: Real-time monitoring (infrastructure, app performance)
- **30 seconds**: Near real-time (business metrics)
- **1 minute**: Stable systems
- **5+ minutes**: Historical analysis, cost optimization

**Why it matters**:
- Faster refresh = more database load
- Slower refresh = might miss critical events
- Find the balance for your use case

---

## 🎨 Grafana Query Examples

### Infrastructure Queries (PromQL)

**CPU Usage by Container**:
```promql
mantis:container:cpu_usage:rate5m * 100
```
*Returns: CPU percentage per service*

**Top 5 Memory Consumers**:
```promql
topk(5, mantis:container:memory_usage:bytes)
```
*Returns: 5 services using most memory*

**Database Connection Utilization**:
```promql
mantis:postgres:connection_pool_usage:ratio * 100
```
*Returns: Percentage of max connections used*

### Application Queries (PromQL)

**Request Rate by Service**:
```promql
sum(mantis:http:requests:rate1m) by (job)
```
*Returns: Requests/sec per service*

**Error Percentage (5xx)**:
```promql
sum(rate(http_requests_total{status=~"5.."}[5m])) by (job)
/
sum(rate(http_requests_total[5m])) by (job) * 100
```
*Returns: Percentage of requests that are errors*

**P95 Latency**:
```promql
histogram_quantile(0.95,
  sum(rate(processing_duration_seconds_bucket[5m])) by (le, job)
)
```
*Returns: 95th percentile response time*

### Business Queries (SQL)

**Assets by Status**:
```sql
SELECT status, COUNT(*) as count
FROM assets
GROUP BY status
ORDER BY count DESC
```

**Critical Assets Offline**:
```sql
SELECT asset_code, name, location_line, updated_at
FROM assets
WHERE criticality = 'critical' AND status != 'operational'
ORDER BY updated_at DESC
```

**Maintenance Completion Rate**:
```sql
SELECT
  DATE_TRUNC('week', updated_at) as week,
  COUNT(*) as completed,
  AVG(EXTRACT(EPOCH FROM (updated_at - created_at))/3600) as avg_hours
FROM work_orders
WHERE status = 'completed'
  AND updated_at > NOW() - INTERVAL '90 days'
GROUP BY week
ORDER BY week
```

---

## 🔧 Useful Commands

### Access Grafana
```bash
# Grafana UI
open http://localhost:3001

# Default credentials
Username: admin
Password: admin
```

### Restart Grafana
```bash
# Restart to reload dashboards
docker-compose -f docker-compose.infrastructure.yml restart grafana

# Check logs
docker logs mantis-grafana -f
```

### Verify Dashboard Provisioning
```bash
# Check provisioning directory
ls -la infrastructure/docker/grafana/provisioning/dashboards/

# Check dashboard files
ls -la infrastructure/docker/grafana/dashboards/

# Verify mounts in container
docker exec mantis-grafana ls -la /etc/grafana/provisioning/dashboards/
docker exec mantis-grafana ls -la /var/lib/grafana/dashboards/
```

### Test Datasources
```bash
# Test Prometheus connection
curl -s http://localhost:9091/api/v1/query?query=up | jq

# Test PostgreSQL connection
docker exec mantis-postgres psql -U mantis -c "SELECT 1"

# Check Grafana datasources via API
curl -s -u admin:admin http://localhost:3001/api/datasources | jq
```

### Export Dashboard
```bash
# Get dashboard JSON
curl -s -u admin:admin \
  http://localhost:3001/api/dashboards/uid/mantis-infrastructure | jq
```

---

## 📊 Dashboard Access

Once Grafana is running, access your dashboards:

1. **Navigate to Grafana**: http://localhost:3001
2. **Login**: admin / admin (change password on first login)
3. **Find Dashboards**:
   - Click "Dashboards" icon (left sidebar)
   - Open "MANTIS" folder
   - Select dashboard:
     - `MANTIS - Infrastructure Monitoring`
     - `MANTIS - Application Performance`
     - `MANTIS - ML Metrics`
     - `MANTIS - Business Metrics`

### Dashboard URLs (after provisioning)
```
Infrastructure: http://localhost:3001/d/mantis-infrastructure
Application:    http://localhost:3001/d/mantis-application
ML Metrics:     http://localhost:3001/d/mantis-ml
Business:       http://localhost:3001/d/mantis-business
```

---

## 🎯 Dashboard Use Cases

### For DevOps Engineers
**Use**: Infrastructure Monitoring Dashboard
**When**:
- System is slow → Check CPU/Memory panels
- Service is down → Check service status panels
- Disk filling up → Check disk space panel
**Action**: Scale resources, restart services, investigate logs

### For Software Engineers
**Use**: Application Performance Dashboard
**When**:
- Users reporting errors → Check error rate panels
- Slow responses → Check latency panels
- Data not processing → Check Kafka lag panel
**Action**: Fix bugs, optimize code, scale services

### For Data Scientists
**Use**: ML Metrics Dashboard
**When**:
- Model predictions slow → Check inference latency
- Too many anomalies → Check detection rate
- Model accuracy drops → Check accuracy/F1 panels
**Action**: Retrain models, adjust thresholds, investigate data quality

### For Operations Managers
**Use**: Business Metrics Dashboard
**When**:
- Planning maintenance schedules → Check work order trends
- Assessing asset health → Check operational status
- Prioritizing resources → Check criticality distribution
**Action**: Allocate resources, schedule maintenance, replace assets

---

## ✅ Phase 4 Checklist

- [x] Dashboard provisioning configured
- [x] Infrastructure monitoring dashboard created (10 panels)
- [x] Application performance dashboard created (8 panels)
- [x] ML metrics dashboard created (9 panels)
- [x] Business metrics dashboard created (12 panels)
- [x] Grafana restarted with new dashboards
- [x] Documentation completed

**Phase 4 Status**: ✅ **COMPLETE**

---

## 🎉 Summary

You now have **4 comprehensive dashboards** that provide:

1. ✅ **39 visualization panels** across all dashboards
2. ✅ **Real-time monitoring** of infrastructure and applications
3. ✅ **ML model performance tracking** with latency and accuracy metrics
4. ✅ **Business KPIs** for asset management and maintenance operations
5. ✅ **Multiple datasources** (Prometheus, PostgreSQL, TimescaleDB, InfluxDB)
6. ✅ **Automatic provisioning** - dashboards load on Grafana startup
7. ✅ **Color-coded alerts** - green (healthy), yellow (warning), red (critical)
8. ✅ **Audience-specific views** - different dashboards for different teams

**Congratulations!** Your MANTIS system is now fully observable with beautiful, actionable dashboards! 📊

---

**Next Phase**: Phase 5 - Service Implementation (Building the actual MANTIS microservices)

**What You'll Build Next**:
- Ingestion IIoT Service (sensor data collection)
- Preprocessing Service (data cleaning and validation)
- Feature Extraction Service (signal processing)
- Anomaly Detection Service (ML-based anomaly detection)
- RUL Prediction Service (remaining useful life estimation)
- Maintenance Orchestrator Service (work order management)
