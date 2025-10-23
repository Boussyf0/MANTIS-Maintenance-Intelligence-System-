# 🔗 Distributed Tracing Context Propagation Guide

## What is Context Propagation?

**Context propagation** is the mechanism that allows traces to flow across service boundaries. When Service A calls Service B, the trace context (trace ID, span ID, etc.) must be passed along so that both operations appear in the same trace.

---

## 📚 Key Concepts

### 1. **Trace Context**

A trace context contains:
- **Trace ID**: Unique identifier for the entire request flow
- **Span ID**: Unique identifier for the current operation
- **Parent Span ID**: Link to the calling operation
- **Trace Flags**: Sampling decision, debug flags

### 2. **Propagation Formats**

Different systems use different header formats:

| Format | Header(s) | Used By |
|--------|-----------|---------|
| **W3C Trace Context** | `traceparent`, `tracestate` | OpenTelemetry default |
| **Jaeger** | `uber-trace-id` | Jaeger native |
| **B3** | `X-B3-TraceId`, `X-B3-SpanId`, etc. | Zipkin, Istio |
| **Baggage** | `baggage` | Cross-cutting concerns |

**Recommendation**: Use **W3C Trace Context** (OpenTelemetry default) for MANTIS.

---

## 🔄 How Context Propagation Works

```
┌─────────────────────────────────────────────────────────────────┐
│                   Trace Context Flow                             │
└─────────────────────────────────────────────────────────────────┘

Client Request
│
│  traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01
│
▼
┌──────────────────────┐
│  Ingestion Service   │  Extract context from headers
│  (Span: ingest)      │  ↓
│                      │  Create child span
│  Trace ID: 4bf92... │
│  Span ID: 00f067...  │
└──────────────────────┘
         │
         │  HTTP Request to Preprocessing
         │  Inject context into headers
         │  traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-5e107953b4e91234-01
         │
         ▼
┌──────────────────────┐
│ Preprocessing Service│  Extract context (same trace ID!)
│  (Span: preprocess)  │  ↓
│                      │  Create child span
│  Trace ID: 4bf92... │
│  Span ID: 5e1079...  │  (Different span ID)
│  Parent: 00f067...   │  (Points to ingestion span)
└──────────────────────┘
         │
         │  Kafka Message
         │  Inject context into message headers
         │
         ▼
┌──────────────────────┐
│  Feature Extraction  │  Extract context from Kafka headers
│  (Span: extract)     │  ↓
│                      │  Create child span
│  Trace ID: 4bf92... │  (Same trace ID!)
│  Span ID: 7a2088...  │  (New span ID)
│  Parent: 5e1079...   │  (Points to preprocessing span)
└──────────────────────┘

Result: Single distributed trace with 3 spans!
```

---

## 🛠️ Implementation Guide

### 1. **HTTP Requests (Service-to-Service)**

#### Sending Service (Client)

```python
from opentelemetry.propagate import inject
import requests

# Create headers dict
headers = {}

# Inject trace context into headers
inject(headers)

# Make request with propagated context
response = requests.post(
    "http://other-service/api/endpoint",
    json=data,
    headers=headers  # Contains traceparent header
)
```

#### Receiving Service (Server)

With FastAPI auto-instrumentation, context is **automatically extracted**:

```python
from opentelemetry.instrumentation.fastapi import FastAPIInstrumentor
from fastapi import FastAPI

app = FastAPI()
FastAPIInstrumentor.instrument_app(app)  # Automatic context extraction!

@app.post("/api/endpoint")
async def endpoint(data: dict):
    # Context is already extracted from request headers
    # Any spans created here will be children of the incoming span
    pass
```

---

### 2. **Kafka Messages**

#### Producer (Send Message with Context)

