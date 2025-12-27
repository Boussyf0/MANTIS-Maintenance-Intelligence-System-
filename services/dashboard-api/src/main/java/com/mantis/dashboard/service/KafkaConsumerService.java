package com.mantis.dashboard.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mantis.dashboard.model.AlertEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final MachineStateService machineStateService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.topic.anomaly-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeAnomaly(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String machineId = node.get("machine_id").asText();
            String timestamp = node.get("timestamp").asText();
            double score = node.get("anomaly_score").asDouble();
            boolean isAnomaly = node.get("is_anomaly").asBoolean();

            machineStateService.updateAnomalyStatus(machineId, timestamp, score, isAnomaly);
        } catch (Exception e) {
            log.error("Error processing anomaly event", e);
        }
    }

    @KafkaListener(topics = "${app.topic.rul-predictions}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeRUL(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String machineId = node.get("machine_id").asText();
            String timestamp = node.get("timestamp").asText();
            double rul = node.get("predicted_rul").asDouble();
            int cycle = node.has("cycle") ? node.get("cycle").asInt() : 0;
            log.info("DEBUG: Received RUL for {}: {}", machineId, rul);

            machineStateService.updateRUL(machineId, timestamp, rul, cycle);
        } catch (Exception e) {
            log.error("Error processing RUL prediction", e);
        }
    }

    @KafkaListener(topics = "${app.topic.alerts}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeAlert(String message) {
        try {
            AlertEntity alert = objectMapper.readValue(message, AlertEntity.class);
            machineStateService.saveAlert(alert);
        } catch (Exception e) {
            log.error("Error processing alert", e);
        }
    }

    @KafkaListener(topics = "${app.topic.sensor-raw}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeSensors(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String machineId = node.get("machine_id").asText();
            String timestamp = node.get("timestamp").asText();

            java.util.Map<String, Double> sensorMap = new java.util.HashMap<>();
            if (node.has("sensors") && node.get("sensors").isArray()) {
                int i = 1;
                for (JsonNode val : node.get("sensors")) {
                    sensorMap.put(String.format("sensor_%02d", i++), val.asDouble());
                }
            }

            if (!sensorMap.isEmpty()) {
                machineStateService.updateSensors(machineId, timestamp, sensorMap);
            }
        } catch (Exception e) {
            log.error("Error processing sensor data", e);
        }
    }
}
