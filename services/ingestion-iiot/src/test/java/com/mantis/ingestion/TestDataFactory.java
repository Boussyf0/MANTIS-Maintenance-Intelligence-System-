package com.mantis.ingestion;

import com.mantis.ingestion.dto.SensorDataRequest;
import com.mantis.ingestion.model.SensorData;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Factory class for creating test data objects.
 */
public class TestDataFactory {

    public static SensorData createValidSensorData() {
        return SensorData.builder()
                .timestamp(Instant.now())
                .assetId(UUID.randomUUID())
                .sensorId(UUID.randomUUID())
                .sensorCode("TEST-001")
                .sensorType("temperature")
                .value(25.5)
                .unit("°C")
                .quality(100)
                .source("test")
                .metadata(Map.of("test", true))
                .build();
    }

    public static SensorData createInvalidSensorData() {
        return SensorData.builder()
                .timestamp(null)  // Invalid: null timestamp
                .assetId(UUID.randomUUID())
                .sensorId(UUID.randomUUID())
                .sensorCode("TEST-001")
                .sensorType("temperature")
                .value(25.5)
                .unit("°C")
                .quality(100)
                .source("test")
                .build();
    }

    public static SensorData createSensorData(String sensorCode, Double value) {
        return SensorData.builder()
                .timestamp(Instant.now())
                .assetId(UUID.randomUUID())
                .sensorId(UUID.randomUUID())
                .sensorCode(sensorCode)
                .sensorType("temperature")
                .value(value)
                .unit("°C")
                .quality(100)
                .source("test")
                .build();
    }

    public static SensorDataRequest createValidSensorDataRequest() {
        return SensorDataRequest.builder()
                .assetId(UUID.randomUUID())
                .sensorId(UUID.randomUUID())
                .sensorCode("TEST-001")
                .sensorType("temperature")
                .value(25.5)
                .unit("°C")
                .quality(100)
                .timestamp(Instant.now())
                .build();
    }

    public static SensorDataRequest createInvalidSensorDataRequest() {
        return SensorDataRequest.builder()
                .assetId(null)  // Invalid: null assetId
                .sensorId(UUID.randomUUID())
                .sensorCode("TEST-001")
                .sensorType("temperature")
                .value(25.5)
                .unit("°C")
                .quality(100)
                .build();
    }
}
