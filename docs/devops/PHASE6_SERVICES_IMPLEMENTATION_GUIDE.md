# 🚀 Phase 6: Service Implementation Guide - Java Spring Boot

**Date**: 2025-10-23
**Status**: IMPLEMENTATION GUIDE
**Tutor**: Claude Code

---

## 📚 Overview

This guide will help you implement all 6 MANTIS microservices using **Java 17** and **Spring Boot 3.2**, with full observability (Prometheus, Grafana, Jaeger) built-in from day 1.

### Services to Implement

1. **Ingestion IIoT** - Collect sensor data from industrial protocols (OPC UA, MQTT, Modbus)
2. **Preprocessing** - Clean, align, and prepare data
3. **Feature Extraction** - Extract time/frequency domain features
4. **Anomaly Detection** - Detect anomalies using ML models
5. **RUL Prediction** - Predict Remaining Useful Life
6. **Maintenance Orchestrator** - Generate and manage work orders

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                      MANTIS Microservices                        │
└─────────────────────────────────────────────────────────────────┘

┌──────────────┐   Kafka    ┌──────────────┐   Kafka    ┌──────────────┐
│  Ingestion   │──────────>│Preprocessing │──────────>│Feature Extr. │
│    IIoT      │sensor.raw │              │preprocessed│              │
│              │            │              │            │              │
│ • OPC UA     │            │ • Cleaning   │            │ • FFT        │
│ • MQTT       │            │ • Resampling │            │ • Wavelets   │
│ • Modbus     │            │ • Windowing  │            │ • RMS/Kurt   │
└──────────────┘            └──────────────┘            └──────────────┘
                                                                 │
                                                                 │ Kafka
                                                                 │ features
                                                                 ▼
┌──────────────┐            ┌──────────────┐            ┌──────────────┐
│ Maintenance  │◀──Kafka───│RUL Prediction│◀──Kafka───│   Anomaly    │
│ Orchestrator │rul.pred   │              │anomalies  │  Detection   │
│              │            │              │            │              │
│ • Work Orders│            │ • LSTM/GRU   │            │ • Isolation  │
│ • Rules Eng. │            │ • Transfer L.│            │   Forest     │
│ • Inventory  │            │ • Uncertain. │            │ • AutoEncoder│
└──────────────┘            └──────────────┘            └──────────────┘
       │
       │ REST API
       ▼
┌──────────────┐
│  Dashboard   │
│   (React)    │
└──────────────┘

All services expose:
├─ HTTP REST API (Spring MVC)
├─ Metrics endpoint: /actuator/prometheus
├─ Health endpoint: /actuator/health
└─ Tracing (Micrometer → Jaeger)
```

---

## 📦 Common Dependencies (All Services)

### Maven Parent POM Structure

Create a parent POM at `services/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.1</version>
        <relativePath/>
    </parent>

    <groupId>com.mantis</groupId>
    <artifactId>mantis-parent</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>

    <modules>
        <module>shared-common</module>
        <module>ingestion-iiot</module>
        <module>preprocessing</module>
        <module>feature-extraction</module>
        <module>anomaly-detection</module>
        <module>rul-prediction</module>
        <module>maintenance-orchestrator</module>
    </modules>

    <properties>
        <java.version>17</java.version>
        <spring-cloud.version>2023.0.0</spring-cloud.version>
        <kafka.version>3.6.0</kafka.version>
        <micrometer.version>1.12.0</micrometer.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Spring Cloud -->
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <!-- Shared Common Library -->
            <dependency>
                <groupId>com.mantis</groupId>
                <artifactId>shared-common</artifactId>
                <version>1.0.0</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

### Common Dependencies for All Services

```xml
<!-- Spring Boot Core -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Actuator (Metrics & Health Checks) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Prometheus Metrics -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>

<!-- OpenTelemetry Tracing -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-jaeger</artifactId>
</dependency>

<!-- Kafka -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>

<!-- PostgreSQL -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>

<!-- Redis (Caching & Feast) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Resilience4j (Circuit Breaker, Retry, Rate Limiter) -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.1.0</version>
</dependency>

<!-- Lombok -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>

<!-- Jackson -->
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
</dependency>

<!-- Tests -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <scope>test</scope>
</dependency>
```

