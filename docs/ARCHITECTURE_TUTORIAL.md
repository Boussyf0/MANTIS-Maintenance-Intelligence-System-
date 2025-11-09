# MANTIS Architecture Tutorial
## Understanding How the 7 Microservices Work Together

---

## 🎯 The Goal

**Problem:** Factories lose $50 billion/year from unexpected equipment failures
**Solution:** MANTIS predicts failures BEFORE they happen, so you can fix things during planned maintenance

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                        FACTORY FLOOR                                 │
│  ┌──────┐  ┌──────┐  ┌──────┐  ┌──────┐                           │
│  │Motor │  │Pump  │  │ CNC  │  │Robot │  ← Real Equipment         │
│  └──┬───┘  └──┬───┘  └──┬───┘  └──┬───┘                           │
│     │         │         │         │                                 │
│  ┌──▼─────────▼─────────▼─────────▼───┐                           │
│  │  PLC/SCADA/Edge Devices (OPC UA)   │ ← Data Collection         │
│  └──────────────┬──────────────────────┘                           │
└─────────────────┼────────────────────────────────────────────────────┘
                  │ Sensor Data (temperature, vibration, current...)
                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    MANTIS PLATFORM                                   │
│                                                                       │
│  ┌────────────────────────────────────────────────────────────┐    │
│  │ 1️⃣  INGESTION IIoT SERVICE (Port 8001) ✅ COMPLETE         │    │
│  │    Language: Java/Spring Boot                               │    │
│  │    Job: Collect sensor data from factory equipment          │    │
│  │    Input: OPC UA, MQTT, Modbus protocols                    │    │
│  │    Output: Raw sensor data → Kafka topic "sensor.raw"       │    │
│  └────────────────────────────────────────────────────────────┘    │
│                              │                                       │
│                              │ Kafka Stream                          │
│                              ▼                                       │
│  ┌────────────────────────────────────────────────────────────┐    │
│  │ 2️⃣  PREPROCESSING SERVICE (Port 8002) ⏭️ NEXT              │    │
│  │    Language: Python/FastAPI                                 │    │
│  │    Job: Clean and prepare data for analysis                 │    │
│  │    - Remove noise and outliers                              │    │
│  │    - Fill missing values                                     │    │
│  │    - Normalize sensor readings                               │    │
│  │    - Create time windows (e.g., last 30 cycles)             │    │
│  │    Input: Kafka "sensor.raw"                                │    │
│  │    Output: Clean data → Kafka "sensor.preprocessed"         │    │
│  └────────────────────────────────────────────────────────────┘    │
│                              │                                       │
│                              │ Kafka Stream                          │
│                              ▼                                       │
│  ┌────────────────────────────────────────────────────────────┐    │
│  │ 3️⃣  FEATURE EXTRACTION SERVICE (Port 8003)                  │    │
│  │    Language: Python/FastAPI                                 │    │
│  │    Job: Calculate meaningful patterns from raw data         │    │
│  │    - Time features: mean, std, trend, slope                 │    │
│  │    - Frequency features: FFT, wavelets                      │    │
│  │    - Statistical: kurtosis, skewness                        │    │
│  │    Input: Kafka "sensor.preprocessed"                       │    │
│  │    Output: Features → Kafka "sensor.features"               │    │
│  └────────────────────────────────────────────────────────────┘    │
│                              │                                       │
│                              ├──────────────┬─────────────────┐     │
│                              │              │                 │     │
│                              ▼              ▼                 ▼     │
│  ┌──────────────────────┐  ┌──────────────────┐  ┌──────────────┐ │
│  │ 4️⃣  ANOMALY          │  │ 5️⃣  RUL          │  │ 6️⃣  MAINTENANCE│ │
│  │    DETECTION         │  │    PREDICTION    │  │    ORCHESTRATOR│ │
│  │    (Port 8004)       │  │    (Port 8005)   │  │    (Port 8006) │ │
│  │                      │  │                  │  │                │ │
│  │  "Is something       │  │  "How much time  │  │  "What should  │ │
│  │   wrong?"            │  │   until failure?"│  │   we do?"      │ │
│  │                      │  │                  │  │                │ │
│  │  ML: PyOD           │  │  ML: LSTM,XGBoost│  │  Rules Engine  │ │
│  │  Output: Anomaly    │  │  Output: RUL hrs │  │  Output: Work  │ │
│  │  score 0-100        │  │                  │  │  orders        │ │
│  └──────────┬───────────┘  └────────┬─────────┘  └───────┬────────┘ │
│             │                       │                    │          │
│             └───────────────────────┴────────────────────┘          │
│                                     │                                │
│                                     ▼                                │
│  ┌────────────────────────────────────────────────────────────┐    │
│  │ 7️⃣  DASHBOARD (Port 3000)                                   │    │
│  │    Language: React.js                                       │    │
│  │    Job: Show everything to factory operators                │    │
│  │    - Real-time equipment health                             │    │
│  │    - RUL predictions                                         │    │
│  │    - Maintenance recommendations                             │    │
│  │    - Alerts and notifications                                │    │
│  └────────────────────────────────────────────────────────────┘    │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                    INFRASTRUCTURE LAYER                               │
│                                                                       │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐           │
│  │  Kafka   │  │PostgreSQL│  │TimescaleDB│ │ InfluxDB │           │
│  │ Streaming│  │Metadata  │  │Time Series│  │ Metrics  │           │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘           │
│                                                                       │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐           │
│  │  MinIO   │  │  MLflow  │  │ Prometheus│  │ Grafana  │           │
│  │ Storage  │  │ML Models │  │ Monitoring│  │ Dashboards│          │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘           │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🔍 Detailed Service Explanations

