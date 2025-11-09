package com.mantis.ingestion.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO pour les réponses d'ingestion.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionResponse {

    private boolean success;
    private String message;
    private String sensorCode;
    private Long kafkaPartition;
    private Long kafkaOffset;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    @Builder.Default
    private Instant timestamp = Instant.now();

    private Long latencyMs;

    public static IngestionResponse success(String sensorCode, Long partition, Long offset, Long latencyMs) {
        return IngestionResponse.builder()
                .success(true)
                .message("Data ingested successfully")
                .sensorCode(sensorCode)
                .kafkaPartition(partition)
                .kafkaOffset(offset)
                .latencyMs(latencyMs)
                .build();
    }

    public static IngestionResponse error(String message) {
        return IngestionResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}