---

## 🔧 Shared Common Library (`shared-common`)

Create a shared module with common utilities, configurations, and base classes.

### Directory Structure

```
services/shared-common/
├── pom.xml
└── src/main/java/com/mantis/common/
    ├── config/
    │   ├── KafkaConfig.java
    │   ├── ObservabilityConfig.java
    │   └── DatabaseConfig.java
    ├── model/
    │   ├── SensorData.java
    │   ├── ProcessedData.java
    │   ├── Feature.java
    │   ├── Anomaly.java
    │   └── RULPrediction.java
    ├── exception/
    │   ├── MantisException.java
    │   └── GlobalExceptionHandler.java
    ├── service/
    │   └── BaseKafkaService.java
    └── util/
        ├── MetricsUtil.java
        ├── TracingUtil.java
        └── ValidationUtil.java
```

### Key Classes

#### 1. SensorData.java (Common Data Model)

```java
package com.mantis.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorData {
    private String sensorId;
    private String sensorCode;
    private String assetId;
    private SensorType type;
    private Instant timestamp;
    private Double value;
    private String unit;
    private DataQuality quality;
    private Map<String, Object> metadata;

    public enum SensorType {
        VIBRATION, TEMPERATURE, PRESSURE,
        CURRENT, VOLTAGE, FLOW, SPEED
    }

    public enum DataQuality {
        GOOD, UNCERTAIN, BAD
    }
}
```

#### 2. ObservabilityConfig.java

```java
package com.mantis.common.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ObservabilityConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${mantis.environment:development}")
    private String environment;

    /**
     * Enable @Timed annotation on methods
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    /**
     * Add common tags to all metrics
     */
    @Bean
    public MeterRegistry meterRegistry(MeterRegistry registry) {
        registry.config().commonTags(
            Tags.of(
                Tag.of("application", applicationName),
                Tag.of("environment", environment),
                Tag.of("namespace", "mantis")
            )
        );
        return registry;
    }
}
```

#### 3. BaseKafkaService.java

```java
package com.mantis.common.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

@Slf4j
public abstract class BaseKafkaService<T> {

    protected final KafkaTemplate<String, T> kafkaTemplate;
    protected final MeterRegistry meterRegistry;
    protected final ObservationRegistry observationRegistry;

    protected final Counter messagesPublished;
    protected final Counter publishErrors;
    protected final Timer publishDuration;

    public BaseKafkaService(
            KafkaTemplate<String, T> kafkaTemplate,
            MeterRegistry meterRegistry,
            ObservationRegistry observationRegistry,
            String serviceName
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.meterRegistry = meterRegistry;
        this.observationRegistry = observationRegistry;

        // Initialize metrics
        this.messagesPublished = Counter.builder("kafka.messages.published")
                .tag("service", serviceName)
                .description("Total messages published to Kafka")
                .register(meterRegistry);

        this.publishErrors = Counter.builder("kafka.publish.errors")
                .tag("service", serviceName)
                .description("Total Kafka publish errors")
                .register(meterRegistry);

        this.publishDuration = Timer.builder("kafka.publish.duration")
                .tag("service", serviceName)
                .description("Kafka publish duration")
                .register(meterRegistry);
    }

    /**
     * Publish message to Kafka with observability
     */
    protected CompletableFuture<SendResult<String, T>> publishWithObservability(
            String topic,
            String key,
            T message
    ) {
        return Observation
                .createNotStarted("kafka.publish", observationRegistry)
                .contextualName("publish to " + topic)
                .lowCardinalityKeyValue("topic", topic)
                .observe(() -> {
                    Timer.Sample sample = Timer.start(meterRegistry);

                    return kafkaTemplate.send(topic, key, message)
                            .whenComplete((result, ex) -> {
                                sample.stop(publishDuration);

                                if (ex != null) {
                                    log.error("Failed to publish to topic: {}", topic, ex);
                                    publishErrors.increment();
                                } else {
                                    log.debug("Published to topic: {} partition: {} offset: {}",
                                            topic,
                                            result.getRecordMetadata().partition(),
                                            result.getRecordMetadata().offset()
                                    );
                                    messagesPublished.increment();
                                }
                            });
                });
    }
}
```

---

