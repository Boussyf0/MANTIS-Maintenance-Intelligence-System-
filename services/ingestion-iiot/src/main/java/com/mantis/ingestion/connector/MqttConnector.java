package com.mantis.ingestion.connector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mantis.ingestion.model.SensorData;
import com.mantis.ingestion.service.KafkaProducerService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Connecteur MQTT utilisant Eclipse Paho.
 *
 * Se connecte à un broker MQTT et souscrit aux topics configurés,
 * puis publie les données reçues vers Kafka.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "mantis.ingestion.mqtt", name = "enabled", havingValue = "true")
public class MqttConnector implements MqttCallback {

    @Value("${mantis.ingestion.mqtt.broker-url}")
    private String brokerUrl;

    @Value("${mantis.ingestion.mqtt.client-id}")
    private String clientId;

    @Value("${mantis.ingestion.mqtt.username:#{null}}")
    private String username;

    @Value("${mantis.ingestion.mqtt.password:#{null}}")
    private String password;

    @Value("${mantis.ingestion.mqtt.topic-prefix}")
    private String topicPrefix;

    @Value("${mantis.ingestion.mqtt.qos}")
    private int qos;

    @Value("${mantis.ingestion.mqtt.clean-session}")
    private boolean cleanSession;

    @Value("${mantis.ingestion.mqtt.connection-timeout}")
    private int connectionTimeout;

    @Value("${mantis.ingestion.mqtt.keep-alive-interval}")
    private int keepAliveInterval;

    @Value("${mantis.ingestion.mqtt.max-reconnect-attempts}")
    private int maxReconnectAttempts;

    @Value("${mantis.ingestion.mqtt.reconnect-delay-ms}")
    private long reconnectDelayMs;

    private final KafkaProducerService kafkaProducerService;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean connected = new AtomicBoolean(false);

    private MqttClient client;

    // Métriques
    private final Counter messagesReceivedCounter;
    private final Counter connectionAttemptsCounter;
    private final Counter connectionFailuresCounter;
    private final Counter messageErrorsCounter;

    public MqttConnector(
            KafkaProducerService kafkaProducerService,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.kafkaProducerService = kafkaProducerService;
        this.objectMapper = objectMapper;

        this.messagesReceivedCounter = Counter.builder("mantis.mqtt.messages.received")
                .description("Total MQTT messages received")
                .register(meterRegistry);

        this.connectionAttemptsCounter = Counter.builder("mantis.mqtt.connection.attempts")
                .description("Total MQTT connection attempts")
                .register(meterRegistry);

        this.connectionFailuresCounter = Counter.builder("mantis.mqtt.connection.failures")
                .description("Total MQTT connection failures")
                .register(meterRegistry);

        this.messageErrorsCounter = Counter.builder("mantis.mqtt.message.errors")
                .description("Total MQTT message processing errors")
                .register(meterRegistry);
    }

