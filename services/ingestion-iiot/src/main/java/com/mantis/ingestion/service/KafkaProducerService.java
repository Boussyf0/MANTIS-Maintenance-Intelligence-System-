package com.mantis.ingestion.service;

import com.mantis.ingestion.model.SensorData;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service de production de messages Kafka pour les données de capteurs.
 *
 * Gère l'envoi asynchrone des données vers Kafka avec métriques et gestion d'erreurs.
 */
@Slf4j
@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, SensorData> kafkaTemplate;
    private final String sensorRawTopic;
    private final EdgeBufferService edgeBufferService;

    // Métriques Prometheus
    private final Counter messagesProducedCounter;
    private final Counter messagesFailedCounter;
    private final Timer sendLatencyTimer;

    public KafkaProducerService(
            KafkaTemplate<String, SensorData> kafkaTemplate,
            @Value("${spring.kafka.topics.sensor-raw}") String sensorRawTopic,
            EdgeBufferService edgeBufferService,
            MeterRegistry meterRegistry
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.sensorRawTopic = sensorRawTopic;
        this.edgeBufferService = edgeBufferService;

        // Initialiser les métriques
        this.messagesProducedCounter = Counter.builder("mantis.kafka.messages.produced")
                .description("Total messages produced to Kafka")
                .tag("topic", sensorRawTopic)
                .register(meterRegistry);

        this.messagesFailedCounter = Counter.builder("mantis.kafka.messages.failed")
                .description("Total messages failed to produce")
                .tag("topic", sensorRawTopic)
                .register(meterRegistry);

        this.sendLatencyTimer = Timer.builder("mantis.kafka.send.latency")
                .description("Latency of Kafka send operations")
                .tag("topic", sensorRawTopic)
                .register(meterRegistry);
    }

    /**
     * Envoie des données de capteur vers Kafka de manière asynchrone.
     *
     * @param sensorData données à envoyer
     * @return CompletableFuture avec le résultat de l'envoi
     */
    public CompletableFuture<SendResult<String, SensorData>> sendSensorData(SensorData sensorData) {
        // Validation
        if (!sensorData.isValid()) {
            log.warn("Invalid sensor data, skipping: {}", sensorData);
            messagesFailedCounter.increment();
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Invalid sensor data")
            );
        }

        // Utiliser assetId comme clé pour partitionnement
        String key = sensorData.getAssetId().toString();

        // Timer pour mesurer la latence
        Timer.Sample sample = Timer.start();

        CompletableFuture<SendResult<String, SensorData>> future = kafkaTemplate.send(
                sensorRawTopic,
                key,
                sensorData
        );

        future.whenComplete((result, ex) -> {
            sample.stop(sendLatencyTimer);

            if (ex == null) {
                // Succès
                messagesProducedCounter.increment();

                if (log.isDebugEnabled()) {
                    log.debug("Sent sensor data: topic={}, partition={}, offset={}, key={}, sensorCode={}",
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset(),
                            key,
                            sensorData.getSensorCode()
                    );
                }
            } else {
                // Échec - ajouter au buffer edge si activé
                log.error("Failed to send sensor data: sensorCode={}, error={}",
                        sensorData.getSensorCode(), ex.getMessage());
                messagesFailedCounter.increment();

                // Tenter de sauvegarder dans le buffer edge
                if (edgeBufferService.isEnabled()) {
                    edgeBufferService.buffer(sensorData);
                }
            }
        });

        return future;
    }

    /**
     * Envoie synchrone (bloquant) - à utiliser avec précaution.
     *
     * @param sensorData données à envoyer
     * @return résultat de l'envoi
     * @throws Exception en cas d'erreur
     */
    public SendResult<String, SensorData> sendSensorDataSync(SensorData sensorData) throws Exception {
        String key = sensorData.getAssetId().toString();
        return kafkaTemplate.send(sensorRawTopic, key, sensorData).get();
    }

    /**
     * Obtient les statistiques du producer.
     *
     * @return statistiques sous forme de map
     */
    public ProducerStats getStats() {
        return ProducerStats.builder()
                .messagesProduced((long) messagesProducedCounter.count())
                .messagesFailed((long) messagesFailedCounter.count())
                .averageSendLatencyMs(sendLatencyTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS))
                .maxSendLatencyMs(sendLatencyTimer.max(java.util.concurrent.TimeUnit.MILLISECONDS))
                .build();
    }

    @lombok.Data
    @lombok.Builder
    public static class ProducerStats {
        private Long messagesProduced;
        private Long messagesFailed;
        private Double averageSendLatencyMs;
        private Double maxSendLatencyMs;
    }
}