## 🎯 Service-Specific Implementation Guides

### Service 1: Ingestion IIoT (STARTED ✅)

**Status**: Foundation exists, needs completion

**Purpose**: Collect sensor data from industrial protocols

**Key Components to Complete**:

1. **REST Controller** (`IngestionController.java`):
```java
@RestController
@RequestMapping("/api/v1/ingest")
@Slf4j
public class IngestionController {

    @PostMapping
    @Timed(value = "ingestion.http.requests", description = "HTTP ingestion requests")
    public ResponseEntity<IngestionResponse> ingestData(
            @Valid @RequestBody SensorDataRequest request
    ) {
        // Validation, processing, Kafka publishing
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<HealthStatus> health() {
        return ResponseEntity.ok(healthStatus);
    }
}
```

2. **OPC UA Connector** (`OpcUaConnectorService.java`):
```java
@Service
@Slf4j
public class OpcUaConnectorService {

    @Value("${mantis.opcua.endpoint}")
    private String opcUaEndpoint;

    private OpcUaClient client;

    @PostConstruct
    public void connect() {
        // Initialize Eclipse Milo OPC UA client
        // Subscribe to nodes
        // Handle data changes
    }

    @CircuitBreaker(name = "opcua", fallbackMethod = "opcuaFallback")
    @Retry(name = "opcua")
    public void subscribeToNode(String nodeId) {
        // Subscribe logic with resilience
    }
}
```

3. **MQTT Connector** (`MqttConnectorService.java`):
```java
@Service
@Slf4j
public class MqttConnectorService implements MqttCallback {

    @Value("${mantis.mqtt.broker}")
    private String brokerUrl;

    private MqttClient mqttClient;

    @PostConstruct
    public void connect() throws MqttException {
        mqttClient = new MqttClient(brokerUrl, MqttClient.generateClientId());
        mqttClient.setCallback(this);
        mqttClient.connect();
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        // Parse message, create SensorData, publish to Kafka
    }
}
```

4. **Modbus TCP Connector** (`ModbusConnectorService.java`):
```java
@Service
@Slf4j
public class ModbusConnectorService {

    @Scheduled(fixedDelayString = "${mantis.modbus.poll-interval:1000}")
    public void pollModbusRegisters() {
        // Poll Modbus TCP registers
        // Convert to SensorData
        // Publish to Kafka
    }
}
```

**Kafka Topic**: `sensor.raw`

**Metrics to Expose**:
- `ingestion_data_points_total` - Counter
- `ingestion_protocol_errors` - Counter by protocol
- `ingestion_buffer_size` - Gauge

---

### Service 2: Preprocessing

**Purpose**: Clean, resample, and prepare data for feature extraction

**Key Components**:

1. **Kafka Consumer** (`PreprocessingConsumer.java`):
```java
@Service
@Slf4j
public class PreprocessingConsumer {

    @KafkaListener(topics = "sensor.raw", groupId = "preprocessing-group")
    @Timed("preprocessing.kafka.consume")
    public void consumeSensorData(
            @Payload SensorData data,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition
    ) {
        // Process with observability
        Observation.createNotStarted("preprocessing.process", observationRegistry)
                .observe(() -> processSensorData(data));
    }
}
```

2. **Data Cleaning Service** (`DataCleaningService.java`):
```java
@Service
public class DataCleaningService {

    @Timed("preprocessing.cleaning")
    public ProcessedData clean(SensorData raw) {
        // Remove outliers (Z-score, IQR)
        // Handle missing values (interpolation)
        // Denoise (moving average, Savitzky-Golay)
        return processedData;
    }
}
```

3. **Resampling Service** (`ResamplingService.java`):
```java
@Service
public class ResamplingService {

    public ProcessedData resample(List<SensorData> timeSeries, Duration targetInterval) {
        // Resample to fixed intervals
        // Aggregate (mean, max, min)
        return resampled;
    }
}
```

4. **Windowing Service** (`WindowingService.java`):
```java
@Service
public class WindowingService {

    public List<Window> createWindows(List<SensorData> data, Duration windowSize, Duration overlap) {
        // Create sliding windows
        // Each window = input for feature extraction
        return windows;
    }
}
```

**Kafka Topics**:
- Consumes: `sensor.raw`
- Produces: `sensor.preprocessed`

