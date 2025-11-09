package com.mantis.ingestion.model;

import com.mantis.ingestion.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SensorData model.
 */
@DisplayName("SensorData Tests")
class SensorDataTest {

    @Test
    @DisplayName("Should validate correct sensor data")
    void shouldValidateCorrectSensorData() {
        // Given
        SensorData sensorData = TestDataFactory.createValidSensorData();

        // When
        boolean isValid = sensorData.isValid();

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should invalidate sensor data with null timestamp")
    void shouldInvalidateSensorDataWithNullTimestamp() {
        // Given
        SensorData sensorData = SensorData.builder()
                .timestamp(null)
                .assetId(UUID.randomUUID())
                .sensorId(UUID.randomUUID())
                .sensorCode("TEST-001")
                .sensorType("temperature")
                .value(25.5)
                .quality(100)
                .build();

        // When
        boolean isValid = sensorData.isValid();

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should invalidate sensor data with NaN value")
    void shouldInvalidateSensorDataWithNaNValue() {
        // Given
        SensorData sensorData = SensorData.builder()
                .timestamp(Instant.now())
                .assetId(UUID.randomUUID())
                .sensorId(UUID.randomUUID())
                .sensorCode("TEST-001")
                .sensorType("temperature")
                .value(Double.NaN)
                .quality(100)
                .build();

        // When
        boolean isValid = sensorData.isValid();

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should invalidate sensor data with infinite value")
    void shouldInvalidateSensorDataWithInfiniteValue() {
        // Given
        SensorData sensorData = SensorData.builder()
                .timestamp(Instant.now())
                .assetId(UUID.randomUUID())
                .sensorId(UUID.randomUUID())
                .sensorCode("TEST-001")
                .sensorType("temperature")
                .value(Double.POSITIVE_INFINITY)
                .quality(100)
                .build();

        // When
        boolean isValid = sensorData.isValid();

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should invalidate sensor data with invalid quality")
    void shouldInvalidateSensorDataWithInvalidQuality() {
        // Given
        SensorData sensorData = SensorData.builder()
                .timestamp(Instant.now())
                .assetId(UUID.randomUUID())
                .sensorId(UUID.randomUUID())
                .sensorCode("TEST-001")
                .sensorType("temperature")
                .value(25.5)
                .quality(150)  // Invalid: > 100
                .build();

        // When
        boolean isValid = sensorData.isValid();

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should invalidate sensor data with blank sensor code")
    void shouldInvalidateSensorDataWithBlankSensorCode() {
        // Given
        SensorData sensorData = SensorData.builder()
                .timestamp(Instant.now())
                .assetId(UUID.randomUUID())
                .sensorId(UUID.randomUUID())
                .sensorCode("")
                .sensorType("temperature")
                .value(25.5)
                .quality(100)
                .build();

        // When
        boolean isValid = sensorData.isValid();

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should calculate latency correctly")
    void shouldCalculateLatencyCorrectly() {
        // Given
        Instant timestamp = Instant.now().minusMillis(500);
        Instant createdAt = Instant.now();

        SensorData sensorData = SensorData.builder()
                .timestamp(timestamp)
                .assetId(UUID.randomUUID())
                .sensorId(UUID.randomUUID())
                .sensorCode("TEST-001")
                .sensorType("temperature")
                .value(25.5)
                .quality(100)
                .createdAt(createdAt)
                .build();

        // When
        long latency = sensorData.getLatencyMs();

        // Then
        assertThat(latency).isGreaterThanOrEqualTo(400);
        assertThat(latency).isLessThanOrEqualTo(600);
    }

    @Test
    @DisplayName("Should return zero latency when timestamps are null")
    void shouldReturnZeroLatencyWhenTimestampsNull() {
        // Given
        SensorData sensorData = SensorData.builder()
                .timestamp(null)
                .createdAt(null)
                .build();

        // When
        long latency = sensorData.getLatencyMs();

        // Then
        assertThat(latency).isEqualTo(0);
    }

    @Test
    @DisplayName("Should set default values correctly")
    void shouldSetDefaultValuesCorrectly() {
        // Given & When
        SensorData sensorData = SensorData.builder()
                .timestamp(Instant.now())
                .assetId(UUID.randomUUID())
                .sensorId(UUID.randomUUID())
                .sensorCode("TEST-001")
                .sensorType("temperature")
                .value(25.5)
                .build();

        // Then
        assertThat(sensorData.getQuality()).isEqualTo(100);  // Default quality
        assertThat(sensorData.getCreatedAt()).isNotNull();   // Auto-generated
    }
}
