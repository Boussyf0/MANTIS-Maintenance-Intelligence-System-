package com.mantis.ingestion.service;

import com.mantis.ingestion.TestDataFactory;
import com.mantis.ingestion.model.SensorData;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for EdgeBufferService.
 */
@DisplayName("EdgeBufferService Tests")
class EdgeBufferServiceTest {

    private EdgeBufferService edgeBufferService;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        edgeBufferService = new EdgeBufferService(meterRegistry);

        // Configure the service for testing
        ReflectionTestUtils.setField(edgeBufferService, "enabled", true);
        ReflectionTestUtils.setField(edgeBufferService, "maxSize", 10);
        ReflectionTestUtils.setField(edgeBufferService, "flushIntervalMs", 1000L);

        edgeBufferService.init();
    }

    @Test
    @DisplayName("Should buffer sensor data when enabled")
    void shouldBufferSensorData() {
        // Given
        SensorData sensorData = TestDataFactory.createValidSensorData();

        // When
        boolean result = edgeBufferService.buffer(sensorData);

        // Then
        assertThat(result).isTrue();
        assertThat(edgeBufferService.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should not buffer when disabled")
    void shouldNotBufferWhenDisabled() {
        // Given
        ReflectionTestUtils.setField(edgeBufferService, "enabled", false);
        SensorData sensorData = TestDataFactory.createValidSensorData();

        // When
        boolean result = edgeBufferService.buffer(sensorData);

        // Then
        assertThat(result).isFalse();
        assertThat(edgeBufferService.size()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should drop data when buffer is full")
    void shouldDropDataWhenBufferFull() {
        // Given: Fill the buffer
        for (int i = 0; i < 10; i++) {
            edgeBufferService.buffer(TestDataFactory.createSensorData("SENSOR-" + i, 25.0));
        }
        assertThat(edgeBufferService.size()).isEqualTo(10);

        // When: Try to add one more
        SensorData extraData = TestDataFactory.createValidSensorData();
        boolean result = edgeBufferService.buffer(extraData);

        // Then
        assertThat(result).isFalse();
        assertThat(edgeBufferService.size()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should clear buffer")
    void shouldClearBuffer() {
        // Given
        edgeBufferService.buffer(TestDataFactory.createValidSensorData());
        edgeBufferService.buffer(TestDataFactory.createValidSensorData());
        assertThat(edgeBufferService.size()).isEqualTo(2);

        // When
        edgeBufferService.clear();

        // Then
        assertThat(edgeBufferService.size()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should return correct stats")
    void shouldReturnCorrectStats() {
        // Given
        edgeBufferService.buffer(TestDataFactory.createValidSensorData());
        edgeBufferService.buffer(TestDataFactory.createValidSensorData());

        // When
        EdgeBufferService.BufferStats stats = edgeBufferService.getStats();

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.isEnabled()).isTrue();
        assertThat(stats.getCurrentSize()).isEqualTo(2);
        assertThat(stats.getMaxSize()).isEqualTo(10);
        assertThat(stats.getTotalBuffered()).isEqualTo(2);
        assertThat(stats.getTotalDropped()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should increment dropped counter when full")
    void shouldIncrementDroppedCounter() {
        // Given: Fill the buffer
        for (int i = 0; i < 10; i++) {
            edgeBufferService.buffer(TestDataFactory.createSensorData("SENSOR-" + i, 25.0));
        }

        // When: Drop 3 items
        edgeBufferService.buffer(TestDataFactory.createValidSensorData());
        edgeBufferService.buffer(TestDataFactory.createValidSensorData());
        edgeBufferService.buffer(TestDataFactory.createValidSensorData());

        // Then
        EdgeBufferService.BufferStats stats = edgeBufferService.getStats();
        assertThat(stats.getTotalDropped()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should return buffered data")
    void shouldReturnBufferedData() {
        // Given
        SensorData data1 = TestDataFactory.createSensorData("SENSOR-1", 25.0);
        SensorData data2 = TestDataFactory.createSensorData("SENSOR-2", 30.0);

        edgeBufferService.buffer(data1);
        edgeBufferService.buffer(data2);

        // When
        var bufferedData = edgeBufferService.getBufferedData();

        // Then
        assertThat(bufferedData).isNotNull();
        assertThat(bufferedData.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should check if enabled")
    void shouldCheckIfEnabled() {
        // When enabled
        assertThat(edgeBufferService.isEnabled()).isTrue();

        // When disabled
        ReflectionTestUtils.setField(edgeBufferService, "enabled", false);
        assertThat(edgeBufferService.isEnabled()).isFalse();
    }
}
