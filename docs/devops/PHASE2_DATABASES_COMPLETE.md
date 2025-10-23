# ✅ Phase 2: Databases - Completion Summary

**Date**: 2025-10-23
**Status**: COMPLETED
**Tutor**: Claude Code

---

## 📚 What You Learned in Phase 2

### 1. **Polyglot Persistence Strategy**

**Concept**: Using different databases for different data types - "The right tool for the right job"

**Why it matters**:
- No single database is perfect for everything
- Each MANTIS database serves a specific purpose
- This approach is used by companies like Netflix, Uber, and Airbnb

### 2. **Database Types in MANTIS**

| Database | Type | Purpose | Best For |
|----------|------|---------|----------|
| **PostgreSQL** | Relational (ACID) | Metadata, relationships | Complex queries, transactions |
| **TimescaleDB** | Time-series (SQL) | Historical sensor data | Time-based aggregations |
| **InfluxDB** | Time-series (NoSQL) | High-frequency streams | Ultra-fast writes |
| **Redis** | In-memory KV store | Cache, features | Sub-millisecond reads |

---

## ✅ What Was Completed

### PostgreSQL (Metadata Database)

**Tables Created**: 15 tables

#### Core Tables:
1. **`assets`** - Industrial equipment/machinery
   ```sql
   - asset_code, name, type, manufacturer, model
   - location (with PostGIS geometry support)
   - criticality (critical/high/medium/low)
   - status (operational/degraded/failed/maintenance)
   - JSONB metadata for flexible attributes
   ```

2. **`sensors`** - Sensor configuration
   ```sql
   - Links to assets (foreign key)
   - sensor_type (vibration, temperature, current, etc.)
   - Protocol configs (OPC UA, MQTT, Modbus)
   - Thresholds (warning/critical)
   ```

3. **`spare_parts`** - Inventory management
   ```sql
   - Part number, supplier, pricing
   - Stock levels and min thresholds
   - Lead times
   ```

4. **`work_orders`** - Maintenance tracking
   ```sql
   - Linked to assets
   - Priority, status, scheduled dates
   - Actual execution times
   - Cost tracking
   ```

5. **`anomalies`** - Anomaly detection journal
   ```sql
   - Timestamp, sensor, severity
   - ML model scores
   - Root cause analysis
   ```

6. **`rul_predictions`** - Remaining Useful Life history
   ```sql
   - Asset-level predictions
   - Confidence intervals
   - Model versions
   ```

7. **`maintenance_rules`** - Business rules engine
   ```sql
   - Condition-based triggers
   - Time-based schedules
   - Rule priorities
   ```

#### Additional Tables:
- `asset_spare_parts` (many-to-many relationship)
- `work_order_parts` (parts used in maintenance)
- `maintenance_history` (audit trail)
- `ml_models` (model registry)
- `kpi_snapshots` (performance metrics)

**Key Features**:
- ✅ PostGIS extension for spatial data (factory floor mapping)
- ✅ UUID primary keys for distributed systems
- ✅ JSONB columns for flexible metadata
- ✅ Proper indexes on frequently queried columns
- ✅ Foreign key constraints for data integrity
- ✅ Automatic timestamp triggers (`updated_at`)

---

### TimescaleDB (Time-Series Database)

**Hypertables Created**: 6 hypertables (confirmed via query)

1. **`sensor_data_raw`**
   - Raw sensor readings
   - Retention: 90 days
   - Compression: after 7 days
   - Partitioning: by timestamp

2. **`sensor_data_windowed`**
   - Pre-aggregated windows (1min, 5min, 1hour)
   - Retention: 180 days
   - Use: Faster dashboard queries

3. **`sensor_features`**
   - Computed features for ML
   - Statistical aggregations (mean, std, FFT)
   - Retention: 1 year

4. **`anomaly_scores`**
   - Real-time anomaly detection scores
   - Multiple algorithms (Isolation Forest, LSTM)
   - Retention: 180 days