### 1️⃣ **Ingestion IIoT Service** ✅ (COMPLETE)

**Analogy:** The "ears" of the system - listens to all factory equipment

**Real-World Example:**
```
Motor sends: "I'm vibrating at 45 Hz, temperature is 75°C"
Service receives this every 100ms via OPC UA protocol
Sends to Kafka: {"motor_id": "M001", "vibration": 45, "temp": 75, "timestamp": "..."}
```

**Why Java/Spring Boot?**
- Industrial protocols (OPC UA) have mature Java libraries
- Spring Boot = production-ready with monitoring built-in
- JVM handles high-throughput data streams efficiently

---

### 2️⃣ **Preprocessing Service** ⏭️ (NEXT TO BUILD)

**Analogy:** The "data janitor" - cleans messy sensor data

**Why do we need this?**

**Problem:** Raw sensor data is MESSY:
```
Time    Vibration   Temperature
10:00   45 Hz       75°C        ✅ Good
10:01   46 Hz       76°C        ✅ Good
10:02   999 Hz      75°C        ❌ Sensor glitch!
10:03   NULL        NULL        ❌ Sensor offline!
10:04   47 Hz       150°C       ❌ Unrealistic spike
```

**Solution:** Preprocessing fixes this:
```python
# 1. Remove outliers (999 Hz is impossible)
# 2. Fill missing values (interpolate or use last known value)
# 3. Smooth spikes (75→150→75 is noise, smooth to 75→77→75)
# 4. Normalize (convert all sensors to 0-1 scale)
# 5. Create windows (group last 30 readings together)
```

**Output:** Clean, analysis-ready data
```
Window: Cycles 1-30
  vibration_mean: 45.2 Hz
  vibration_std: 1.3 Hz
  temp_mean: 75.5°C
  temp_trend: +0.1°C/cycle (slowly increasing)
```

**Why Python/FastAPI?**
- Python has pandas, numpy for data manipulation
- FastAPI = modern, async, auto-generates API docs
- Easy integration with ML libraries later

---

### 3️⃣ **Feature Extraction Service**

**Analogy:** The "pattern finder" - turns data into insights

**Example:** Instead of 1000 raw temperature readings, extract:
- Average temperature: 75°C
- Temperature rising? +0.5°C per hour (trend)
- Temperature stability: Low (std deviation = 5°C means unstable)
- Frequency analysis: Peaks at 50 Hz (might indicate electrical issue)

**Why this matters:** ML models can't understand raw sensor streams, but they CAN understand patterns like "temperature rising + vibration increasing = bearing failure soon"

---

### 4️⃣ **Anomaly Detection Service**

**Analogy:** The "watchdog" - barks when something looks wrong

**How it works:**
1. Train on HEALTHY equipment data (from C-MAPSS)
2. Learn what "normal" looks like
3. When real data comes in, compare to normal
4. If too different → anomaly detected!

**Example:**
```
Normal motor: vibration 40-50 Hz
Today's reading: 85 Hz
Anomaly score: 95/100 (very abnormal!)
Alert: "Motor M001 showing unusual vibration"
```

**ML Models:** Isolation Forest, Autoencoders, One-Class SVM

---

### 5️⃣ **RUL Prediction Service**

**Analogy:** The "fortune teller" - predicts when equipment will fail

**How it works:**
1. Trained on C-MAPSS data (we know when engines failed)
2. Learns degradation patterns
3. Predicts: "This motor has 240 hours left before failure"

