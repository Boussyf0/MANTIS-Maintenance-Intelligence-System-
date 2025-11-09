package com.mantis.ingestion.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * DTO pour les requêtes HTTP d'ingestion de données de capteurs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorDataRequest {

    @NotNull(message = "Asset ID is required")
    private UUID assetId;

    @NotNull(message = "Sensor ID is required")
    private UUID sensorId;

    @NotBlank(message = "Sensor code is required")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Sensor code must contain only uppercase letters, numbers, hyphens and underscores")
    private String sensorCode;

    @NotBlank(message = "Sensor type is required")
    private String sensorType;

    @NotNull(message = "Value is required")
    @DecimalMin(value = "-999999.99", message = "Value too small")
    @DecimalMax(value = "999999.99", message = "Value too large")
    private Double value;

    @NotBlank(message = "Unit is required")
    private String unit;

    @Min(value = 0, message = "Quality must be between 0 and 100")
    @Max(value = 100, message = "Quality must be between 0 and 100")
    @Builder.Default
    private Integer quality = 100;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant timestamp;

    private Map<String, Object> metadata;
}
