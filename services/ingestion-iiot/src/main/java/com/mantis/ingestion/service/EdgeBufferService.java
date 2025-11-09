package com.mantis.ingestion.service;

import com.mantis.ingestion.model.SensorData;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service de buffer edge pour stocker temporairement les données
 * en cas de défaillance de Kafka.
 *
 * Permet de ne pas perdre de données lors de coupures réseau ou
 * d'indisponibilité temporaire de Kafka.
 */
@Slf4j
@Service
public class EdgeBufferService {

    @Value("${mantis.ingestion.edge-buffer.enabled}")
    private boolean enabled;

    @Value("${mantis.ingestion.edge-buffer.max-size}")
    private int maxSize;

    @Value("${mantis.ingestion.edge-buffer.flush-interval-ms}")
    private long flushIntervalMs;

    private final BlockingQueue<SensorData> buffer;
    private final AtomicLong bufferedCount = new AtomicLong(0);
    private final AtomicLong droppedCount = new AtomicLong(0);

    private final Counter bufferAddedCounter;
    private final Counter bufferDroppedCounter;
    private final Gauge bufferSizeGauge;

    public EdgeBufferService(MeterRegistry meterRegistry) {
        this.buffer = new LinkedBlockingQueue<>();

        // Métriques
        this.bufferAddedCounter = Counter.builder("mantis.edge.buffer.added")
                .description("Total items added to edge buffer")
                .register(meterRegistry);

        this.bufferDroppedCounter = Counter.builder("mantis.edge.buffer.dropped")
                .description("Total items dropped (buffer full)")
                .register(meterRegistry);

        this.bufferSizeGauge = Gauge.builder("mantis.edge.buffer.size", buffer, BlockingQueue::size)
                .description("Current edge buffer size")
                .register(meterRegistry);
    }

    @PostConstruct
    public void init() {
        if (enabled) {
            log.info("Edge buffer service enabled: maxSize={}, flushInterval={}ms",
                    maxSize, flushIntervalMs);
        } else {
            log.info("Edge buffer service disabled");
        }
    }

    /**
     * Ajoute une donnée au buffer.
     *
     * @param sensorData donnée à buffer
     * @return true si ajoutée avec succès, false si buffer plein
     */
    public boolean buffer(SensorData sensorData) {
        if (!enabled) {
            return false;
        }

        if (buffer.size() >= maxSize) {
            log.warn("Edge buffer full ({}), dropping data: sensorCode={}",
                    maxSize, sensorData.getSensorCode());
            droppedCount.incrementAndGet();
            bufferDroppedCounter.increment();
            return false;
        }

        boolean added = buffer.offer(sensorData);
        if (added) {
            bufferedCount.incrementAndGet();
            bufferAddedCounter.increment();
            log.debug("Added to edge buffer: sensorCode={}, bufferSize={}",
                    sensorData.getSensorCode(), buffer.size());
        }

        return added;
    }

    /**
     * Récupère toutes les données du buffer.
     *
     * @return liste des données
     */
    public BlockingQueue<SensorData> getBufferedData() {
        return buffer;
    }

    /**
     * Vide le buffer.
     */
    public void clear() {
        int size = buffer.size();
        buffer.clear();
        log.info("Edge buffer cleared: {} items removed", size);
    }

    /**
     * Retourne la taille actuelle du buffer.
     */
    public int size() {
        return buffer.size();
    }

    /**
     * Vérifie si le buffer est activé.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Retourne les statistiques du buffer.
     */
    public BufferStats getStats() {
        return BufferStats.builder()
                .enabled(enabled)
                .currentSize(buffer.size())
                .maxSize(maxSize)
                .totalBuffered(bufferedCount.get())
                .totalDropped(droppedCount.get())
                .build();
    }

    /**
     * Tâche planifiée pour logger les statistiques du buffer.
     */
    @Scheduled(fixedDelayString = "${mantis.ingestion.edge-buffer.flush-interval-ms}")
    public void logStats() {
        if (enabled && buffer.size() > 0) {
            log.info("Edge buffer stats: size={}, buffered={}, dropped={}",
                    buffer.size(), bufferedCount.get(), droppedCount.get());
        }
    }

    @PreDestroy
    public void shutdown() {
        if (enabled && buffer.size() > 0) {
            log.warn("Shutting down with {} items in edge buffer - data will be lost",
                    buffer.size());
        }
    }

    @lombok.Data
    @lombok.Builder
    public static class BufferStats {
        private boolean enabled;
        private int currentSize;
        private int maxSize;
        private long totalBuffered;
        private long totalDropped;
    }
}
