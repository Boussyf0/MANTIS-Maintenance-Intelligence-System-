# Preprocessing Service Tutorial
## Building MANTIS Service #2

---

## 🎯 Learning Objectives

By the end of this tutorial, you will understand:
1. Why preprocessing is critical for ML pipelines
2. Common data quality issues and how to fix them
3. How to build a production-ready FastAPI service
4. How to consume from and produce to Kafka
5. How to test and deploy the service

---

## 📊 The Data Quality Problem

### Real Sensor Data Issues

#### **Problem 1: Sensor Glitches (Outliers)**
```
Time    Temperature    Issue
10:00   75.2°C        ✅ Normal
10:01   75.5°C        ✅ Normal
10:02   999.9°C       ❌ Sensor returned error code as temperature!
10:03   75.3°C        ✅ Normal
```
**Impact:** ML model thinks temperature spiked to 999°C → false alarm!

**Solution:** Detect and remove outliers using statistical methods (IQR, Z-score)

---

#### **Problem 2: Missing Data**
```
Time    Vibration    Cause
10:00   45 Hz       ✅ Good
10:01   46 Hz       ✅ Good
10:02   NULL        ❌ Sensor disconnected
10:03   NULL        ❌ Still disconnected
10:04   47 Hz       ✅ Reconnected
```
**Impact:** ML models can't handle NULL values → crash or skip data

**Solutions:**
- **Forward fill:** Use last known value (45→45→45→45→47)
- **Interpolation:** Estimate middle values (45→46→46.33→46.66→47)
- **Drop:** If too many missing values, discard the window

---

#### **Problem 3: Different Scales**
```
Sensor          Min    Max     Unit
Temperature     20     100     °C
Vibration       0      1000    Hz
Pressure        0      10      bar
Current         0      500     A
```
**Problem:** Vibration (0-1000) dominates calculations over Pressure (0-10)

**Solution:** Normalize all sensors to 0-1 range
```python
normalized = (value - min) / (max - min)
# Temperature 75°C → (75-20)/(100-20) = 0.6875
# Vibration 500Hz → (500-0)/(1000-0) = 0.5
```

---

#### **Problem 4: High-Frequency Noise**
```
Time    Vibration (Raw)    Vibration (Smoothed)
10:00   45.2 Hz           45.0 Hz
10:01   46.8 Hz           45.5 Hz
10:02   44.1 Hz           45.3 Hz
10:03   47.3 Hz           45.8 Hz
```
**Problem:** Sensor picks up electrical noise, makes data "jumpy"

**Solution:** Apply smoothing (moving average, exponential smoothing)

---

#### **Problem 5: No Context (No Time Windows)**
```
Single Reading:
  Temperature: 85°C

Question: Is this bad?
Answer: We don't know! Could be:
  - Normal operating temperature
  - Heating up (was 60°C 5 minutes ago) ← Concerning!
  - Cooling down (was 100°C 5 minutes ago) ← Good!
```

**Solution:** Create time windows showing history
```python
Window (last 30 cycles):
  current_temp: 85°C
  avg_temp: 75°C
  max_temp: 85°C
  temp_trend: +0.5°C/cycle  ← This tells us it's heating up!
  temp_std: 4.2°C
```

---

