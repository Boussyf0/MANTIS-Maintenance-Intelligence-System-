package com.mantis.ingestion.exception;

import com.mantis.ingestion.dto.IngestionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.KafkaException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire global des exceptions pour le service d'ingestion.
 *
 * Intercepte les exceptions et retourne des réponses HTTP appropriées.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Gère les erreurs de validation des requêtes.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(
            MethodArgumentNotValidException ex
    ) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation error: {}", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    /**
     * Gère les erreurs Kafka.
     */
    @ExceptionHandler(KafkaException.class)
    public ResponseEntity<IngestionResponse> handleKafkaException(KafkaException ex) {
        log.error("Kafka error: {}", ex.getMessage(), ex);

        IngestionResponse response = IngestionResponse.error(
                "Kafka service unavailable: " + ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Gère les erreurs d'argument illégal.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<IngestionResponse> handleIllegalArgumentException(
            IllegalArgumentException ex
    ) {
        log.warn("Invalid argument: {}", ex.getMessage());

        IngestionResponse response = IngestionResponse.error(
                "Invalid request: " + ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Gère les erreurs d'état illégal.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<IngestionResponse> handleIllegalStateException(
            IllegalStateException ex
    ) {
        log.error("Illegal state: {}", ex.getMessage(), ex);

        IngestionResponse response = IngestionResponse.error(
                "Service not ready: " + ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Gère toutes les autres exceptions non prévues.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<IngestionResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        IngestionResponse response = IngestionResponse.error(
                "Internal server error: " + ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