**Example:**
```
Input: Motor running for 5000 hours, vibration increasing, temp stable
Model (LSTM): Analyzes trend over last 500 hours
Output: RUL = 240 hours (±50 hours confidence)
```

**Why this is valuable:**
- Schedule maintenance for 200 hours from now
- Order replacement parts ahead of time
- Avoid unexpected downtime during production

---

### 6️⃣ **Maintenance Orchestrator**

**Analogy:** The "decision maker" - decides what actions to take

**Combines information:**
```
Anomaly Service says: "70% abnormal"
RUL Service says: "120 hours left"
Business rules: "This motor is critical for production line A"

Decision:
  Priority: HIGH
  Action: Schedule maintenance in 3 days
  Order part: Bearing #XYZ-123
  Notify: Maintenance team + Production manager
```

**Optimization:** Uses OR-Tools to schedule maintenance across entire factory efficiently

---

### 7️⃣ **Dashboard**

**Analogy:** The "control panel" - shows everything to humans

**What operators see:**
- Live equipment status (green/yellow/red)
- RUL predictions with confidence bars
- Active alerts and recommendations
- Historical trends and patterns

---

## 🌊 Data Flow Example: Complete Journey

**Minute 0:**
```
1. Motor M001 sends vibration=45Hz to Ingestion Service
2. Ingestion → Kafka "sensor.raw"
```

**Minute 0.1:**
```
3. Preprocessing Service reads from Kafka
4. Cleans data, creates 30-cycle window
5. Sends to Kafka "sensor.preprocessed"
```

**Minute 0.2:**
```
6. Feature Extraction reads preprocessed data
7. Calculates: mean=45.2, std=1.3, trend=+0.1
8. Sends to Kafka "sensor.features"
```

**Minute 0.3:**
```
9. Anomaly Detection: Analyzes features → Score: 15/100 (normal)
10. RUL Prediction: Analyzes features → RUL: 500 hours
11. Both send results to Kafka
```

**Minute 0.4:**
```
12. Maintenance Orchestrator: Reads anomaly + RUL
13. Applies rules: "RUL > 200 hours = OK, no action needed"
14. Saves to database
```

**Minute 0.5:**
```
15. Dashboard polls database
16. Shows operator: "Motor M001: Healthy ✅, RUL: 500h"
```

**Total latency: < 1 second from sensor to dashboard!**

---

## 🔑 Key Architectural Decisions

### Why Kafka?
- **Decoupling:** Services don't talk directly (if one crashes, others continue)
- **Replay:** Can reprocess old data (useful for testing new ML models)
- **Scale:** Handles millions of messages/second
- **Persistence:** Data is stored, not lost if service is down

### Why Multiple Databases?
- **PostgreSQL:** Structured data (equipment IDs, maintenance records)
- **TimescaleDB:** Time-series sensor data (optimized for time-based queries)
- **InfluxDB:** High-frequency metrics (ingests 100K points/second)
- **MinIO:** Large files (ML models, raw data archives)

### Why Microservices Not Monolith?
| Aspect | Monolith | Microservices (MANTIS) |
|--------|----------|------------------------|
| **Scaling** | Must scale everything | Scale only busy services |
| **Technology** | One language | Best tool for each job |
| **Failure** | All or nothing | Isolated failures |
| **Development** | Teams block each other | Teams work independently |
| **Deployment** | Big bang releases | Deploy services separately |

---

## 📊 MANTIS vs Traditional Maintenance

### Traditional (Reactive):
```
Equipment breaks → Production stops → Emergency repair → $$$
Cost: $125K/hour downtime + emergency labor
```

### MANTIS (Predictive):
```
MANTIS predicts failure → Schedule repair → Fix during planned downtime
Cost: Planned labor + $0 production loss
Savings: ~80% cost reduction
```

---

## 🎓 Key Concepts to Remember

1. **Microservices = Specialized workers**, each doing one job well
2. **Kafka = Highway**, data flows between services
3. **ML Pipeline = Assembly line**, raw data → features → predictions
4. **Real-time = Fast**, from sensor to alert in < 5 seconds
5. **Scalable = Flexible**, handle 1 machine or 1000 machines

---

## 🚀 Next Steps

Now that you understand the architecture, we'll build **Service #2: Preprocessing**.

You'll learn:
- How to consume from Kafka
- Data cleaning techniques
- Time-series windowing
- FastAPI service structure
- Docker containerization
- Testing strategies

Ready to start building? 🛠️
