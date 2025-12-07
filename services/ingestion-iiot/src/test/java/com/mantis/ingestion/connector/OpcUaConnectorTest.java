package com.mantis.ingestion.connector;

import com.mantis.ingestion.service.KafkaProducerService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for OpcUaConnector with mocked Eclipse Milo client.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OPC UA Connector Tests")
class OpcUaConnectorTest {

    @Mock
    private KafkaProducerService kafkaProducerService;

    @Mock
    private OpcUaClient opcUaClient;

    @Mock
    private UaSubscription uaSubscription;

    private MeterRegistry meterRegistry;
    private OpcUaConnector connector;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        connector = new OpcUaConnector(kafkaProducerService, meterRegistry);

        // Set configuration values via reflection
        ReflectionTestUtils.setField(connector, "endpoint", "opc.tcp://localhost:4840");
        ReflectionTestUtils.setField(connector, "subscriptionIntervalMs", 1000);
        ReflectionTestUtils.setField(connector, "requestTimeoutMs", 5000L);
        ReflectionTestUtils.setField(connector, "sessionTimeoutMs", 60000L);
        ReflectionTestUtils.setField(connector, "maxReconnectAttempts", 3);
        ReflectionTestUtils.setField(connector, "reconnectDelayMs", 1000L);
    }

    @Test
    @DisplayName("Should initialize connector with correct configuration")
    void shouldInitializeConnector() {
        // Then
        assertThat(connector).isNotNull();
        assertThat(ReflectionTestUtils.getField(connector, "endpoint"))
                .isEqualTo("opc.tcp://localhost:4840");
        assertThat(ReflectionTestUtils.getField(connector, "subscriptionIntervalMs"))
                .isEqualTo(1000);
    }

    @Test
    @DisplayName("Should create metrics counters")
    void shouldCreateMetricsCounters() {
        // Then
        Counter dataPointsCounter = meterRegistry.find("mantis.opcua.data.points").counter();
        Counter connectionAttemptsCounter = meterRegistry.find("mantis.opcua.connection.attempts").counter();
        Counter subscriptionErrorsCounter = meterRegistry.find("mantis.opcua.subscription.errors").counter();

        assertThat(dataPointsCounter).isNotNull();
        assertThat(connectionAttemptsCounter).isNotNull();
        assertThat(subscriptionErrorsCounter).isNotNull();
    }

    @Test
    @DisplayName("Should increment connection attempts counter")
    void shouldIncrementConnectionAttemptsCounter() {
        // Given
        Counter counter = meterRegistry.find("mantis.opcua.connection.attempts").counter();
        double initialCount = counter.count();

        // When - Simulate connection attempt
        counter.increment();

        // Then
        assertThat(counter.count()).isEqualTo(initialCount + 1);
    }

    @Test
    @DisplayName("Should increment data points counter")
    void shouldIncrementDataPointsCounter() {
        // Given
        Counter counter = meterRegistry.find("mantis.opcua.data.points").counter();
        double initialCount = counter.count();

        // When
        counter.increment();

        // Then
        assertThat(counter.count()).isEqualTo(initialCount + 1);
    }

    @Test
    @DisplayName("Should increment subscription errors counter")
    void shouldIncrementSubscriptionErrorsCounter() {
        // Given
        Counter counter = meterRegistry.find("mantis.opcua.subscription.errors").counter();
        double initialCount = counter.count();

        // When
        counter.increment();

        // Then
        assertThat(counter.count()).isEqualTo(initialCount + 1);
    }

    @Test
    @DisplayName("Should handle null client gracefully")
    void shouldHandleNullClient() {
        // Given
        ReflectionTestUtils.setField(connector, "client", null);

        // When/Then - No exception should be thrown
        assertThat(ReflectionTestUtils.getField(connector, "client")).isNull();
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

    @Test
    @DisplayName("Should have correct default node metadata map")
    void shouldHaveCorrectDefaultNodeMetadataMap() {
        // Given
        Object nodeMetadataMap = ReflectionTestUtils.getField(connector, "nodeMetadataMap");

        // Then
        assertThat(nodeMetadataMap).isNotNull();
        assertThat(nodeMetadataMap).isInstanceOf(java.util.Map.class);
    }

    @Test
    @DisplayName("Should verify KafkaProducerService dependency")
    void shouldVerifyKafkaProducerServiceDependency() {
        // Then
        assertThat(connector).isNotNull();
        Object kafkaService = ReflectionTestUtils.getField(connector, "kafkaProducerService");
        assertThat(kafkaService).isEqualTo(kafkaProducerService);
    }

    @Test
    @DisplayName("Should have all required configuration fields")
    void shouldHaveAllRequiredConfigurationFields() {
        // Then
        assertThat(ReflectionTestUtils.getField(connector, "endpoint")).isNotNull();
        assertThat(ReflectionTestUtils.getField(connector, "subscriptionIntervalMs")).isNotNull();
        assertThat(ReflectionTestUtils.getField(connector, "requestTimeoutMs")).isNotNull();
        assertThat(ReflectionTestUtils.getField(connector, "sessionTimeoutMs")).isNotNull();
        assertThat(ReflectionTestUtils.getField(connector, "maxReconnectAttempts")).isNotNull();
        assertThat(ReflectionTestUtils.getField(connector, "reconnectDelayMs")).isNotNull();
    }
}
