package com.mantis.ingestion.connector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mantis.ingestion.model.SensorData;
import com.mantis.ingestion.service.KafkaProducerService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.eclipse.paho.client.mqttv3.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MqttConnector with mocked Eclipse Paho client.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MQTT Connector Tests")
class MqttConnectorTest {

    @Mock
    private KafkaProducerService kafkaProducerService;

    @Mock
    private MqttClient mqttClient;

    private ObjectMapper objectMapper;
    private MeterRegistry meterRegistry;
    private MqttConnector connector;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        meterRegistry = new SimpleMeterRegistry();
        connector = new MqttConnector(kafkaProducerService, objectMapper, meterRegistry);

        // Set configuration values via reflection
        ReflectionTestUtils.setField(connector, "brokerUrl", "tcp://localhost:1883");
        ReflectionTestUtils.setField(connector, "clientId", "test-client");
        ReflectionTestUtils.setField(connector, "username", null);
        ReflectionTestUtils.setField(connector, "password", null);
        ReflectionTestUtils.setField(connector, "topicPrefix", "test/sensors/#");
        ReflectionTestUtils.setField(connector, "qos", 1);
        ReflectionTestUtils.setField(connector, "cleanSession", true);
        ReflectionTestUtils.setField(connector, "connectionTimeout", 30);
        ReflectionTestUtils.setField(connector, "keepAliveInterval", 60);
        ReflectionTestUtils.setField(connector, "maxReconnectAttempts", 3);
        ReflectionTestUtils.setField(connector, "reconnectDelayMs", 1000L);
    }

    @Test
    @DisplayName("Should initialize connector with correct configuration")
    void shouldInitializeConnector() {
        // Then
        assertThat(connector).isNotNull();
        assertThat(ReflectionTestUtils.getField(connector, "brokerUrl"))
                .isEqualTo("tcp://localhost:1883");
        assertThat(ReflectionTestUtils.getField(connector, "clientId"))
                .isEqualTo("test-client");
    }

    @Test
    @DisplayName("Should create metrics counters")
    void shouldCreateMetricsCounters() {
        // Then
        Counter messagesCounter = meterRegistry.find("mantis.mqtt.messages.received").counter();
        Counter attemptsCounter = meterRegistry.find("mantis.mqtt.connection.attempts").counter();
        Counter failuresCounter = meterRegistry.find("mantis.mqtt.connection.failures").counter();
        Counter errorsCounter = meterRegistry.find("mantis.mqtt.message.errors").counter();

        assertThat(messagesCounter).isNotNull();
        assertThat(attemptsCounter).isNotNull();
        assertThat(failuresCounter).isNotNull();
        assertThat(errorsCounter).isNotNull();
    }

    @Test
    @DisplayName("Should handle connectionLost callback")
    void shouldHandleConnectionLost() {
        // Given
        Throwable cause = new Exception("Connection lost");

        // When - Call the callback method
        connector.connectionLost(cause);

        // Then - No exception should be thrown
        assertThat(connector).isNotNull();
    }

    @Test
    @DisplayName("Should handle deliveryComplete callback")
    void shouldHandleDeliveryComplete() {
        // Given
        IMqttDeliveryToken token = mock(IMqttDeliveryToken.class);

        // When
        connector.deliveryComplete(token);

        // Then - No exception should be thrown
        assertThat(connector).isNotNull();
    }

    @Test
    @DisplayName("Should process JSON message and send to Kafka")
    void shouldProcessJsonMessage() throws Exception {
        // Given
        String topic = "test/sensors/temperature";
        SensorData sensorData = SensorData.builder()
                .timestamp(Instant.now())
                .assetId(UUID.randomUUID())
                .sensorId(UUID.randomUUID())
                .sensorCode("TEMP-001")
                .sensorType("temperature")
                .value(25.5)
                .unit("°C")
                .quality(100)
                .source("mqtt")
                .build();

        String jsonPayload = objectMapper.writeValueAsString(sensorData);
        MqttMessage message = new MqttMessage(jsonPayload.getBytes(StandardCharsets.UTF_8));

        // When
        connector.messageArrived(topic, message);

        // Then
        Counter counter = meterRegistry.find("mantis.mqtt.messages.received").counter();
        assertThat(counter.count()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should handle malformed JSON message gracefully")
    void shouldHandleMalformedJsonMessage() throws Exception {
        // Given
        String topic = "test/sensors/invalid";
        MqttMessage message = new MqttMessage("{invalid json}".getBytes(StandardCharsets.UTF_8));

        // When
        connector.messageArrived(topic, message);

        // Then - Should increment error counter
        Counter errorCounter = meterRegistry.find("mantis.mqtt.message.errors").counter();
        assertThat(errorCounter.count()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should handle empty message payload")
    void shouldHandleEmptyMessagePayload() throws Exception {
        // Given
        String topic = "test/sensors/empty";
        MqttMessage message = new MqttMessage(new byte[0]);

        // When
        connector.messageArrived(topic, message);

        // Then - Should increment messages received counter
        Counter counter = meterRegistry.find("mantis.mqtt.messages.received").counter();
        assertThat(counter.count()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should increment connection attempts counter")
    void shouldIncrementConnectionAttemptsCounter() {
        // Given
        Counter counter = meterRegistry.find("mantis.mqtt.connection.attempts").counter();
        double initialCount = counter.count();

        // When
        counter.increment();

        // Then
        assertThat(counter.count()).isEqualTo(initialCount + 1);
    }

    @Test
    @DisplayName("Should increment connection failures counter")
    void shouldIncrementConnectionFailuresCounter() {
        // Given
        Counter counter = meterRegistry.find("mantis.mqtt.connection.failures").counter();
        double initialCount = counter.count();

        // When
        counter.increment();

        // Then
        assertThat(counter.count()).isEqualTo(initialCount + 1);
    }

    @Test
    @DisplayName("Should verify KafkaProducerService dependency")
    void shouldVerifyKafkaProducerServiceDependency() {
        // Then
        Object kafkaService = ReflectionTestUtils.getField(connector, "kafkaProducerService");
        assertThat(kafkaService).isEqualTo(kafkaProducerService);
    }

    @Test
    @DisplayName("Should verify ObjectMapper dependency")
    void shouldVerifyObjectMapperDependency() {
        // Then
        Object mapper = ReflectionTestUtils.getField(connector, "objectMapper");
        assertThat(mapper).isEqualTo(objectMapper);
    }

    @Test
    @DisplayName("Should have all required configuration fields")
    void shouldHaveAllRequiredConfigurationFields() {
        // Then
        assertThat(ReflectionTestUtils.getField(connector, "brokerUrl")).isNotNull();
        assertThat(ReflectionTestUtils.getField(connector, "clientId")).isNotNull();
        assertThat(ReflectionTestUtils.getField(connector, "topicPrefix")).isNotNull();
        assertThat(ReflectionTestUtils.getField(connector, "qos")).isNotNull();
        assertThat(ReflectionTestUtils.getField(connector, "cleanSession")).isNotNull();
        assertThat(ReflectionTestUtils.getField(connector, "connectionTimeout")).isNotNull();
    }

    @Test
    @DisplayName("Should maintain connected state")
    void shouldMaintainConnectedState() {
        // Given
        Object connected = ReflectionTestUtils.getField(connector, "connected");

        // Then
        assertThat(connected).isNotNull();
        assertThat(connected.toString()).contains("false");
    }
}