**Metrics**:
- `preprocessing_outliers_removed` - Counter
- `preprocessing_windows_created` - Counter
- `preprocessing_duration_seconds` - Histogram

---

### Service 3: Feature Extraction

**Purpose**: Extract time-domain and frequency-domain features

**Key Components**:

1. **Time Domain Features** (`TimeDomainService.java`):
```java
@Service
public class TimeDomainService {

    public TimeFeatures extract(Window window) {
        return TimeFeatures.builder()
                .rms(calculateRMS(window.getData()))
                .kurtosis(calculateKurtosis(window.getData()))
                .skewness(calculateSkewness(window.getData()))
                .peakToPeak(calculatePeakToPeak(window.getData()))
                .crestFactor(calculateCrestFactor(window.getData()))
                .build();
    }

    private double calculateRMS(double[] data) {
        return Math.sqrt(Arrays.stream(data)
                .map(x -> x * x)
                .average()
                .orElse(0.0));
    }
}
```

2. **Frequency Domain Features** (`FrequencyDomainService.java`):
```java
@Service
public class FrequencyDomainService {

    public FrequencyFeatures extract(Window window) {
        // FFT using JTransforms or Apache Commons Math
        double[] fft = performFFT(window.getData());

        return FrequencyFeatures.builder()
                .dominantFrequency(findDominantFrequency(fft))
                .spectralCentroid(calculateSpectralCentroid(fft))
                .spectralEntropy(calculateSpectralEntropy(fft))
                .bandPower(calculateBandPower(fft))
                .build();
    }
}
```

3. **Wavelet Features** (`WaveletService.java`):
```java
@Service
public class WaveletService {

    public WaveletFeatures extract(Window window) {
        // Continuous Wavelet Transform (CWT)
        // Discrete Wavelet Transform (DWT)
        // Energy distribution across scales
        return waveletFeatures;
    }
}
```

4. **Feature Store Integration** (`FeastService.java`):
```java
@Service
public class FeastService {

    @Value("${feast.online.host}")
    private String feastHost;

    private OnlineServingServiceBlockingStub feastClient;

    public void writeFeatures(String entityId, Map<String, Feature> features) {
        // Write features to Feast (Redis-backed)
        // For online serving to ML models
    }
}
```

**Kafka Topics**:
- Consumes: `sensor.preprocessed`
- Produces: `features.computed`

**Metrics**:
- `features_extracted_total` - Counter by feature type
- `feature_extraction_duration` - Histogram

---

### Service 4: Anomaly Detection

**Purpose**: Detect anomalies using ML models

**Key Components**:

1. **Model Service** (`AnomalyModelService.java`):
```java
@Service
public class AnomalyModelService {

    @Value("${mantis.model.path}")
    private String modelPath;

    private IsolationForest isolationForest;
    private AutoEncoder autoEncoder; // PyTorch via DJL

    @PostConstruct
    public void loadModels() {
        // Load pre-trained models
        // Can use Deep Java Library (DJL) for PyTorch models
    }

    @CircuitBreaker(name = "ml-inference")
    @Timed("anomaly.inference")
    public AnomalyPrediction predict(Map<String, Double> features) {
        // Model inference
        // Return anomaly score + classification
        double score = isolationForest.anomalyScore(features);
        boolean isAnomaly = score > threshold;

        return AnomalyPrediction.builder()
                .score(score)
                .isAnomaly(isAnomaly)
                .confidence(calculateConfidence(score))
                .build();
    }
}
```

2. **Threshold Management** (`ThresholdService.java`):
```java
@Service
public class ThresholdService {

    @Scheduled(fixedRate = 3600000) // Every hour
    public void updateAdaptiveThresholds() {
        // Calculate percentile-based thresholds
        // Account for concept drift
        // Store in database/cache
    }
}
```

**Kafka Topics**:
- Consumes: `features.computed`
- Produces: `anomalies.detected`

**Metrics**:
- `anomalies_detected_total` - Counter
- `anomaly_score_distribution` - Histogram
- `model_inference_duration` - Histogram

---

### Service 5: RUL Prediction

**Purpose**: Predict Remaining Useful Life

**Key Components**:

