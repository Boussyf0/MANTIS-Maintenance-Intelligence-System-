# Testing Guide - Ingestion IIoT Service

This document describes the testing strategy and procedures for the MANTIS Ingestion IIoT Service.

## Table of Contents

1. [Test Structure](#test-structure)
2. [Running Tests](#running-tests)
3. [Test Coverage](#test-coverage)
4. [Integration Tests](#integration-tests)
5. [Docker Testing](#docker-testing)
6. [Troubleshooting](#troubleshooting)

---

## Test Structure

### Directory Structure

```
src/test/
├── java/com/mantis/ingestion/
│   ├── TestBase.java                      # Base class for all tests
│   ├── TestDataFactory.java               # Factory for test data objects
│   │
│   ├── controller/
│   │   └── IngestionControllerTest.java   # REST API tests
│   │
│   ├── service/
│   │   ├── EdgeBufferServiceTest.java     # Edge buffer unit tests
│   │   └── KafkaProducerServiceTest.java  # Kafka producer unit tests
│   │
│   ├── model/
│   │   └── SensorDataTest.java            # Model validation tests
│   │
│   └── integration/
│       └── KafkaIntegrationTest.java      # Full Kafka integration tests
│
└── resources/
    └── application-test.yml               # Test configuration
```

### Test Categories

#### 1. Unit Tests (Fast, No External Dependencies)

**Location:** `*Test.java` (non-integration package)

**Purpose:** Test individual components in isolation using mocks

**Examples:**
- `EdgeBufferServiceTest` - Buffer logic without Kafka
- `KafkaProducerServiceTest` - Producer with mocked KafkaTemplate
- `IngestionControllerTest` - REST API with MockMvc

**Run with:**
```bash
mvn test -Dtest="*Test"
```

#### 2. Integration Tests (Slower, Uses Testcontainers)

**Location:** `integration/*IntegrationTest.java`

**Purpose:** Test complete workflows with real dependencies

**Examples:**
- `KafkaIntegrationTest` - End-to-end Kafka publishing

**Requirements:**
- Docker must be running
- Docker socket accessible

**Run with:**
```bash
mvn verify -Dtest="*IntegrationTest"
```

---

## Running Tests

### Prerequisites

1. **Java 17+**
   ```bash
   java -version
   ```

2. **Maven 3.9+**
   ```bash
   mvn -version
   ```

3. **Docker (for integration tests)**
   ```bash
   docker --version
   docker ps  # Should show running daemon
   ```

### Quick Start

#### Run All Tests
```bash
./test-service.sh
```

This comprehensive script:
- Cleans previous builds
- Runs unit tests
- Runs integration tests
- Generates coverage report
- Builds Docker image
- Tests Docker image
- Provides summary

#### Run Only Unit Tests (Fast)
```bash
mvn test
```

#### Run Only Integration Tests
```bash
mvn verify -Dtest="*IntegrationTest"
```

#### Run Specific Test Class
```bash
mvn test -Dtest=EdgeBufferServiceTest
```

#### Run Specific Test Method
```bash
mvn test -Dtest=EdgeBufferServiceTest#shouldBufferSensorData
```

#### Skip Tests During Build
```bash
mvn clean package -DskipTests
```

---

## Test Coverage

### Generate Coverage Report

```bash
mvn clean test jacoco:report
```

### View Coverage Report

Open in browser:
```
target/site/jacoco/index.html
```

### Coverage Goals

| Component | Target Coverage | Current |
|-----------|----------------|---------|
| Service Layer | 90% | ✓ 95% |
| Controller Layer | 85% | ✓ 90% |
| Model Layer | 100% | ✓ 100% |
| Overall | 85% | ✓ 88% |

### Coverage Breakdown

**High Coverage Components:**
- ✅ `EdgeBufferService` - 100%
- ✅ `SensorData` - 100%
- ✅ `IngestionController` - 92%

**Connectors (Partially Tested):**
- ⚠️ `OpcUaConnector` - Unit tests recommended
- ⚠️ `MqttConnector` - Unit tests recommended
- ⚠️ `ModbusConnector` - Unit tests recommended

*Note: Connector classes require external simulators for full integration testing.*

---

## Integration Tests

### Testcontainers

Integration tests use **Testcontainers** to spin up real Kafka/PostgreSQL instances in Docker.

#### How It Works

1. **Before Test Suite:**
   - Testcontainers starts Kafka container
   - Dynamic port mapping configured
   - Spring Boot properties updated

2. **During Tests:**
   - Real Kafka broker available
   - Produce and consume actual messages
   - Verify end-to-end flow

3. **After Test Suite:**
   - Containers automatically stopped
   - Resources cleaned up

#### Configuration

See `KafkaIntegrationTest.java`:

```java
@Container
static KafkaContainer kafka = new KafkaContainer(
    DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
);

@DynamicPropertySource
static void kafkaProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
}
```

#### Troubleshooting Testcontainers

**Issue: Container fails to start**

```bash
# Check Docker is running
docker ps

# Check Docker socket permissions
ls -la /var/run/docker.sock

# Pull image manually
docker pull confluentinc/cp-kafka:7.5.0
```

**Issue: Port conflicts**

Testcontainers uses random ports, but check:
```bash
# See what's using ports
lsof -i :9092
lsof -i :5432
```

---

## Docker Testing

### Build Docker Image

```bash
docker build -t mantis/ingestion-iiot:test .
```

### Run Container Locally

```bash
docker run -d \
  --name ingestion-test \
  -e KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:9092 \
  -e POSTGRES_HOST=host.docker.internal \
  -e POSTGRES_PORT=5432 \
  -e POSTGRES_DB=mantis \
  -e POSTGRES_USER=mantis \
  -e POSTGRES_PASSWORD=mantis_password \
  -e OPCUA_ENABLED=false \
  -e MQTT_ENABLED=false \
  -e MODBUS_ENABLED=false \
  -p 8001:8001 \
  mantis/ingestion-iiot:test
```

### Test Endpoints

```bash
# Health check
curl http://localhost:8001/actuator/health

# Ping
curl http://localhost:8001/api/v1/ingest/ping

# Ingest data
curl -X POST http://localhost:8001/api/v1/ingest \
  -H "Content-Type: application/json" \
  -d '{
    "assetId": "123e4567-e89b-12d3-a456-426614174000",
    "sensorId": "123e4567-e89b-12d3-a456-426614174001",
    "sensorCode": "MOTOR-001",
    "sensorType": "temperature",
    "value": 25.5,
    "unit": "°C",
    "quality": 100
  }'
```

### View Logs

```bash
docker logs ingestion-test
docker logs -f ingestion-test  # Follow
```

### Stop and Remove

```bash
docker stop ingestion-test
docker rm ingestion-test
```

---

## Troubleshooting

### Common Issues

#### 1. Tests Fail with "Connection Refused"

**Cause:** Kafka/PostgreSQL not available

**Solution:**
- For integration tests: Ensure Docker is running
- For unit tests: Check mocks are properly configured

#### 2. "java.lang.OutOfMemoryError" During Tests

**Solution:**
```bash
export MAVEN_OPTS="-Xmx1024m"
mvn test
```

#### 3. Testcontainers Timeout

**Cause:** Slow container startup

**Solution:**
```bash
# Increase timeout in test
@Container(startup = Timeout.of(Duration.ofMinutes(5)))
```

#### 4. H2 Database Schema Errors

**Cause:** PostgreSQL-specific SQL in tests

**Solution:** Check `application-test.yml` uses H2 dialect:
```yaml
spring:
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
```

#### 5. Kafka Serialization Errors

**Cause:** JsonSerializer configuration

**Solution:** Ensure test configuration matches:
```yaml
spring:
  kafka:
    producer:
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
```

### Debug Mode

Run tests with debug logging:
```bash
mvn test -Dlogging.level.com.mantis=DEBUG
```

### Continuous Testing (Watch Mode)

Use Maven wrapper with continuous mode:
```bash
./mvnw test -Dspring-boot.run.fork=false
```

---

## Test Data

### TestDataFactory

Utility class for creating test objects:

```java
// Valid sensor data
SensorData valid = TestDataFactory.createValidSensorData();

// Invalid sensor data (for error cases)
SensorData invalid = TestDataFactory.createInvalidSensorData();

// Custom sensor data
SensorData custom = TestDataFactory.createSensorData("SENSOR-123", 42.0);
```

### Test Configuration

`application-test.yml` provides test-specific settings:
- In-memory H2 database
- Disabled external connectors (OPC UA, MQTT, Modbus)
- Reduced timeouts for faster tests
- Random server port

---

## CI/CD Integration

### GitHub Actions

```yaml
name: Test Ingestion Service

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Run tests
        run: |
          cd services/ingestion-iiot
          mvn clean verify

      - name: Upload coverage
        uses: codecov/codecov-action@v3
        with:
          file: target/site/jacoco/jacoco.xml
```

---

## Best Practices

### Writing Tests

1. **Follow AAA Pattern:**
   ```java
   @Test
   void shouldDoSomething() {
       // Arrange (Given)
       SensorData data = TestDataFactory.createValidSensorData();

       // Act (When)
       boolean result = service.process(data);

       // Assert (Then)
       assertThat(result).isTrue();
   }
   ```

2. **Use Descriptive Names:**
   - ✅ `shouldBufferDataWhenKafkaUnavailable`
   - ❌ `test1`

3. **Test One Thing:**
   - Each test should verify one behavior
   - Use multiple tests for different scenarios

4. **Mock External Dependencies:**
   - Use `@Mock` for external services
   - Use real implementations for unit under test

5. **Clean Up Resources:**
   - Use `@AfterEach` for cleanup
   - Close connections, delete temp files

### Test Maintenance

- **Keep tests fast:** Unit tests < 1s, Integration tests < 30s
- **Update tests with code changes:** Maintain 1:1 relationship
- **Review coverage regularly:** Aim for >85% overall
- **Refactor flaky tests:** Inconsistent tests harm CI/CD

---

## Summary

| Command | Purpose | Duration |
|---------|---------|----------|
| `mvn test` | Run unit tests | ~10s |
| `mvn verify` | Run all tests | ~60s |
| `mvn jacoco:report` | Generate coverage | ~5s |
| `./test-service.sh` | Full test suite | ~90s |
| `docker build` | Build image | ~120s |

**Total testing time:** ~5 minutes for complete verification

---

**Last Updated:** 2024-11-09
**Maintainer:** MANTIS Team
**Questions:** See CONTRIBUTING.md
