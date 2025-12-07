package com.mantis.orchestrator.service;

import com.mantis.orchestrator.model.Alert;
import com.mantis.orchestrator.model.AnomalyEvent;
import com.mantis.orchestrator.model.RULPrediction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RuleEngineService {

    private final KafkaTemplate<String, Alert> kafkaTemplate;

    @Value("${app.topic.alerts}")
    private String alertsTopic;

    public void processAnomaly(AnomalyEvent event) {
        if (event.is_anomaly()) {
            Alert alert = Alert.builder()
                    .alertId(UUID.randomUUID().toString())
                    .machineId(event.getMachine_id())
                    .timestamp(event.getTimestamp())
                    .severity("WARNING")
                    .message("Anomaly detected: " + event.getDetails())
                    .source("ANOMALY_DETECTION")
                    .build();

            sendAlert(alert);
        }
    }

    public void processRUL(RULPrediction prediction) {
        double rul = prediction.getPredicted_rul();
        String severity = null;
        String message = null;

        if (rul < 20) {
            severity = "EMERGENCY";
            message = "RUL is critically low (" + rul + " cycles). Immediate maintenance required.";
        } else if (rul < 50) {
            severity = "CRITICAL";
            message = "RUL is low (" + rul + " cycles). Schedule maintenance soon.";
        }

        if (severity != null) {
            Alert alert = Alert.builder()
                    .alertId(UUID.randomUUID().toString())
                    .machineId(prediction.getMachine_id())
                    .timestamp(prediction.getTimestamp())
                    .severity(severity)
                    .message(message)
                    .source("RUL_PREDICTION")
                    .build();

            sendAlert(alert);
        }
    }

    private void sendAlert(Alert alert) {
        log.info("Generating Alert: [{}] {} - {}", alert.getSeverity(), alert.getMachineId(), alert.getMessage());
        kafkaTemplate.send(alertsTopic, alert);
    }
}