1. **LSTM Model Service** (`RULModelService.java`):
```java
@Service
public class RULModelService {

    private Predictor<float[], float[]> lstmPredictor;

    @PostConstruct
    public void loadModel() {
        // Load LSTM model using DJL
        // Model trained on C-MAPSS dataset (transfer learning)
    }

    @Timed("rul.prediction")
    public RULPrediction predictRUL(List<Map<String, Double>> sequenceFeatures) {
        // LSTM expects sequence of features (time steps)
        float[] input = prepareSequence(sequenceFeatures);
        float[] output = lstmPredictor.predict(input);

        int rulDays = (int) output[0];
        double confidence = output[1];

        return RULPrediction.builder()
                .remainingDays(rulDays)
                .confidence(confidence)
                .predictionDate(Instant.now())
                .build();
    }
}
```

2. **Uncertainty Quantification** (`UncertaintyService.java`):
```java
@Service
public class UncertaintyService {

    public UncertaintyEstimate calculateUncertainty(List<Float> predictions) {
        // Bayesian approach or MC Dropout
        // Calculate confidence intervals
        double mean = calculateMean(predictions);
        double std = calculateStd(predictions);

        return UncertaintyEstimate.builder()
                .mean(mean)
                .stdDev(std)
                .confidenceInterval95(new double[]{mean - 1.96 * std, mean + 1.96 * std})
                .build();
    }
}
```

3. **MLflow Integration** (`MLflowService.java`):
```java
@Service
public class MLflowService {

    @Value("${mlflow.tracking.uri}")
    private String mlflowUri;

    private MlflowClient mlflowClient;

    public void logPrediction(RULPrediction prediction, Map<String, Object> context) {
        // Log predictions to MLflow for monitoring
        // Track model performance over time
    }

    public ModelVersion getLatestModel(String modelName) {
        // Fetch latest production model from MLflow registry
    }
}
```

**Kafka Topics**:
- Consumes: `features.computed`, `anomalies.detected`
- Produces: `rul.predictions`

**Metrics**:
- `rul_predictions_total` - Counter
- `rul_mae` - Gauge (Mean Absolute Error)
- `rul_prediction_distribution` - Histogram

---

### Service 6: Maintenance Orchestrator

**Purpose**: Generate and manage work orders based on predictions

**Key Components**:

1. **Rules Engine** (`BusinessRulesService.java`):
```java
@Service
public class BusinessRulesService {

    public WorkOrder evaluateRules(AnomalyEvent anomaly, RULPrediction rul) {
        // Drools rules engine or simple if-else
        Priority priority = determinePriority(anomaly, rul);
        MaintenanceType type = determineMaintenanceType(anomaly);

        return WorkOrder.builder()
                .assetId(anomaly.getAssetId())
                .priority(priority)
                .type(type)
                .estimatedRUL(rul.getRemainingDays())
                .scheduledDate(calculateOptimalDate(rul))
                .build();
    }

    private Priority determinePriority(AnomalyEvent anomaly, RULPrediction rul) {
        if (rul.getRemainingDays() < 7) return Priority.CRITICAL;
        if (rul.getRemainingDays() < 30) return Priority.HIGH;
        if (anomaly.getScore() > 0.8) return Priority.HIGH;
        return Priority.MEDIUM;
    }
}
```

2. **Optimization Service** (`MaintenanceOptimizationService.java`):
```java
@Service
public class MaintenanceOptimizationService {

    public OptimizedSchedule optimizeSchedule(List<WorkOrder> workOrders) {
        // Use OR-Tools for optimization
        // Constraints: technician availability, spare parts, downtime windows
        // Objective: minimize cost, maximize reliability
        return optimizedSchedule;
    }
}
```

3. **Inventory Service** (`SparePartsService.java`):
```java
@Service
public class SparePartsService {

    @Autowired
    private SparePartsRepository repository;

    public boolean checkAvailability(String partCode, int quantity) {
        Optional<SparePart> part = repository.findByCode(partCode);
        return part.isPresent() && part.get().getStockLevel() >= quantity;
    }

    public void reserveParts(WorkOrder workOrder) {
        // Reserve parts for work order
        // Trigger reorder if below threshold
    }
}
```

