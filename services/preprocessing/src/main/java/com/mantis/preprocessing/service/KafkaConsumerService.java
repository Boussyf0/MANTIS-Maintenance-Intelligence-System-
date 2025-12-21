package com.mantis.preprocessing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mantis.preprocessing.model.ProcessedData;
import com.mantis.preprocessing.model.RULPrediction;
import com.mantis.preprocessing.model.SensorData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final DataProcessingService processingService;
    private final KafkaTemplate<String, ProcessedData> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final InfluxDBService influxDBService;

    @Value("${app.topic.output}")
    private String outputTopic;

    @KafkaListener(topics = "${app.topic.input}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message) {
        try {
            SensorData data = objectMapper.readValue(message, SensorData.class);
            log.debug("Received data for machine {}", data.getMachine_id());

            // Write raw data to InfluxDB
            influxDBService.writeSensorData(data);

            Optional<ProcessedData> result = processingService.process(data);

            result.ifPresent(processedData -> {
                kafkaTemplate.send(outputTopic, processedData);
                log.info("Processed and sent data for machine {}, cycle {}",
                        processedData.getMachine_id(), processedData.getCycle());
            });

        } catch (Exception e) {
            log.error("Error processing sensor data: {}", message, e);
        }
    }

    @KafkaListener(topics = "${app.topic.rul-predictions}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeRUL(String message) {
        try {
            RULPrediction prediction = objectMapper.readValue(message, RULPrediction.class);
            log.debug("Received RUL prediction: {}", prediction);

            // Write to InfluxDB
            influxDBService.writeRULPrediction(prediction);

        } catch (Exception e) {
            log.error("Error processing RUL prediction: {}", message, e);
        }
    }
}
