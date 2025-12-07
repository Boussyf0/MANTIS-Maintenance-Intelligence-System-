package com.mantis.ingestion.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mantis.ingestion.TestDataFactory;

import com.mantis.ingestion.dto.SensorDataRequest;
import com.mantis.ingestion.model.SensorData;
import com.mantis.ingestion.service.KafkaProducerService;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for IngestionController.
 */
@WebMvcTest(IngestionController.class)
@DisplayName("IngestionController Tests")
class IngestionControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private KafkaProducerService kafkaProducerService;

        @Test
        @DisplayName("Should ingest sensor data successfully")
        void shouldIngestSensorDataSuccessfully() throws Exception {
                // Given
                SensorDataRequest request = TestDataFactory.createValidSensorDataRequest();
                SendResult<String, SensorData> sendResult = createMockSendResult();

                when(kafkaProducerService.sendSensorDataSync(any(SensorData.class))).thenReturn(sendResult);

                // When & Then
                mockMvc.perform(post("/api/v1/ingest")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.sensorCode").value(request.getSensorCode()))
                                .andExpect(jsonPath("$.kafkaPartition").exists())
                                .andExpect(jsonPath("$.kafkaOffset").exists())
                                .andExpect(jsonPath("$.latencyMs").exists());

                verify(kafkaProducerService).sendSensorDataSync(any(SensorData.class));
        }

        @Test
        @DisplayName("Should reject invalid sensor data")
        void shouldRejectInvalidSensorData() throws Exception {
                // Given
                SensorDataRequest invalidRequest = TestDataFactory.createInvalidSensorDataRequest();

                // When & Then
                mockMvc.perform(post("/api/v1/ingest")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isBadRequest());

                verify(kafkaProducerService, never()).sendSensorDataSync(any(SensorData.class));
        }

        @Test
        @DisplayName("Should handle Kafka errors")
        void shouldHandleKafkaErrors() throws Exception {
                // Given
                SensorDataRequest request = TestDataFactory.createValidSensorDataRequest();
                when(kafkaProducerService.sendSensorDataSync(any(SensorData.class)))
                                .thenThrow(new RuntimeException("Kafka error"));

                // When & Then
                mockMvc.perform(post("/api/v1/ingest")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("Should ingest batch of sensor data")
        void shouldIngestBatchOfSensorData() throws Exception {
                // Given
                List<SensorDataRequest> requests = List.of(
                                TestDataFactory.createValidSensorDataRequest(),
                                TestDataFactory.createValidSensorDataRequest());

                SendResult<String, SensorData> sendResult = createMockSendResult();
                when(kafkaProducerService.sendSensorData(any(SensorData.class)))
                                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(sendResult));

                // When & Then
                mockMvc.perform(post("/api/v1/ingest/batch")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requests)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(2));

                verify(kafkaProducerService, times(2)).sendSensorData(any(SensorData.class));
        }

        @Test
        @DisplayName("Should return stats")
        void shouldReturnStats() throws Exception {
                // Given
                KafkaProducerService.ProducerStats stats = KafkaProducerService.ProducerStats.builder()
                                .messagesProduced(100L)
                                .messagesFailed(5L)
                                .averageSendLatencyMs(10.5)
                                .maxSendLatencyMs(50.0)
                                .build();

                when(kafkaProducerService.getStats()).thenReturn(stats);

                // When & Then
                mockMvc.perform(get("/api/v1/ingest/stats"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.messagesProduced").value(100))
                                .andExpect(jsonPath("$.messagesFailed").value(5))
                                .andExpect(jsonPath("$.averageSendLatencyMs").value(10.5))
                                .andExpect(jsonPath("$.maxSendLatencyMs").value(50.0));
        }

        @Test
        @DisplayName("Should respond to ping")
        void shouldRespondToPing() throws Exception {
                mockMvc.perform(get("/api/v1/ingest/ping"))
                                .andExpect(status().isOk())
                                .andExpect(content().string("Ingestion service is running"));
        }

        private SendResult<String, SensorData> createMockSendResult() {
                ProducerRecord<String, SensorData> producerRecord = new ProducerRecord<>(
                                "sensor.raw", 0, "test-key", TestDataFactory.createValidSensorData());

                RecordMetadata metadata = new RecordMetadata(
                                new TopicPartition("sensor.raw", 0),
                                0L, 0, 0L, 0, 0);
                return new SendResult<>(producerRecord, metadata);
        }
}