```python
from opentelemetry.propagate import inject
from opentelemetry import trace
from kafka import KafkaProducer
import json

tracer = trace.get_tracer(__name__)
producer = KafkaProducer(
    bootstrap_servers=['kafka:9092'],
    value_serializer=lambda v: json.dumps(v).encode('utf-8')
)

def send_message(topic: str, data: dict):
    with tracer.start_as_current_span("send_kafka_message") as span:
        # Create headers dict for context
        headers = {}
        inject(headers)  # Inject trace context

        # Convert headers to Kafka format [(key, value), ...]
        kafka_headers = [(k, v.encode('utf-8')) for k, v in headers.items()]

        # Send with headers
        producer.send(
            topic,
            value=data,
            headers=kafka_headers  # Propagate context!
        )

        span.set_attribute("kafka.topic", topic)
```

#### Consumer (Receive Message with Context)

```python
from opentelemetry.propagate import extract
from opentelemetry import trace
from kafka import KafkaConsumer
import json

tracer = trace.get_tracer(__name__)
consumer = KafkaConsumer(
    'my-topic',
    bootstrap_servers=['kafka:9092'],
    value_deserializer=lambda m: json.loads(m.decode('utf-8'))
)

for message in consumer:
    # Extract context from Kafka message headers
    # Convert headers from [(key, bytes)] to {key: str}
    header_dict = {
        k: v.decode('utf-8')
        for k, v in message.headers
    }

    # Extract trace context
    context = extract(header_dict)

    # Create span with parent context
    with tracer.start_as_current_span(
        "process_kafka_message",
        context=context  # Link to producer's span!
    ) as span:
        data = message.value
        span.set_attribute("kafka.topic", message.topic)
        span.set_attribute("kafka.offset", message.offset)

        # Process data - all child spans inherit this context
        process_data(data)
```

---

### 3. **Database Queries**

Database queries don't cross service boundaries, but we still trace them:

```python
from opentelemetry.instrumentation.sqlalchemy import SQLAlchemyInstrumentor
from sqlalchemy import create_engine

engine = create_engine("postgresql://user:pass@host/db")

# Auto-instrument - queries become child spans automatically!
SQLAlchemyInstrumentor().instrument(engine=engine)

# Now all queries are traced
with engine.connect() as conn:
    # This query will appear as a child span in the current trace
    result = conn.execute("SELECT * FROM assets WHERE status = 'operational'")
```

---

### 4. **Redis Operations**

```python
from opentelemetry.instrumentation.redis import RedisInstrumentor
import redis

# Auto-instrument Redis
RedisInstrumentor().instrument()

# Now all Redis operations are traced
redis_client = redis.Redis(host='redis', port=6379)

# These operations will appear as child spans
redis_client.set('key', 'value')
value = redis_client.get('key')
```

---

## 🧪 Testing Context Propagation

### Test Script

```python
"""
Test context propagation across services
"""
from opentelemetry import trace
from opentelemetry.propagate import inject, extract
import requests

def test_propagation():
    # Service A: Create a span and make a request
    tracer = trace.get_tracer(__name__)

    with tracer.start_as_current_span("test_parent_span") as parent_span:
        trace_id = parent_span.get_span_context().trace_id
        print(f"Parent Trace ID: {trace_id:032x}")

        # Inject context
        headers = {}
        inject(headers)
        print(f"Propagated Headers: {headers}")

        # Make request
        response = requests.post(
            "http://ingestion-iiot/api/v1/ingest",
            json={"sensor_id": "TEST-001", "value": 42.0},
            headers=headers
        )

    # Check Jaeger UI - you should see a single trace with multiple spans!
    print(f"Check Jaeger: http://localhost:16686/trace/{trace_id:032x}")

if __name__ == "__main__":
    from tracing_example import setup_tracing
    setup_tracing("test-client")
    test_propagation()
```

### Verification Checklist

1. ✅ **Single Trace ID**: All spans have the same trace ID
2. ✅ **Parent-Child Links**: Spans are properly nested
3. ✅ **Timing**: Child spans are within parent span duration
4. ✅ **Attributes**: Context attributes (user_id, request_id) are present

