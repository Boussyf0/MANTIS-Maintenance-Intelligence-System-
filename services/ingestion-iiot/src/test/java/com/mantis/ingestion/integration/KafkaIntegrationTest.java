package com.mantis.ingestion.integration;

import com.mantis.ingestion.TestDataFactory;
import com.mantis.ingestion.model.SensorData;
import com.mantis.ingestion.service.KafkaProducerService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Kafka producer using Testcontainers.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Kafka Integration Tests")
class KafkaIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );

    @Autowired
    private KafkaProducerService kafkaProducerService;

    private KafkaConsumer<String, SensorData> testConsumer;
    private static final String TEST_TOPIC = "sensor.raw.test";

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.topics.sensor-raw", () -> TEST_TOPIC);
    }

    @BeforeEach
    void setUp() {
        // Create test consumer
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, SensorData.class);

        testConsumer = new KafkaConsumer<>(props);
        testConsumer.subscribe(Collections.singletonList(TEST_TOPIC));
    }

    @AfterEach
    void tearDown() {
        if (testConsumer != null) {
            testConsumer.close();
        }
    }

    @Test
    @DisplayName("Should publish sensor data to Kafka successfully")
    void shouldPublishSensorDataToKafka() throws Exception {
        // Given
        SensorData sensorData = TestDataFactory.createValidSensorData();

        // When
        kafkaProducerService.sendSensorDataSync(sensorData);

        // Then - Poll for the message
        ConsumerRecords<String, SensorData> records = testConsumer.poll(Duration.ofSeconds(10));

        assertThat(records).isNotEmpty();
        ConsumerRecord<String, SensorData> record = records.iterator().next();

        assertThat(record.value().getSensorCode()).isEqualTo(sensorData.getSensorCode());
        assertThat(record.value().getValue()).isEqualTo(sensorData.getValue());
        assertThat(record.key()).isEqualTo(sensorData.getAssetId().toString());
    }

    @Test
    @DisplayName("Should handle multiple messages")
    void shouldHandleMultipleMessages() throws Exception {
        // Given
        int messageCount = 5;

        // When
        for (int i = 0; i < messageCount; i++) {
            SensorData sensorData = TestDataFactory.createSensorData("SENSOR-" + i, 25.0 + i);
            kafkaProducerService.sendSensorDataSync(sensorData);
        }

        // Then
        ConsumerRecords<String, SensorData> records = testConsumer.poll(Duration.ofSeconds(10));

        assertThat(records.count()).isEqualTo(messageCount);
    }

    @Test
    @DisplayName("Should partition by assetId")
    void shouldPartitionByAssetId() throws Exception {
        // Given
        SensorData sensorData = TestDataFactory.createValidSensorData();

        // When
        kafkaProducerService.sendSensorDataSync(sensorData);

        // Then
        ConsumerRecords<String, SensorData> records = testConsumer.poll(Duration.ofSeconds(10));

        assertThat(records).isNotEmpty();
        ConsumerRecord<String, SensorData> record = records.iterator().next();

        // Key should be assetId for partitioning
        assertThat(record.key()).isEqualTo(sensorData.getAssetId().toString());
    }
}
