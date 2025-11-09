package com.mantis.ingestion.controller;

import com.mantis.ingestion.dto.IngestionResponse;
import com.mantis.ingestion.dto.SensorDataRequest;
import com.mantis.ingestion.model.SensorData;
import com.mantis.ingestion.service.KafkaProducerService;
import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.support.SendResult;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Contrôleur REST pour l'ingestion de données de capteurs.
 *
 * Expose des endpoints HTTP pour permettre l'ingestion de données
 * par des clients externes (simulateurs, clients HTTP, etc.).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ingest")
@RequiredArgsConstructor
public class IngestionController {

    private final KafkaProducerService kafkaProducerService;

    /**
     * Ingère une donnée de capteur via HTTP POST.
     *
     * @param request données du capteur
     * @return réponse avec statut et métadonnées
     */
    @PostMapping
    @Timed(value = "mantis.ingestion.http.requests", description = "HTTP ingestion requests")
    public ResponseEntity<IngestionResponse> ingestSensorData(
            @Valid @RequestBody SensorDataRequest request
    ) {
        log.debug("Received ingestion request: sensorCode={}", request.getSensorCode());

        try {
            // Convertir le DTO en modèle
            SensorData sensorData = convertToSensorData(request);

            // Valider
            if (!sensorData.isValid()) {
                log.warn("Invalid sensor data: {}", request.getSensorCode());
                return ResponseEntity.badRequest()
                        .body(IngestionResponse.error("Invalid sensor data"));
            }

            // Envoyer vers Kafka (synchrone pour pouvoir retourner partition/offset)
            long startTime = System.currentTimeMillis();
            SendResult<String, SensorData> result = kafkaProducerService.sendSensorDataSync(sensorData);
            long latency = System.currentTimeMillis() - startTime;

            // Construire la réponse
            IngestionResponse response = IngestionResponse.success(
                    sensorData.getSensorCode(),
                    (long) result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset(),
                    latency
            );

            log.info("Data ingested successfully: sensorCode={}, partition={}, offset={}, latency={}ms",
                    sensorData.getSensorCode(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset(),
                    latency);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Failed to ingest sensor data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(IngestionResponse.error("Ingestion failed: " + e.getMessage()));
        }
    }

    /**
     * Ingère plusieurs données de capteurs en batch.
     *
     * @param requests liste de données de capteurs
     * @return liste des réponses
     */
    @PostMapping("/batch")
    @Timed(value = "mantis.ingestion.http.batch.requests", description = "HTTP batch ingestion requests")
    public ResponseEntity<List<IngestionResponse>> ingestSensorDataBatch(
            @Valid @RequestBody List<SensorDataRequest> requests
    ) {
        log.info("Received batch ingestion request: count={}", requests.size());

        try {
            // Envoyer toutes les données en parallèle
            List<CompletableFuture<IngestionResponse>> futures = requests.stream()
                    .map(request -> {
                        SensorData sensorData = convertToSensorData(request);
                        long startTime = System.currentTimeMillis();

                        return kafkaProducerService.sendSensorData(sensorData)
                                .thenApply(result -> {
                                    long latency = System.currentTimeMillis() - startTime;
                                    return IngestionResponse.success(
                                            sensorData.getSensorCode(),
                                            (long) result.getRecordMetadata().partition(),
                                            result.getRecordMetadata().offset(),
                                            latency
                                    );
                                })
                                .exceptionally(ex -> IngestionResponse.error(
                                        "Failed: " + ex.getMessage()
                                ));
                    })
                    .toList();

            // Attendre toutes les réponses
            List<IngestionResponse> responses = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();

            long successCount = responses.stream().filter(IngestionResponse::isSuccess).count();
            log.info("Batch ingestion completed: total={}, success={}, failed={}",
                    requests.size(), successCount, requests.size() - successCount);

            return ResponseEntity.status(HttpStatus.CREATED).body(responses);

        } catch (Exception e) {
            log.error("Failed to ingest batch: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of(IngestionResponse.error("Batch ingestion failed: " + e.getMessage())));
        }
    }

    /**
     * Obtient les statistiques du producer Kafka.
     *
     * @return statistiques
     */
    @GetMapping("/stats")
    public ResponseEntity<KafkaProducerService.ProducerStats> getStats() {
        return ResponseEntity.ok(kafkaProducerService.getStats());
    }

    /**
     * Endpoint de test simple.
     *
     * @return message de confirmation
     */
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("Ingestion service is running");
    }

    /**
     * Convertit un SensorDataRequest en SensorData.
     */
    private SensorData convertToSensorData(SensorDataRequest request) {
        return SensorData.builder()
                .timestamp(request.getTimestamp() != null ? request.getTimestamp() : Instant.now())
                .assetId(request.getAssetId())
                .sensorId(request.getSensorId())
                .sensorCode(request.getSensorCode())
                .sensorType(request.getSensorType())
                .value(request.getValue())
                .unit(request.getUnit())
                .quality(request.getQuality())
                .source("http")
                .metadata(request.getMetadata())
                .build();
    }
}