---

## 🎯 Common Patterns in MANTIS

### Pattern 1: Synchronous Service Chain

```
User Request → Ingestion → Preprocessing → Feature Extraction → ML Service
               ↓           ↓              ↓                    ↓
               HTTP        HTTP           HTTP                 HTTP
               (inject)    (extract)      (inject)            (extract)
```

### Pattern 2: Asynchronous Kafka Pipeline

```
Ingestion → [Kafka Topic] → Preprocessing → [Kafka Topic] → ML Service
    ↓                            ↓                               ↓
  (inject headers)          (extract headers)              (extract headers)
```

### Pattern 3: Parallel Fan-Out

```
Orchestrator
    ├─→ Anomaly Detection (parallel)
    ├─→ RUL Prediction (parallel)
    └─→ Asset Service (parallel)
         ↓
    All with same parent trace ID!
```

---

## 🚨 Common Pitfalls

### ❌ Problem 1: Forgetting to Inject

```python
# BAD: No context propagation!
response = requests.post("http://other-service/api", json=data)
```

```python
# GOOD: Context propagated
headers = {}
inject(headers)
response = requests.post("http://other-service/api", json=data, headers=headers)
```

### ❌ Problem 2: Not Extracting from Kafka

```python
# BAD: New trace for each message!
for message in consumer:
    with tracer.start_as_current_span("process"):
        process(message.value)
```

```python
# GOOD: Continue existing trace
for message in consumer:
    context = extract({k: v.decode('utf-8') for k, v in message.headers})
    with tracer.start_as_current_span("process", context=context):
        process(message.value)
```

### ❌ Problem 3: Mixing Propagation Formats

```python
# BAD: Jaeger propagator in one service, W3C in another
# They won't understand each other!
```

```python
# GOOD: Use same propagator everywhere (W3C Trace Context is default)
from opentelemetry.propagate import set_global_textmap
from opentelemetry.propagators.composite import CompositeHTTPPropagator
from opentelemetry.propagators.b3 import B3MultiFormat

# Optional: Support multiple formats
set_global_textmap(CompositeHTTPPropagator([
    TraceContextTextMapPropagator(),  # W3C (primary)
    B3MultiFormat(),                  # B3 (compatibility)
]))
```

---

## 📊 Monitoring Propagation Health

### Metrics to Track

1. **Orphan Spans**: Spans without a parent (propagation failed)
2. **Trace Depth**: How many services does a trace cross?
3. **Broken Chains**: Gaps in the trace timeline

### Jaeger Queries

```
# Find traces crossing multiple services
service=ingestion-iiot AND service=preprocessing AND service=anomaly-detection

# Find long traces (potential performance issues)
minDuration=5s

# Find error traces
error=true
```

---

## ✅ Best Practices

1. **Always Inject**: When making any cross-service call (HTTP, Kafka, gRPC)
2. **Always Extract**: When receiving cross-service requests
3. **Use Auto-Instrumentation**: For HTTP, it's automatic with FastAPI instrumentor
4. **Manual for Kafka**: Kafka requires manual inject/extract
5. **Test Propagation**: Verify trace IDs match across services
6. **Use Span Attributes**: Add correlation IDs, user IDs, request IDs
7. **Set Span Names**: Use descriptive operation names (not URLs)

---

## 🎓 Learning Exercise

Try this exercise to understand propagation:

1. **Create Service A**: Send HTTP request to Service B
2. **Create Service B**: Log the incoming trace ID
3. **Send Request**: `curl -X POST http://service-a/api/test`
4. **Check Jaeger**: Find the trace
5. **Verify**: Both services appear in the same trace

Expected result: Single trace with 2 spans (one per service).

---

**Next**: See `PHASE5_JAEGER_COMPLETE.md` for complete Jaeger setup and usage guide.
