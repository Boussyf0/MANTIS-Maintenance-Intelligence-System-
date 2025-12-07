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

            machineStateService.updateRUL(machineId, timestamp, rul);
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
}