5. **`rul_predictions_ts`**
   - Time-series of RUL predictions
   - Tracks degradation over time
   - Retention: 1 year

6. **`system_events`**
   - System-wide events log
   - Start/stop events, config changes
   - Retention: 90 days

**Materialized Views** (for performance):
- `sensor_data_hourly` - Hourly aggregations
- `sensor_data_daily` - Daily rollups
- `anomalies_hourly` - Anomaly counts per hour

**Key Features**:
- ✅ Automatic partitioning by time
- ✅ Compression policies (saves 90%+ storage)
- ✅ Retention policies (auto-delete old data)
- ✅ Continuous aggregates (real-time rollups)
- ✅ Time-bucket functions for analytics

---

### InfluxDB (High-Frequency Metrics)

**Bucket**: `sensors`
**Retention**: 30 days (720 hours)
**Organization**: mantis-org

**Use Cases**:
- Sensor data at millisecond precision
- High write throughput (100k+ points/sec)
- Real-time dashboards
- Short-term buffering before TimescaleDB

**Data Model**:
```
Measurement: sensor_readings
Tags: asset_id, sensor_id, sensor_type
Fields: value, quality, status
Timestamp: nanosecond precision
```

**Key Features**:
- ✅ Schema-less (flexible tags/fields)
- ✅ Built-in downsampling
- ✅ Flux query language
- ✅ Native Grafana integration

---

### Redis (Feature Store & Cache)

**Configuration**:
- Port: 6380 (updated to avoid conflicts)
- Persistence: AOF (append-only file) enabled
- Data structures: Strings, Hashes, Lists, Sets, Sorted Sets

**Use Cases in MANTIS**:

1. **Feature Store** (for ML):
   ```
   Key: feature:asset_123:latest
   Value: {vibration_rms: 0.45, temp_avg: 68.2, ...}
   TTL: 5 minutes
   ```

2. **Session Cache**:
   - API response caching
   - Computed aggregations
   - Temporary calculations

3. **Real-time Features**:
   - Latest sensor values
   - Rolling windows (last N readings)
   - Online feature computation

**Key Features**:
- ✅ Sub-millisecond latency
- ✅ TTL (time-to-live) support
- ✅ Pub/Sub messaging
- ✅ Lua scripting for atomic operations

---

## 🧪 Verification Tests Performed

### 1. Container Health
```bash
✅ All database containers running
✅ Health checks passing
✅ Proper port mappings
```

### 2. Schema Validation
```bash
✅ PostgreSQL: 15 tables created
✅ TimescaleDB: 6 hypertables confirmed
✅ InfluxDB: sensors bucket exists
✅ Redis: PING successful
```

### 3. Extensions & Features
```bash
✅ PostGIS extension installed
✅ TimescaleDB extension active
✅ Hypertables properly partitioned
✅ Compression policies enabled
```

---

## 📊 Database Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                     MANTIS Data Layer                        │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
┌───────▼────────┐  ┌─────────▼─────────┐  ┌──────▼──────┐
│   PostgreSQL   │  │   TimescaleDB     │  │  InfluxDB   │
│   (Metadata)   │  │  (Time-Series)    │  │ (Real-time) │
├────────────────┤  ├───────────────────┤  ├─────────────┤
│ • Assets       │  │ • sensor_data_raw │  │ • sensors   │
│ • Sensors      │  │ • features        │  │   (30d)     │
│ • Work Orders  │  │ • anomalies       │  │             │
│ • Spare Parts  │  │ • predictions     │  │ High-freq   │
│ • ML Models    │  │                   │  │ streaming   │
│                │  │ Compressed        │  │             │
│ ACID           │  │ Partitioned       │  │ Schemaless  │
│ Relational     │  │ Time-optimized    │  │ Fast writes │
└────────────────┘  └───────────────────┘  └─────────────┘
                              │
                    ┌─────────▼─────────┐
                    │       Redis       │
                    │  (Feature Store)  │
                    ├───────────────────┤
                    │ • Latest features │
                    │ • Cached queries  │
                    │ • Sessions        │
                    │                   │
                    │ In-Memory         │
                    │ Sub-ms latency    │
                    └───────────────────┘