## 🏗️ Preprocessing Service Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                  PREPROCESSING SERVICE                           │
│                                                                   │
│  INPUT                                                            │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ Kafka Consumer                                           │   │
│  │ Topic: "sensor.raw"                                      │   │
│  │ Format: {"asset_id": "...", "sensor_id": "...",         │   │
│  │          "value": 45.2, "timestamp": "..."}             │   │
│  └──────────────────┬───────────────────────────────────────┘   │
│                     │                                             │
│                     ▼                                             │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ STEP 1: Validation                                       │   │
│  │ - Check required fields exist                            │   │
│  │ - Validate data types (number, not string)              │   │
│  │ - Check timestamp is recent (< 1 hour old)             │   │
│  └──────────────────┬───────────────────────────────────────┘   │
│                     │ ✅ Valid data                              │
│                     ▼                                             │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ STEP 2: Outlier Detection                                │   │
│  │ - Calculate z-score: (value - mean) / std                │   │
│  │ - If |z-score| > 3 → outlier                            │   │
│  │ - Replace outliers with median                           │   │
│  └──────────────────┬───────────────────────────────────────┘   │
│                     │                                             │
│                     ▼                                             │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ STEP 3: Missing Value Handling                           │   │
│  │ - Forward fill (use last known value)                    │   │
│  │ - If no previous value → use sensor type default         │   │
│  └──────────────────┬───────────────────────────────────────┘   │
│                     │                                             │
│                     ▼                                             │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ STEP 4: Smoothing                                        │   │
│  │ - Apply exponential moving average                       │   │
│  │ - Smoothing factor α = 0.3                               │   │
│  │ - smoothed = α × new_value + (1-α) × old_value          │   │
│  └──────────────────┬───────────────────────────────────────┘   │
│                     │                                             │
│                     ▼                                             │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ STEP 5: Normalization                                    │   │
│  │ - Min-Max scaling: (x - min) / (max - min)              │   │
│  │ - Result: all values between 0 and 1                     │   │
│  └──────────────────┬───────────────────────────────────────┘   │
│                     │                                             │
│                     ▼                                             │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ STEP 6: Time Window Creation                             │   │
│  │ - Group last 30 cycles per asset                         │   │
│  │ - Calculate window statistics:                           │   │
│  │   * mean, median, std, min, max                          │   │
│  │   * trend (linear regression slope)                      │   │
│  │   * rate of change                                        │   │
│  └──────────────────┬───────────────────────────────────────┘   │
│                     │                                             │
│                     ▼                                             │
│  OUTPUT                                                           │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ Kafka Producer                                           │   │
│  │ Topic: "sensor.preprocessed"                             │   │
│  │ Format: {                                                │   │
│  │   "asset_id": "...",                                     │   │
│  │   "window": {                                            │   │
│  │     "cycles": 30,                                        │   │
│  │     "sensors": {                                         │   │
│  │       "temperature": {                                   │   │
│  │         "mean": 75.2, "std": 2.1,                        │   │
│  │         "trend": +0.5, "current": 76.0                   │   │
│  │       }                                                   │   │
│  │     }                                                     │   │
│  │   }                                                       │   │
│  │ }                                                         │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                   │
│  SIDE EFFECTS                                                     │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ TimescaleDB Storage                                      │   │
│  │ - Save preprocessed data for historical analysis         │   │
│  │ - Optimized time-series queries                          │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ Prometheus Metrics                                       │   │
│  │ - Messages processed per second                          │   │
│  │ - Processing latency                                      │   │
│  │ - Outliers detected count                                │   │
│  │ - Errors count                                            │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 💻 Technology Stack Explained

### **FastAPI** (Web Framework)
**Why FastAPI and not Flask/Django?**

| Feature | FastAPI | Flask | Django |
|---------|---------|-------|--------|
| **Speed** | Very fast (async) | Slower (sync) | Slower (sync) |
| **Type hints** | Required ✅ | Optional | Optional |
| **Auto docs** | Yes ✅ (Swagger) | No | No |
| **Data validation** | Built-in ✅ (Pydantic) | Manual | Django Forms |
| **Async support** | Native ✅ | Limited | Limited |

**Example:**
```python
from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI()

# Pydantic automatically validates input
class SensorData(BaseModel):
    sensor_id: str
    value: float  # Must be a number!
    timestamp: str

@app.post("/process")
async def process_sensor(data: SensorData):
    # If 'value' is not a float, FastAPI returns 422 error automatically!
    return {"processed": data.value * 2}
```

---

### **Kafka** (Message Streaming)
**Why Kafka and not REST APIs?**

**REST Approach (Bad for this use case):**
```
Ingestion → HTTP POST → Preprocessing → HTTP POST → Feature Extraction
```
Problems:
- ❌ If Preprocessing is down, Ingestion fails (tight coupling)
- ❌ No message persistence (data lost if service crashes)
- ❌ Synchronous (slow)
- ❌ Hard to scale

**Kafka Approach (Good!):**
```
Ingestion → Kafka Topic → Preprocessing → Kafka Topic → Feature Extraction
```
Benefits:
- ✅ Services decoupled (if Preprocessing is down, data waits in Kafka)
- ✅ Messages persisted (can replay if needed)
- ✅ Asynchronous (fast)
- ✅ Easy to scale (add more consumers)

---

### **Pandas** (Data Manipulation)
**Why Pandas?**

Handles time-series data elegantly:
```python
import pandas as pd

# Raw data
data = [
    {"time": "10:00", "temp": 75.2},
    {"time": "10:01", "temp": None},   # Missing!
    {"time": "10:02", "temp": 999.9},  # Outlier!
]

df = pd.DataFrame(data)

# One line to fix!
df['temp'] = df['temp'].fillna(method='ffill')  # Fill missing
df['temp'] = df['temp'].clip(0, 200)            # Cap outliers

# Calculate statistics
df['temp'].mean()      # 75.2
df['temp'].rolling(3).mean()  # 3-point moving average
```

---

## 🔧 Key Preprocessing Techniques

### 1. Outlier Detection with Z-Score

**Concept:** How many standard deviations away from the mean?

```python
import numpy as np

def detect_outliers_zscore(values, threshold=3):
    """
    Z-score = (value - mean) / std

    If |z-score| > 3, the value is an outlier
    (99.7% of data is within 3 standard deviations)
    """
    mean = np.mean(values)
    std = np.std(values)

    z_scores = [(x - mean) / std for x in values]
    outliers = [abs(z) > threshold for z in z_scores]

    return outliers

# Example
temps = [75, 76, 75, 999, 74]  # 999 is clearly wrong
outliers = detect_outliers_zscore(temps)
# Result: [False, False, False, True, False]
```

