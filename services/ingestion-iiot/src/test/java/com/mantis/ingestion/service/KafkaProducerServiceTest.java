package com.mantis.ingestion.service;

import com.mantis.ingestion.TestDataFactory;
import com.mantis.ingestion.model.SensorData;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for KafkaProducerService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaProducerService Tests")
class KafkaProducerServiceTest {

    @Mock
    private KafkaTemplate<String, SensorData> kafkaTemplate;

    @Mock
    private EdgeBufferService edgeBufferService;

    private KafkaProducerService kafkaProducerService;
    private MeterRegistry meterRegistry;
    private final String testTopic = "sensor.raw.test";

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        kafkaProducerService = new KafkaProducerService(
                kafkaTemplate,
                testTopic,
                edgeBufferService,
                meterRegistry
        );
    }

    @Test
    @DisplayName("Should send valid sensor data to Kafka")
    void shouldSendValidSensorData() {
        // Given
        SensorData sensorData = TestDataFactory.createValidSensorData();
        SendResult<String, SensorData> sendResult = createMockSendResult();

        CompletableFuture<SendResult<String, SensorData>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(anyString(), anyString(), any(SensorData.class))).thenReturn(future);

        // When
        CompletableFuture<SendResult<String, SensorData>> result = kafkaProducerService.sendSensorData(sensorData);

        // Then
        assertThat(result).isCompletedWithValue(sendResult);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<SensorData> dataCaptor = ArgumentCaptor.forClass(SensorData.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), dataCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo(testTopic);
        assertThat(keyCaptor.getValue()).isEqualTo(sensorData.getAssetId().toString());
        assertThat(dataCaptor.getValue()).isEqualTo(sensorData);
    }

    @Test
    @DisplayName("Should reject invalid sensor data")
    void shouldRejectInvalidSensorData() {
        // Given
        SensorData invalidData = TestDataFactory.createInvalidSensorData();

        // When
        CompletableFuture<SendResult<String, SensorData>> result = kafkaProducerService.sendSensorData(invalidData);

        // Then
        assertThat(result).isCompletedExceptionally();
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any(SensorData.class));
    }

    @Test
    @DisplayName("Should buffer data when Kafka send fails")
    void shouldBufferDataWhenKafkaFails() {
        // Given
        SensorData sensorData = TestDataFactory.createValidSensorData();
        when(edgeBufferService.isEnabled()).thenReturn(true);

        CompletableFuture<SendResult<String, SensorData>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka unavailable"));
        when(kafkaTemplate.send(anyString(), anyString(), any(SensorData.class))).thenReturn(failedFuture);

        // When
        CompletableFuture<SendResult<String, SensorData>> result = kafkaProducerService.sendSensorData(sensorData);

        // Then
        assertThat(result).isCompletedExceptionally();

        // Wait for async callback
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(edgeBufferService).buffer(sensorData);
    }

    @Test
    @DisplayName("Should not buffer when edge buffer is disabled")
    void shouldNotBufferWhenEdgeBufferDisabled() {
        // Given
        SensorData sensorData = TestDataFactory.createValidSensorData();
        when(edgeBufferService.isEnabled()).thenReturn(false);

        CompletableFuture<SendResult<String, SensorData>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka unavailable"));
        when(kafkaTemplate.send(anyString(), anyString(), any(SensorData.class))).thenReturn(failedFuture);

        // When
        kafkaProducerService.sendSensorData(sensorData);

        // Wait for async callback
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Then
        verify(edgeBufferService, never()).buffer(any());
    }

    @Test
    @DisplayName("Should send data synchronously")
    void shouldSendDataSynchronously() throws Exception {
        // Given
        SensorData sensorData = TestDataFactory.createValidSensorData();
        SendResult<String, SensorData> sendResult = createMockSendResult();

        CompletableFuture<SendResult<String, SensorData>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(anyString(), anyString(), any(SensorData.class))).thenReturn(future);

        // When
        SendResult<String, SensorData> result = kafkaProducerService.sendSensorDataSync(sensorData);

        // Then
        assertThat(result).isEqualTo(sendResult);
        verify(kafkaTemplate).send(eq(testTopic), eq(sensorData.getAssetId().toString()), eq(sensorData));
    }

    @Test
    @DisplayName("Should return producer stats")
    void shouldReturnProducerStats() {
        // Given
        SensorData sensorData = TestDataFactory.createValidSensorData();
        SendResult<String, SensorData> sendResult = createMockSendResult();

        CompletableFuture<SendResult<String, SensorData>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(anyString(), anyString(), any(SensorData.class))).thenReturn(future);

        // Send some data to generate stats
        kafkaProducerService.sendSensorData(sensorData);

        // Wait for async completion
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When
        KafkaProducerService.ProducerStats stats = kafkaProducerService.getStats();

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.getMessagesProduced()).isGreaterThanOrEqualTo(0);
        assertThat(stats.getMessagesFailed()).isGreaterThanOrEqualTo(0);
        assertThat(stats.getAverageSendLatencyMs()).isNotNull();
        assertThat(stats.getMaxSendLatencyMs()).isNotNull();
    }

    private SendResult<String, SensorData> createMockSendResult() {
        ProducerRecord<String, SensorData> producerRecord = new ProducerRecord<>(
                testTopic, 0, "test-key", TestDataFactory.createValidSensorData()
        );

        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition(testTopic, 0),
                0L, 0, 0L, System.currentTimeMillis(), 0, 0
        );

        return new SendResult<>(producerRecord, metadata);
    }
}
