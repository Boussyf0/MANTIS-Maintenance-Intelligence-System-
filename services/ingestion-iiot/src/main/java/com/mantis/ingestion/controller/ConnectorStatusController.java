package com.mantis.ingestion.controller;

import com.mantis.ingestion.connector.ModbusConnector;
import com.mantis.ingestion.connector.MqttConnector;
import com.mantis.ingestion.connector.OpcUaConnector;
import com.mantis.ingestion.service.EdgeBufferService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Contrôleur pour vérifier le statut des connecteurs IIoT.
 */
@RestController
@RequestMapping("/api/v1/connectors")
@RequiredArgsConstructor
public class ConnectorStatusController {

    @Autowired(required = false)
    private OpcUaConnector opcUaConnector;

    @Autowired(required = false)
    private MqttConnector mqttConnector;

    @Autowired(required = false)
    private ModbusConnector modbusConnector;

    private final EdgeBufferService edgeBufferService;

    /**
     * Obtient le statut de tous les connecteurs.
     *
     * @return statut global
     */
    @GetMapping("/status")
    public ResponseEntity<ConnectorStatus> getStatus() {
        ConnectorStatus status = ConnectorStatus.builder()
                .timestamp(Instant.now())
                .opcua(opcUaConnector != null ?
                        ConnectorInfo.builder()
                                .enabled(true)
                                .connected(opcUaConnector.isConnected())
                                .build() :
                        ConnectorInfo.builder().enabled(false).connected(false).build())
                .mqtt(mqttConnector != null ?
                        ConnectorInfo.builder()
                                .enabled(true)
                                .connected(mqttConnector.isConnected())
                                .build() :
                        ConnectorInfo.builder().enabled(false).connected(false).build())
                .modbus(modbusConnector != null ?
                        ConnectorInfo.builder()
                                .enabled(true)
                                .connected(modbusConnector.isConnected())
                                .details("Configured registers: " + modbusConnector.getConfiguredRegistersCount())
                                .build() :
                        ConnectorInfo.builder().enabled(false).connected(false).build())
                .edgeBuffer(EdgeBufferInfo.builder()
                        .enabled(edgeBufferService.isEnabled())
                        .currentSize(edgeBufferService.size())
                        .stats(edgeBufferService.getStats())
                        .build())
                .build();

        return ResponseEntity.ok(status);
    }

    /**
     * Obtient le statut du connecteur OPC UA.
     */
    @GetMapping("/opcua/status")
    public ResponseEntity<ConnectorInfo> getOpcUaStatus() {
        if (opcUaConnector == null) {
            return ResponseEntity.ok(ConnectorInfo.builder()
                    .enabled(false)
                    .connected(false)
                    .details("OPC UA connector not configured")
                    .build());
        }

        return ResponseEntity.ok(ConnectorInfo.builder()
                .enabled(true)
                .connected(opcUaConnector.isConnected())
                .details("OPC UA connector active")
                .build());
    }

    /**
     * Obtient le statut du connecteur MQTT.
     */
    @GetMapping("/mqtt/status")
    public ResponseEntity<ConnectorInfo> getMqttStatus() {
        if (mqttConnector == null) {
            return ResponseEntity.ok(ConnectorInfo.builder()
                    .enabled(false)
                    .connected(false)
                    .details("MQTT connector not configured")
                    .build());
        }

        return ResponseEntity.ok(ConnectorInfo.builder()
                .enabled(true)
                .connected(mqttConnector.isConnected())
                .details("MQTT connector active")
                .build());
    }

    /**
     * Obtient le statut du connecteur Modbus.
     */
    @GetMapping("/modbus/status")
    public ResponseEntity<ConnectorInfo> getModbusStatus() {
        if (modbusConnector == null) {
            return ResponseEntity.ok(ConnectorInfo.builder()
                    .enabled(false)
                    .connected(false)
                    .details("Modbus connector not configured")
                    .build());
        }

        return ResponseEntity.ok(ConnectorInfo.builder()
                .enabled(true)
                .connected(modbusConnector.isConnected())
                .details("Registers configured: " + modbusConnector.getConfiguredRegistersCount())
                .build());
    }

    @Data
    @Builder
    public static class ConnectorStatus {
        private Instant timestamp;
        private ConnectorInfo opcua;
        private ConnectorInfo mqtt;
        private ConnectorInfo modbus;
        private EdgeBufferInfo edgeBuffer;
    }

    @Data
    @Builder
    public static class ConnectorInfo {
        private boolean enabled;
        private boolean connected;
        private String details;
    }

    @Data
    @Builder
    public static class EdgeBufferInfo {
        private boolean enabled;
        private int currentSize;
        private EdgeBufferService.BufferStats stats;
    }
}
