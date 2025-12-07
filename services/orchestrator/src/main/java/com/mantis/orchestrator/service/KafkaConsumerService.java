package com.mantis.orchestrator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mantis.orchestrator.model.AnomalyEvent;
import com.mantis.orchestrator.model.RULPrediction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final RuleEngineService ruleEngineService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.topic.anomaly-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeAnomaly(String message) {
        try {
            AnomalyEvent event = objectMapper.readValue(message, AnomalyEvent.class);
            ruleEngineService.processAnomaly(event);
        } catch (Exception e) {
            log.error("Error processing anomaly event: {}", message, e);
        }
    }

    @KafkaListener(topics = "${app.topic.rul-predictions}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeRUL(String message) {
        try {
            RULPrediction prediction = objectMapper.readValue(message, RULPrediction.class);
            ruleEngineService.processRUL(prediction);
        } catch (Exception e) {
            log.error("Error processing RUL prediction: {}", message, e);
        }
    }
}