4. **REST API** (`MaintenanceController.java`):
```java
@RestController
@RequestMapping("/api/v1/maintenance")
public class MaintenanceController {

    @GetMapping("/work-orders")
    public ResponseEntity<Page<WorkOrder>> getWorkOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Priority priority
    ) {
        return ResponseEntity.ok(workOrderService.findAll(page, size, priority));
    }

    @PostMapping("/work-orders")
    public ResponseEntity<WorkOrder> createWorkOrder(@Valid @RequestBody WorkOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workOrderService.create(request));
    }
}
```

**Kafka Topics**:
- Consumes: `anomalies.detected`, `rul.predictions`
- Produces: `maintenance.actions`

**Metrics**:
- `work_orders_created_total` - Counter by priority
- `work_orders_overdue` - Gauge
- `mtbf_days` - Gauge (Mean Time Between Failures)
- `mttr_hours` - Gauge (Mean Time To Repair)

---

## 🔐 Security Best Practices

### 1. API Security

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable() // For service-to-service communication
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()
                .requestMatchers("/api/v1/**").authenticated()
                .anyRequest().denyAll()
            )
            .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt);

        return http.build();
    }
}
```

### 2. Secrets Management

```yaml
# application.yml
spring:
  config:
    import: optional:secrets.yml  # External secrets file

# secrets.yml (not in git!)
mantis:
  database:
    password: ${DB_PASSWORD}  # From environment variable
  kafka:
    sasl:
      username: ${KAFKA_USERNAME}
      password: ${KAFKA_PASSWORD}
```

### 3. Input Validation

```java
@Data
public class SensorDataRequest {

    @NotNull(message = "Sensor ID is required")
    @Pattern(regexp = "^[A-Z]{3}-\\d{3}$", message = "Invalid sensor ID format")
    private String sensorId;

    @NotNull
    @PastOrPresent(message = "Timestamp cannot be in the future")
    private Instant timestamp;

    @NotNull
    @DecimalMin(value = "-273.15", message = "Temperature below absolute zero")
    private Double value;
}
```

---

## 📊 Observability Integration

### Application Configuration (application.yml)

```yaml
spring:
  application:
    name: ingestion-iiot  # Change per service

management:
  endpoints:
    web:
      exposure:
        include: health,prometheus,info,metrics
      base-path: /actuator
  endpoint:
    health:
      show-details: always
    prometheus:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
      environment: ${mantis.environment:development}
  tracing:
    sampling:
      probability: 1.0  # 100% for development, adjust for production
  otlp:
    tracing:
      endpoint: http://jaeger:4318/v1/traces

logging:
  level:
    com.mantis: DEBUG
    org.springframework.kafka: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg trace_id=%X{traceId} span_id=%X{spanId}%n"
```

### Custom Metrics Example

```java
@Component
public class CustomMetrics {

    private final Counter dataPointsProcessed;
    private final Gauge bufferSize;
    private final Timer processingDuration;

    public CustomMetrics(MeterRegistry registry) {
        this.dataPointsProcessed = Counter.builder("mantis.data.processed")
                .description("Total data points processed")
                .tag("type", "sensor")
                .register(registry);

        this.bufferSize = Gauge.builder("mantis.buffer.size", this::getBufferSize)
                .description("Current buffer size")
                .register(registry);

        this.processingDuration = Timer.builder("mantis.processing.duration")
                .description("Data processing duration")
                .register(registry);
    }

    private int getBufferSize() {
        // Return actual buffer size
        return buffer.size();
    }
}
```

---

## 🐳 Docker Configuration

### Dockerfile Template

```dockerfile
FROM eclipse-temurin:17-jre-alpine

# Create app user
RUN addgroup -S mantis && adduser -S mantis -G mantis

# Set working directory
WORKDIR /app

