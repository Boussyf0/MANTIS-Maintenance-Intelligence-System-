package com.mantis.ingestion.connector;

import com.digitalpetri.modbus.master.ModbusTcpMaster;
import com.digitalpetri.modbus.master.ModbusTcpMasterConfig;
import com.digitalpetri.modbus.requests.ReadHoldingRegistersRequest;
import com.digitalpetri.modbus.requests.ReadInputRegistersRequest;
import com.digitalpetri.modbus.responses.ReadHoldingRegistersResponse;
import com.digitalpetri.modbus.responses.ReadInputRegistersResponse;
import com.mantis.ingestion.model.SensorData;
import com.mantis.ingestion.service.KafkaProducerService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Connecteur Modbus TCP.
 *
 * Interroge périodiquement des registres Modbus et publie
 * les données vers Kafka.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "mantis.ingestion.modbus", name = "enabled", havingValue = "true")
public class ModbusConnector {

    @Value("${mantis.ingestion.modbus.host}")
    private String host;

    @Value("${mantis.ingestion.modbus.port}")
    private int port;

    @Value("${mantis.ingestion.modbus.unit-id}")
    private int unitId;

    @Value("${mantis.ingestion.modbus.timeout-ms}")
    private long timeoutMs;

    @Value("${mantis.ingestion.modbus.poll-interval-ms}")
    private long pollIntervalMs;

    private final KafkaProducerService kafkaProducerService;
    private final AtomicBoolean connected = new AtomicBoolean(false);

    private ModbusTcpMaster master;

    // Métriques
    private final Counter registersReadCounter;
    private final Counter readErrorsCounter;
    private final Counter connectionAttemptsCounter;

    // Configuration des registres à lire
    private final List<RegisterConfig> registerConfigs = new ArrayList<>();

    public ModbusConnector(KafkaProducerService kafkaProducerService, MeterRegistry meterRegistry) {
        this.kafkaProducerService = kafkaProducerService;

        this.registersReadCounter = Counter.builder("mantis.modbus.registers.read")
                .description("Total Modbus registers read")
                .register(meterRegistry);

        this.readErrorsCounter = Counter.builder("mantis.modbus.read.errors")
                .description("Total Modbus read errors")
                .register(meterRegistry);

        this.connectionAttemptsCounter = Counter.builder("mantis.modbus.connection.attempts")
                .description("Total Modbus connection attempts")
                .register(meterRegistry);
    }

    @PostConstruct
    public void connect() {
        log.info("Initializing Modbus TCP connector: host={}, port={}, unitId={}",
                host, port, unitId);

        try {
            connectionAttemptsCounter.increment();

            // Configuration du master Modbus TCP
            ModbusTcpMasterConfig config = new ModbusTcpMasterConfig.Builder(host)
                    .setPort(port)
                    .build();

            master = new ModbusTcpMaster(config);

            // Tester la connexion avec une lecture simple
            testConnection();

            connected.set(true);
            log.info("Modbus TCP master initialized successfully");

        } catch (Exception e) {
            log.error("Failed to initialize Modbus TCP master: {}", e.getMessage(), e);
            connected.set(false);
            throw new RuntimeException("Modbus connection failed", e);
        }
    }

    /**
     * Teste la connexion Modbus en lisant le registre 0.
     */
    private void testConnection() {
        try {
            readHoldingRegisters(0, 1).get();
            log.info("Modbus connection test successful");
        } catch (Exception e) {
            throw new RuntimeException("Modbus connection test failed", e);
        }
    }

    /**
     * Configure un registre Modbus à interroger.
     *
     * @param registerType Type de registre (holding ou input)
     * @param address      Adresse du registre
     * @param assetId      ID de l'asset
     * @param sensorId     ID du capteur
     * @param sensorCode   Code du capteur
     * @param sensorType   Type de capteur
     * @param unit         Unité de mesure
     * @param scaleFactor  Facteur d'échelle (ex: 0.1 pour convertir 255 en 25.5)
     */
    public void configureRegister(
            RegisterType registerType,
            int address,
            UUID assetId,
            UUID sensorId,
            String sensorCode,
            String sensorType,
            String unit,
            double scaleFactor) {
        RegisterConfig config = new RegisterConfig(
                registerType, address, assetId, sensorId,
                sensorCode, sensorType, unit, scaleFactor);

        registerConfigs.add(config);

        log.info("Configured Modbus register: type={}, address={}, sensorCode={}",
                registerType, address, sensorCode);
    }

    /**
     * Tâche planifiée pour lire tous les registres configurés.
     */
    @Scheduled(fixedDelayString = "${mantis.ingestion.modbus.poll-interval-ms}")
    public void pollRegisters() {
        if (!connected.get() || registerConfigs.isEmpty()) {
            return;
        }

        log.debug("Polling {} Modbus registers", registerConfigs.size());

        for (RegisterConfig config : registerConfigs) {
            try {
                readAndPublish(config);
            } catch (Exception e) {
                log.error("Error reading Modbus register {}: {}",
                        config.address, e.getMessage());
                readErrorsCounter.increment();
            }
        }
    }

