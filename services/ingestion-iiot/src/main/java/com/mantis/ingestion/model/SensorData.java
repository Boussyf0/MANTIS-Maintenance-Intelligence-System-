package com.mantis.ingestion.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Modèle de données pour les lectures de capteurs.
 *
 * Représente une mesure unique provenant d'un capteur industriel,
 * avec métadonnées pour traçabilité et qualité.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SensorData {

    /**
     * Timestamp de la mesure (UTC).
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant timestamp;

    /**
     * Identifiant unique de l'asset (équipement).
     */
    private UUID assetId;

    /**
     * Identifiant unique du capteur.
     */
    private UUID sensorId;

    /**
     * Code du capteur (ex: "MOTOR-001_TEMP").
     */
    private String sensorCode;

    /**
     * Type de capteur (ex: "temperature", "vibration", "pressure").
     */
    private String sensorType;

    /**
     * Valeur mesurée.
     */
    private Double value;

    /**
     * Unité de mesure (ex: "°C", "mm/s", "bar").
     */
    private String unit;

    /**
     * Qualité du signal (0-100).
     * 100 = excellente qualité, 0 = mauvaise qualité.
     */
    @Builder.Default
    private Integer quality = 100;

    /**
     * Source de la donnée (ex: "opcua", "mqtt", "modbus").
     */
    private String source;

    /**
     * Métadonnées additionnelles (format libre JSON).
     */
    private Map<String, Object> metadata;

    /**
     * Timestamp de création du message (pour calcul de latence).
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    @Builder.Default
    private Instant createdAt = Instant.now();

    /**
     * Vérifie si la donnée est valide.
     *
     * @return true si tous les champs obligatoires sont présents et valides
     */
    public boolean isValid() {
        return timestamp != null
                && assetId != null
                && sensorId != null
                && sensorCode != null && !sensorCode.isBlank()
                && sensorType != null && !sensorType.isBlank()
                && value != null && !value.isNaN() && !value.isInfinite()
                && quality != null && quality >= 0 && quality <= 100;
    }

    /**
     * Calcule la latence entre la mesure et la création du message.
     *
     * @return latence en millisecondes
     */
    public long getLatencyMs() {
        if (timestamp == null || createdAt == null) {
            return 0;
        }
        return createdAt.toEpochMilli() - timestamp.toEpochMilli();
    }
}