# Copy jar
COPY target/*.jar app.jar

# Change ownership
RUN chown -R mantis:mantis /app

# Switch to app user
USER mantis

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
```

### docker-compose.services.yml

```yaml
version: '3.8'

services:
  ingestion-iiot:
    build: ./services/ingestion-iiot
    container_name: mantis-ingestion-iiot
    ports:
      - "8080:8080"
      - "9091:9091"  # Metrics
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/mantis
      SPRING_DATASOURCE_USERNAME: mantis
      SPRING_DATASOURCE_PASSWORD: mantis_password
      MANTIS_OPCUA_ENDPOINT: opc.tcp://plc:4840
      MANTIS_MQTT_BROKER: tcp://mqtt-broker:1883
    depends_on:
      - kafka
      - postgres
    networks:
      - mantis-network
    restart: unless-stopped

  preprocessing:
    build: ./services/preprocessing
    container_name: mantis-preprocessing
    ports:
      - "8081:8080"
      - "9092:9091"
    environment:
      # Similar to above
    depends_on:
      - kafka
    networks:
      - mantis-network

  # ... other services

networks:
  mantis-network:
    external: true
```

---

## ✅ Implementation Checklist (Per Service)

### Planning Phase
- [ ] Review service responsibilities
- [ ] Identify input/output Kafka topics
- [ ] Define data models
- [ ] List required external dependencies (OPC UA, MQTT, etc.)

### Development Phase
- [ ] Create Maven module with pom.xml
- [ ] Implement domain models (entities, DTOs)
- [ ] Implement business logic services
- [ ] Implement Kafka consumers/producers
- [ ] Add REST controllers (if needed)
- [ ] Add custom metrics (@Timed, Counter, Gauge)
- [ ] Add circuit breakers and retry logic
- [ ] Implement health checks

### Testing Phase
- [ ] Unit tests (JUnit 5, Mockito)
- [ ] Integration tests (Testcontainers for Kafka, PostgreSQL)
- [ ] Load tests (JMeter or Gatling)
- [ ] Test metrics endpoint: `curl http://localhost:8080/actuator/prometheus`
- [ ] Test health endpoint: `curl http://localhost:8080/actuator/health`

### Observability Phase
- [ ] Verify metrics appear in Prometheus
- [ ] Create Grafana dashboard for service
- [ ] Verify traces appear in Jaeger
- [ ] Test context propagation across services
- [ ] Set up alerts in Prometheus

### Deployment Phase
- [ ] Create Dockerfile
- [ ] Add service to docker-compose.services.yml
- [ ] Test Docker build: `mvn clean package && docker build .`
- [ ] Test Docker run locally
- [ ] Document environment variables
- [ ] Deploy to development environment

---

## 🎓 Key Learning Points

### 1. Microservices Design Patterns

**Used in MANTIS**:
- **API Gateway**: (To be added) Single entry point
- **Service Discovery**: Spring Cloud (optional for Docker Compose)
- **Circuit Breaker**: Resilience4j (prevent cascading failures)
- **Saga Pattern**: For distributed transactions (work order creation)
- **Event Sourcing**: Kafka as event log
- **CQRS**: Read/write separation for analytics

### 2. Java Best Practices

- Use **Lombok** to reduce boilerplate
- Use **Optional** for null safety
- Use **CompletableFuture** for async operations
- Use **Stream API** for collections
- Follow **SOLID** principles
- Use **Builder pattern** for complex objects

### 3. Testing Strategy

**Test Pyramid**:
```
        /\
       /  \      E2E Tests (few)
      /────\
     /      \    Integration Tests (some)
    /────────\
   /          \  Unit Tests (many)
  /────────────\
```

**Example**:
- 70% Unit tests (business logic)
- 20% Integration tests (Kafka, database)
- 10% E2E tests (full service chain)

---

## 🚀 Next Steps

1. **Complete Ingestion Service**:
   - Finish OPC UA, MQTT, Modbus connectors
   - Add REST endpoint for manual ingestion
   - Test with simulated sensor data

2. **Implement Preprocessing Service**:
   - Start with Kafka consumer
   - Implement cleaning algorithms
   - Add windowing logic

3. **Continue with other services** following the patterns above

4. **Integration Testing**:
   - Send data through entire pipeline
   - Verify Kafka topic flow
   - Check Grafana dashboards
   - Trace requests in Jaeger

5. **Performance Tuning**:
   - Optimize Kafka consumer configs
   - Add caching where appropriate
   - Tune JVM parameters

---

**Documentation**: See `PHASE6_SERVICES_COMPLETE.md` (to be created after implementation)

**Questions?** Review architecture diagram, check existing code, or consult Spring Boot / Kafka documentation.

Good luck building MANTIS! 🏭🤖