    /**
     * Lit un registre et publie la valeur vers Kafka.
     */
    private void readAndPublish(RegisterConfig config) {
        try {
            CompletableFuture<? extends Object> future;

            if (config.registerType == RegisterType.HOLDING) {
                future = readHoldingRegisters(config.address, 1);
            } else {
                future = readInputRegisters(config.address, 1);
            }

            future.whenComplete((response, ex) -> {
                if (ex != null) {
                    log.error("Failed to read register {}: {}", config.address, ex.getMessage());
                    readErrorsCounter.increment();
                    return;
                }

                try {
                    int rawValue = extractValue(response);
                    double scaledValue = rawValue * config.scaleFactor;

                    SensorData sensorData = SensorData.builder()
                            .timestamp(Instant.now())
                            .assetId(config.assetId)
                            .sensorId(config.sensorId)
                            .sensorCode(config.sensorCode)
                            .sensorType(config.sensorType)
                            .value(scaledValue)
                            .unit(config.unit)
                            .quality(100)
                            .source("modbus")
                            .metadata(Map.of(
                                    "modbusRegisterType", config.registerType.name(),
                                    "modbusAddress", config.address,
                                    "rawValue", rawValue,
                                    "scaleFactor", config.scaleFactor))
                            .build();

                    kafkaProducerService.sendSensorData(sensorData);
                    registersReadCounter.increment();

                    log.debug("Modbus data published: address={}, sensorCode={}, value={}",
                            config.address, config.sensorCode, scaledValue);

                } catch (Exception e) {
                    log.error("Error processing Modbus response: {}", e.getMessage(), e);
                    readErrorsCounter.increment();
                }
            });

        } catch (Exception e) {
            log.error("Error initiating Modbus read: {}", e.getMessage(), e);
            readErrorsCounter.increment();
        }
    }

    /**
     * Lit des registres Holding.
     */
    public CompletableFuture<ReadHoldingRegistersResponse> readHoldingRegisters(int address, int quantity) {
        if (!connected.get()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Modbus master not connected"));
        }

        ReadHoldingRegistersRequest request = new ReadHoldingRegistersRequest(address, quantity);

        return master.sendRequest(request, unitId);
    }

    /**
     * Lit des registres Input.
     */
    public CompletableFuture<ReadInputRegistersResponse> readInputRegisters(int address, int quantity) {
        if (!connected.get()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Modbus master not connected"));
        }

        ReadInputRegistersRequest request = new ReadInputRegistersRequest(address, quantity);

        return master.sendRequest(request, unitId);
    }

    /**
     * Extrait la première valeur d'une réponse Modbus.
     */
    private int extractValue(Object response) {
        ByteBuf registers = null;
        try {
            if (response instanceof ReadHoldingRegistersResponse) {
                registers = ((ReadHoldingRegistersResponse) response).getRegisters();
            } else if (response instanceof ReadInputRegistersResponse) {
                registers = ((ReadInputRegistersResponse) response).getRegisters();
            } else {
                throw new IllegalArgumentException("Unknown response type: " +
                        response.getClass().getName());
            }

            // Lire le premier registre (16 bits)
            return registers.readUnsignedShort();

        } finally {
            // Libérer le buffer
            if (registers != null) {
                ReferenceCountUtil.release(registers);
            }
        }
    }

    /**
     * Vérifie si le connecteur est connecté.
     */
    public boolean isConnected() {
        return connected.get() && master != null;
    }

    /**
     * Obtient le nombre de registres configurés.
     */
    public int getConfiguredRegistersCount() {
        return registerConfigs.size();
    }

    @PreDestroy
    public void disconnect() {
        if (master != null && connected.get()) {
            try {
                log.info("Disconnecting Modbus TCP master...");
                master.disconnect();
                connected.set(false);
                log.info("Modbus TCP master disconnected");
            } catch (Exception e) {
                log.error("Error disconnecting Modbus master: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Types de registres Modbus.
     */
    public enum RegisterType {
        HOLDING, // Read/Write registers (function code 3)
        INPUT // Read-only registers (function code 4)
    }

    /**
     * Configuration d'un registre Modbus.
     */
    private static class RegisterConfig {
        final RegisterType registerType;
        final int address;
        final UUID assetId;
        final UUID sensorId;
        final String sensorCode;
        final String sensorType;
        final String unit;
        final double scaleFactor;

        RegisterConfig(
                RegisterType registerType, int address, UUID assetId,
                UUID sensorId, String sensorCode, String sensorType,
                String unit, double scaleFactor) {
            this.registerType = registerType;
            this.address = address;
            this.assetId = assetId;
            this.sensorId = sensorId;
            this.sensorCode = sensorCode;
            this.sensorType = sensorType;
            this.unit = unit;
            this.scaleFactor = scaleFactor;
        }
    }
}