**Why Z-score?**
- Simple and fast
- Works well for normally distributed data
- Threshold of 3 is standard (99.7% confidence)

---

### 2. Time Window Aggregation

**Concept:** Group sequential data points to see trends

```python
def create_time_window(df, window_size=30):
    """
    Create rolling windows of sensor data

    Example: Last 30 cycles of temperature readings
    """
    windows = []

    for i in range(len(df) - window_size + 1):
        window_data = df.iloc[i:i+window_size]

        window_features = {
            'mean': window_data['value'].mean(),
            'std': window_data['value'].std(),
            'min': window_data['value'].min(),
            'max': window_data['value'].max(),
            'trend': calculate_trend(window_data['value']),
            'current': window_data['value'].iloc[-1]
        }

        windows.append(window_features)

    return windows

def calculate_trend(values):
    """Linear regression slope"""
    x = np.arange(len(values))
    y = values.values
    slope = np.polyfit(x, y, 1)[0]  # Fit linear line, get slope
    return slope
```

**Example Output:**
```python
Window 1 (cycles 1-30):
  mean: 75.2°C
  std: 1.5°C
  trend: +0.1°C/cycle  # Slowly heating up
  current: 76°C

Window 2 (cycles 2-31):
  mean: 75.5°C
  std: 1.6°C
  trend: +0.2°C/cycle  # Heating up faster!
  current: 77°C
```

---

### 3. Exponential Smoothing

**Concept:** Smooth noisy data while giving more weight to recent values

```python
def exponential_smoothing(values, alpha=0.3):
    """
    Exponentially Weighted Moving Average (EWMA)

    alpha: Smoothing factor (0-1)
      - High alpha (0.7-0.9): Responsive to changes
      - Low alpha (0.1-0.3): Smoother but slower to react
    """
    smoothed = [values[0]]  # Start with first value

    for i in range(1, len(values)):
        new_value = alpha * values[i] + (1 - alpha) * smoothed[i-1]
        smoothed.append(new_value)

    return smoothed

# Example
raw = [45, 48, 44, 46, 47, 44, 49]
smoothed = exponential_smoothing(raw, alpha=0.3)
# Result: [45.0, 45.9, 45.33, 45.53, 45.97, 45.38, 46.47]
# Notice how smoothed values change gradually!
```

---

## 📁 Service Directory Structure

```
services/preprocessing/
├── src/
│   ├── __init__.py
│   ├── main.py                  # FastAPI app entry point
│   ├── config/
│   │   ├── __init__.py
│   │   └── settings.py          # Configuration (Kafka URLs, etc.)
│   ├── models/
│   │   ├── __init__.py
│   │   ├── sensor_data.py       # Pydantic models for validation
│   │   └── preprocessed_data.py
│   ├── services/
│   │   ├── __init__.py
│   │   ├── kafka_service.py     # Kafka consumer/producer
│   │   ├── preprocessing.py     # Core preprocessing logic
│   │   └── storage_service.py   # TimescaleDB operations
│   ├── utils/
│   │   ├── __init__.py
│   │   ├── outlier_detection.py
│   │   ├── normalization.py
│   │   └── windowing.py
│   └── api/
│       ├── __init__.py
│       └── routes.py            # REST API endpoints
├── tests/
│   ├── __init__.py
│   ├── test_preprocessing.py
│   ├── test_kafka.py
│   └── test_windowing.py
├── Dockerfile
├── requirements.txt
├── README.md
└── docker-compose.test.yml
```

---

## 🎓 Key Concepts Summary

### 1. **Why Preprocess?**
- Clean messy sensor data
- Remove outliers and fill missing values
- Normalize different scales
- Create time context (windows)
- Prepare data for ML models

### 2. **Key Techniques**
- **Outlier Detection:** Z-score, IQR
- **Missing Values:** Forward fill, interpolation
- **Smoothing:** Exponential weighted average
- **Normalization:** Min-max scaling
- **Windowing:** Rolling time windows with statistics

### 3. **Why These Technologies?**
- **FastAPI:** Fast, async, auto-validates
- **Kafka:** Decouples services, persists messages
- **Pandas:** Best tool for time-series data manipulation
- **TimescaleDB:** Optimized for time-series storage

---

## 🚀 Next: Let's Build It!

Now that you understand:
- ✅ What preprocessing does
- ✅ Why each technique is needed
- ✅ How the service fits in the architecture
- ✅ Which technologies we'll use

**Ready to write code?**

I'll guide you through:
1. Setting up the project structure
2. Writing the preprocessing logic
3. Connecting to Kafka
4. Creating the FastAPI service
5. Adding tests
6. Dockerizing the service

**Want to start building now, or do you have questions first?** 🎯