    @PostConstruct
    public void connect() {
        log.info("Initializing MQTT connector: broker={}, clientId={}, topic={}",
                brokerUrl, clientId, topicPrefix);

        try {
            connectionAttemptsCounter.increment();

            // Créer le client MQTT
            client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());

            // Options de connexion
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(cleanSession);
            options.setConnectionTimeout(connectionTimeout);
            options.setKeepAliveInterval(keepAliveInterval);
            options.setAutomaticReconnect(true);
            options.setMaxReconnectDelay((int) reconnectDelayMs);

            // Authentification si nécessaire
            if (username != null && !username.isEmpty()) {
                options.setUserName(username);
                if (password != null) {
                    options.setPassword(password.toCharArray());
                }
            }

            // Callback pour les événements
            client.setCallback(this);

            // Se connecter
            client.connect(options);
            connected.set(true);

            log.info("MQTT client connected successfully");

            // Souscrire au topic
            subscribe();

        } catch (MqttException e) {
            log.error("Failed to connect to MQTT broker: {}", e.getMessage(), e);
            connectionFailuresCounter.increment();
            connected.set(false);
            throw new RuntimeException("MQTT connection failed", e);
        }
    }

    /**
     * Souscrit au topic MQTT configuré.
     */
    private void subscribe() {
        try {
            client.subscribe(topicPrefix, qos);
            log.info("Subscribed to MQTT topic: topic={}, qos={}", topicPrefix, qos);
        } catch (MqttException e) {
            log.error("Failed to subscribe to MQTT topic {}: {}", topicPrefix, e.getMessage(), e);
            throw new RuntimeException("MQTT subscription failed", e);
        }
    }

    /**
     * Callback appelé lors de la perte de connexion.
     */
    @Override
    public void connectionLost(Throwable cause) {
        log.warn("MQTT connection lost: {}", cause.getMessage());
        connected.set(false);
        connectionFailuresCounter.increment();

        // Le client tentera de se reconnecter automatiquement grâce à
        // automaticReconnect=true
        log.info("MQTT client will attempt to reconnect automatically");
    }

    /**
     * Callback appelé lors de la réception d'un message MQTT.
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) {
        try {
            messagesReceivedCounter.increment();

            String payload = new String(message.getPayload());
            log.debug("MQTT message received: topic={}, payload={}", topic, payload);

            // Parser le message JSON
            SensorData sensorData = parseMqttMessage(topic, payload);

            if (sensorData == null) {
                log.warn("Failed to parse MQTT message from topic: {}", topic);
                messageErrorsCounter.increment();
                return;
            }

            // Ajouter les métadonnées MQTT
            if (sensorData.getMetadata() == null) {
                sensorData.setMetadata(Map.of());
            }
            sensorData.getMetadata().put("mqttTopic", topic);
            sensorData.getMetadata().put("mqttQos", message.getQos());
            sensorData.getMetadata().put("mqttRetained", message.isRetained());

            // Envoyer vers Kafka
            kafkaProducerService.sendSensorData(sensorData);

            log.debug("MQTT data published: topic={}, sensorCode={}",
                    topic, sensorData.getSensorCode());

        } catch (Exception e) {
            log.error("Error processing MQTT message from topic {}: {}",
                    topic, e.getMessage(), e);
            messageErrorsCounter.increment();
        }
    }

    /**
     * Parse un message MQTT en objet SensorData.
     *
     * Formats supportés:
     * 1. JSON complet (tous les champs)
     * 2. JSON simple: {"value": 25.5, "timestamp": "2024-...", ...}
     * 3. Valeur simple: "25.5"
     */
    private SensorData parseMqttMessage(String topic, String payload) {
        try {
            // Essayer de parser comme JSON complet
            if (payload.trim().startsWith("{")) {
                Map<String, Object> jsonMap = objectMapper.readValue(payload,
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                        });

                // Vérifier si c'est un objet SensorData complet
                if (jsonMap.containsKey("sensorCode") && jsonMap.containsKey("value")) {
                    return objectMapper.convertValue(jsonMap, SensorData.class);
                }

                // Sinon, construire un SensorData à partir des champs disponibles
                return buildSensorDataFromJson(topic, jsonMap);
            }

            // Sinon, traiter comme valeur simple
            Double value = Double.parseDouble(payload.trim());
            return buildSensorDataFromValue(topic, value);

        } catch (Exception e) {
            log.warn("Failed to parse MQTT payload as JSON or number: {}", payload, e);
            return null;
        }
    }

    /**
     * Construit un SensorData à partir d'un objet JSON partiel.
     */
    private SensorData buildSensorDataFromJson(String topic, Map<String, Object> jsonMap) {
        // Extraire les informations du topic (ex:
        // factory/sensors/MOTOR-001/temperature)
        String[] topicParts = topic.split("/");
        String sensorCode = topicParts.length > 2 ? topicParts[topicParts.length - 2] : "UNKNOWN";
        String sensorType = topicParts.length > 1 ? topicParts[topicParts.length - 1] : "unknown";

        Double value = jsonMap.get("value") != null ? ((Number) jsonMap.get("value")).doubleValue() : null;

        String timestampStr = (String) jsonMap.get("timestamp");
        Instant timestamp = timestampStr != null ? Instant.parse(timestampStr) : Instant.now();

        return SensorData.builder()
                .timestamp(timestamp)
                .assetId(jsonMap.containsKey("assetId") ? UUID.fromString((String) jsonMap.get("assetId"))
                        : UUID.randomUUID())
                .sensorId(jsonMap.containsKey("sensorId") ? UUID.fromString((String) jsonMap.get("sensorId"))
                        : UUID.randomUUID())
                .sensorCode(jsonMap.containsKey("sensorCode") ? (String) jsonMap.get("sensorCode") : sensorCode)
                .sensorType(jsonMap.containsKey("sensorType") ? (String) jsonMap.get("sensorType") : sensorType)
                .value(value)
                .unit(jsonMap.containsKey("unit") ? (String) jsonMap.get("unit") : "")
                .quality(jsonMap.containsKey("quality") ? ((Number) jsonMap.get("quality")).intValue() : 100)
                .source("mqtt")
                .build();
    }

    /**
     * Construit un SensorData à partir d'une valeur simple.
     */
    private SensorData buildSensorDataFromValue(String topic, Double value) {
        String[] topicParts = topic.split("/");
        String sensorCode = topicParts.length > 2 ? topicParts[topicParts.length - 2] : "UNKNOWN";
        String sensorType = topicParts.length > 1 ? topicParts[topicParts.length - 1] : "unknown";

        return SensorData.builder()
                .timestamp(Instant.now())
                .assetId(UUID.randomUUID())
                .sensorId(UUID.randomUUID())
                .sensorCode(sensorCode)
                .sensorType(sensorType)
                .value(value)
                .unit("")
                .quality(100)
                .source("mqtt")
                .build();
    }

    /**
     * Callback appelé lorsque la livraison d'un message est terminée.
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Non utilisé pour un subscriber
    }

    /**
     * Publie un message MQTT (utile pour les tests ou la communication
     * bidirectionnelle).
     */
    public void publish(String topic, String payload, int qos, boolean retained) throws MqttException {
        if (!connected.get()) {
            throw new IllegalStateException("MQTT client not connected");
        }

        MqttMessage message = new MqttMessage(payload.getBytes());
        message.setQos(qos);
        message.setRetained(retained);

        client.publish(topic, message);
        log.debug("Published MQTT message: topic={}, payload={}", topic, payload);
    }

    /**
     * Vérifie si le connecteur est connecté.
     */
    public boolean isConnected() {
        return connected.get() && client != null && client.isConnected();
    }

    @PreDestroy
    public void disconnect() {
        if (client != null && connected.get()) {
            try {
                log.info("Disconnecting MQTT client...");
                client.disconnect();
                client.close();
                connected.set(false);
                log.info("MQTT client disconnected");
            } catch (MqttException e) {
                log.error("Error disconnecting MQTT client: {}", e.getMessage(), e);
            }
        }
    }
}
