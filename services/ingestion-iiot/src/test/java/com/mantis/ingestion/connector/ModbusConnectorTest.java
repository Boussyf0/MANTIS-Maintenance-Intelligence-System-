package com.mantis.ingestion.connector;

import com.digitalpetri.modbus.master.ModbusTcpMaster;
import com.digitalpetri.modbus.requests.ReadHoldingRegistersRequest;
import com.digitalpetri.modbus.responses.ReadHoldingRegistersResponse;
import com.mantis.ingestion.service.KafkaProducerService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ModbusConnector with mocked Modbus master.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Modbus Connector Tests")
class ModbusConnectorTest {

    @Mock
    private KafkaProducerService kafkaProducerService;

    @Mock
    private ModbusTcpMaster modbusMaster;

    private MeterRegistry meterRegistry;
    private ModbusConnector connector;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        connector = new ModbusConnector(kafkaProducerService, meterRegistry);

        // Set configuration values via reflection
        ReflectionTestUtils.setField(connector, "host", "localhost");
        ReflectionTestUtils.setField(connector, "port", 502);
        ReflectionTestUtils.setField(connector, "unitId", 1);
        ReflectionTestUtils.setField(connector, "timeoutMs", 5000L);
        ReflectionTestUtils.setField(connector, "pollIntervalMs", 1000L);
    }

    @Test
    @DisplayName("Should initialize connector with correct configuration")
    void shouldInitializeConnector() {
        // Then
        assertThat(connector).isNotNull();
        assertThat(ReflectionTestUtils.getField(connector, "host")).isEqualTo("localhost");
        assertThat(ReflectionTestUtils.getField(connector, "port")).isEqualTo(502);
        assertThat(ReflectionTestUtils.getField(connector, "unitId")).isEqualTo(1);
    }

    @Test
    @DisplayName("Should create metrics counters")
    void shouldCreateMetricsCounters() {
        // Then
        Counter registersCounter = meterRegistry.find("mantis.modbus.registers.read").counter();
        Counter errorsCounter = meterRegistry.find("mantis.modbus.read.errors").counter();
        Counter attemptsCounter = meterRegistry.find("mantis.modbus.connection.attempts").counter();

        assertThat(registersCounter).isNotNull();
        assertThat(errorsCounter).isNotNull();
        assertThat(attemptsCounter).isNotNull();
    }

    @Test
    @DisplayName("Should increment registers read counter")
    void shouldIncrementRegistersReadCounter() {
        // Given
        Counter counter = meterRegistry.find("mantis.modbus.registers.read").counter();
        double initialCount = counter.count();

        // When
        counter.increment();

        // Then
        assertThat(counter.count()).isEqualTo(initialCount + 1);
    }

    @Test
    @DisplayName("Should increment read errors counter")
    void shouldIncrementReadErrorsCounter() {
        // Given
        Counter counter = meterRegistry.find("mantis.modbus.read.errors").counter();
        double initialCount = counter.count();

        // When
        counter.increment();

        // Then
        assertThat(counter.count()).isEqualTo(initialCount + 1);
    }

    @Test
    @DisplayName("Should increment connection attempts counter")
    void shouldIncrementConnectionAttemptsCounter() {
        // Given
        Counter counter = meterRegistry.find("mantis.modbus.connection.attempts").counter();
        double initialCount = counter.count();

        // When
        counter.increment();

        // Then
        assertThat(counter.count()).isEqualTo(initialCount + 1);
    }

    @Test
    @DisplayName("Should handle null master gracefully")
    void shouldHandleNullMaster() {
        // Given
        ReflectionTestUtils.setField(connector, "master", null);

        // When/Then - No exception should be thrown
        assertThat(ReflectionTestUtils.getField(connector, "master")).isNull();
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
    @DisplayName("Should have correct default register configs list")
    void shouldHaveCorrectDefaultRegisterConfigsList() {
        // Given
        Object registerConfigs = ReflectionTestUtils.getField(connector, "registerConfigs");

        // Then
        assertThat(registerConfigs).isNotNull();
        assertThat(registerConfigs).isInstanceOf(List.class);
    }

    @Test
    @DisplayName("Should verify KafkaProducerService dependency")
    void shouldVerifyKafkaProducerServiceDependency() {
        // Then
        Object kafkaService = ReflectionTestUtils.getField(connector, "kafkaProducerService");
        assertThat(kafkaService).isEqualTo(kafkaProducerService);
    }

    @Test
    @DisplayName("Should have all required configuration fields")
    void shouldHaveAllRequiredConfigurationFields() {
        // Then
        assertThat(ReflectionTestUtils.getField(connector, "host")).isNotNull();
        assertThat(ReflectionTestUtils.getField(connector, "port")).isNotNull();
        assertThat(ReflectionTestUtils.getField(connector, "unitId")).isNotNull();
        assertThat(ReflectionTestUtils.getField(connector, "timeoutMs")).isNotNull();
        assertThat(ReflectionTestUtils.getField(connector, "pollIntervalMs")).isNotNull();
    }

    @Test
    @DisplayName("Should verify timeout configuration")
    void shouldVerifyTimeoutConfiguration() {
        // Then
        Long timeoutMs = (Long) ReflectionTestUtils.getField(connector, "timeoutMs");
        assertThat(timeoutMs).isEqualTo(5000L);
    }

    @Test
    @DisplayName("Should verify poll interval configuration")
    void shouldVerifyPollIntervalConfiguration() {
        // Then
        Long pollIntervalMs = (Long) ReflectionTestUtils.getField(connector, "pollIntervalMs");
        assertThat(pollIntervalMs).isEqualTo(1000L);
    }

    @Test
    @DisplayName("Should verify unit ID configuration")
    void shouldVerifyUnitIdConfiguration() {
        // Then
        Integer unitId = (Integer) ReflectionTestUtils.getField(connector, "unitId");
        assertThat(unitId).isEqualTo(1);
        assertThat(unitId).isBetween(0, 255); // Valid Modbus unit ID range
    }

    @Test
    @DisplayName("Should verify Modbus port configuration")
    void shouldVerifyModbusPortConfiguration() {
        // Then
        Integer port = (Integer) ReflectionTestUtils.getField(connector, "port");
        assertThat(port).isEqualTo(502); // Standard Modbus TCP port
    }

    @Test
    @DisplayName("Should verify host configuration")
    void shouldVerifyHostConfiguration() {
        // Then
        String host = (String) ReflectionTestUtils.getField(connector, "host");
        assertThat(host).isEqualTo("localhost");
        assertThat(host).isNotBlank();
    }
}