```

---

## 🎓 Key Learning Points

### 1. **Database Initialization**
- Init scripts run only on **first container creation**
- Scripts in `/docker-entrypoint-initdb.d/` execute alphabetically
- If a script fails, subsequent scripts don't run
- Use `CREATE TABLE IF NOT EXISTS` for idempotency

### 2. **Time-Series Optimization**
TimescaleDB hypertables provide:
- **Partitioning**: Data split by time chunks (1 day default)
- **Compression**: Older data compressed (saves 90%+ space)
- **Retention**: Auto-delete data beyond retention period
- **Fast queries**: Optimized for time-range queries

### 3. **Why Multiple Databases?**
| Scenario | Database | Reason |
|----------|----------|--------|
| "Show all assets for this factory line" | PostgreSQL | Complex JOIN queries |
| "Average vibration last 24 hours" | TimescaleDB | Time-based aggregation |
| "Current temperature of sensor XYZ" | InfluxDB | Real-time, high-frequency |
| "Latest ML features for prediction" | Redis | Ultra-fast access |

### 4. **Data Flow in MANTIS**
```
Sensor → Kafka → InfluxDB (real-time buffer)
                     ↓
                 TimescaleDB (historical storage)
                     ↓
                 Feature Engineering
                     ↓
                 Redis (feature cache)
                     ↓
                 ML Model (prediction)
                     ↓
                 PostgreSQL (results storage)
```

---

## 📝 Next Steps (Phase 3)

Now that databases are ready, Phase 3 will focus on:
1. **Data Ingestion Services** - Connecting to industrial protocols
2. **Kafka Producers** - Streaming data into the system
3. **Data Validation** - Ensuring data quality
4. **Initial Data Population** - Adding sample assets/sensors

---

## 🔧 Useful Commands for Database Management

### PostgreSQL
```bash
# Connect to database
docker exec -it mantis-postgres psql -U mantis -d mantis

# List all tables
\dt

# Describe table structure
\d assets

# Count records
SELECT COUNT(*) FROM assets;
```

### TimescaleDB
```bash
# Connect
docker exec -it mantis-timescaledb psql -U mantis -d mantis_timeseries

# List hypertables
SELECT * FROM timescaledb_information.hypertables;

# Check compression
SELECT * FROM timescaledb_information.compression_settings;

# Query time-series
SELECT time_bucket('1 hour', timestamp) as hour,
       avg(value)
FROM sensor_data_raw
WHERE timestamp > NOW() - INTERVAL '24 hours'
GROUP BY hour;
```

### InfluxDB
```bash
# List buckets
docker exec mantis-influxdb influx bucket list

# Query data (Flux)
docker exec mantis-influxdb influx query 'from(bucket:"sensors") |> range(start: -1h)'
```

### Redis
```bash
# Connect
docker exec -it mantis-redis redis-cli

# Test
PING

# Set/Get value
SET feature:test "value"
GET feature:test

# List keys
KEYS *
```

---

## ✅ Phase 2 Checklist

- [x] PostgreSQL schema deployed (15 tables)
- [x] TimescaleDB hypertables created (6 hypertables)
- [x] InfluxDB bucket configured
- [x] Redis cache operational
- [x] PostGIS extension installed
- [x] Retention policies configured
- [x] Compression policies enabled
- [x] All services health-checked
- [x] Documentation created

**Phase 2 Status**: ✅ **COMPLETE**

---

**Congratulations!** You now have a production-grade, multi-database architecture ready for predictive maintenance! 🎉
