package com.mantis.ingestion.connector;

import com.mantis.ingestion.model.SensorData;
import com.mantis.ingestion.service.KafkaProducerService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.stack.client.DiscoveryClient;
import org.eclipse.milo.opcua.stack.core.AttributeId;

import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemCreateRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoringParameters;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

/**
 * Connecteur OPC UA utilisant Eclipse Milo.
 *
 * Se connecte à un serveur OPC UA et souscrit aux changements de valeurs
 * des nœuds configurés, puis publie les données vers Kafka.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "mantis.ingestion.opcua", name = "enabled", havingValue = "true")
public class OpcUaConnector {

    @Value("${mantis.ingestion.opcua.endpoint}")
    private String endpoint;

    @Value("${mantis.ingestion.opcua.subscription-interval-ms}")
    private int subscriptionIntervalMs;

    @Value("${mantis.ingestion.opcua.request-timeout-ms}")
    private long requestTimeoutMs;

    @Value("${mantis.ingestion.opcua.session-timeout-ms}")
    private long sessionTimeoutMs;

    @Value("${mantis.ingestion.opcua.max-reconnect-attempts}")
    private int maxReconnectAttempts;

    @Value("${mantis.ingestion.opcua.reconnect-delay-ms}")
    private long reconnectDelayMs;

    private final KafkaProducerService kafkaProducerService;
    private final AtomicBoolean connected = new AtomicBoolean(false);

    private OpcUaClient client;
    private UaSubscription subscription;

    // Métriques
    private final Counter dataPointsCounter;
    private final Counter connectionAttemptsCounter;
    private final Counter subscriptionErrorsCounter;

    // Map pour stocker les métadonnées des nœuds (NodeId -> asset/sensor info)
    private final Map<NodeId, NodeMetadata> nodeMetadataMap = new HashMap<>();

    public OpcUaConnector(KafkaProducerService kafkaProducerService, MeterRegistry meterRegistry) {
        this.kafkaProducerService = kafkaProducerService;

        this.dataPointsCounter = Counter.builder("mantis.opcua.data.points")
                .description("Total OPC UA data points received")
                .register(meterRegistry);

        this.connectionAttemptsCounter = Counter.builder("mantis.opcua.connection.attempts")
                .description("Total OPC UA connection attempts")
                .register(meterRegistry);

        this.subscriptionErrorsCounter = Counter.builder("mantis.opcua.subscription.errors")
                .description("Total OPC UA subscription errors")
                .register(meterRegistry);
    }

    @PostConstruct
    @CircuitBreaker(name = "opcua")
    @Retry(name = "opcua")
    public void connect() {
        log.info("Initializing OPC UA connector: endpoint={}", endpoint);

        try {
            connectionAttemptsCounter.increment();

            // Découverte des endpoints
            List<EndpointDescription> endpoints = DiscoveryClient.getEndpoints(endpoint).get();
            EndpointDescription selectedEndpoint = endpoints.stream()
                    .filter(e -> e.getSecurityPolicyUri().equals("http://opcfoundation.org/UA/SecurityPolicy#None"))
                    .findFirst()
                    .orElse(endpoints.get(0));

            log.info("Selected OPC UA endpoint: {}", selectedEndpoint.getEndpointUrl());

            // Configuration du client
            OpcUaClientConfig config = OpcUaClientConfig.builder()
                    .setApplicationName(LocalizedText.english("MANTIS Ingestion Client"))
                    .setApplicationUri("urn:mantis:ingestion:client")
                    .setEndpoint(selectedEndpoint)
                    .setIdentityProvider(new AnonymousProvider())
                    .setRequestTimeout(UInteger.valueOf(requestTimeoutMs))
                    .setSessionTimeout(UInteger.valueOf(sessionTimeoutMs))
                    .build();

            // Créer et connecter le client
            client = OpcUaClient.create(config);
            client.connect().get();

            connected.set(true);
            log.info("OPC UA client connected successfully");

            // Créer une souscription
            createSubscription();

        } catch (Exception e) {
            log.error("Failed to connect to OPC UA server: {}", e.getMessage(), e);
            connected.set(false);
            throw new RuntimeException("OPC UA connection failed", e);
        }
    }

    /**
     * Crée une souscription OPC UA pour surveiller les changements de valeurs.
     */
    private void createSubscription() throws ExecutionException, InterruptedException {
        subscription = client.getSubscriptionManager()
                .createSubscription(subscriptionIntervalMs)
                .get();

        log.info("OPC UA subscription created: interval={}ms", subscriptionIntervalMs);
    }

    /**
     * Souscrit à un nœud OPC UA.
     *
     * @param nodeId     ID du nœud OPC UA
     * @param assetId    ID de l'asset MANTIS
     * @param sensorId   ID du capteur MANTIS
     * @param sensorCode Code du capteur
     * @param sensorType Type de capteur
     * @param unit       Unité de mesure
     */
    public void subscribeToNode(
            String nodeId,
            UUID assetId,
            UUID sensorId,
            String sensorCode,
            String sensorType,
            String unit) {
        if (!connected.get()) {
            log.warn("Cannot subscribe to node {}: not connected", nodeId);
            return;
        }

        try {
            NodeId node = NodeId.parse(nodeId);

            // Stocker les métadonnées
            nodeMetadataMap.put(node, new NodeMetadata(assetId, sensorId, sensorCode, sensorType, unit));

            // Configurer la surveillance
            ReadValueId readValueId = new ReadValueId(
                    node,
                    AttributeId.Value.uid(),
                    null,
                    QualifiedName.NULL_VALUE);

            MonitoringParameters parameters = new MonitoringParameters(
                    uint(1), // clientHandle
                    (double) subscriptionIntervalMs, // samplingInterval
                    null, // filter
                    uint(10), // queueSize
                    true // discardOldest
            );

            MonitoredItemCreateRequest request = new MonitoredItemCreateRequest(
                    readValueId,
                    MonitoringMode.Reporting,
                    parameters);

            // Créer l'item surveillé
            subscription
                    .createMonitoredItems(
                            TimestampsToReturn.Both,
                            Collections.singletonList(request),
                            (item, id) -> item.setValueConsumer(this::onValueChange))
                    .get();

            log.info("Subscribed to OPC UA node: nodeId={}, sensorCode={}", nodeId, sensorCode);

        } catch (Exception e) {
            log.error("Failed to subscribe to node {}: {}", nodeId, e.getMessage(), e);
            subscriptionErrorsCounter.increment();
        }
    }

    /**
     * Callback appelé lors d'un changement de valeur d'un nœud.
     */
    private void onValueChange(UaMonitoredItem item, DataValue value) {
        try {
            NodeId nodeId = item.getReadValueId().getNodeId();
            NodeMetadata metadata = nodeMetadataMap.get(nodeId);

            if (metadata == null) {
                log.warn("No metadata found for node: {}", nodeId);
                return;
            }

            // Extraire la valeur
            Variant variant = value.getValue();
            if (variant == null || variant.getValue() == null) {
                log.warn("Null value received for node: {}", nodeId);
                return;
            }

            Object rawValue = variant.getValue();
            Double numericValue = convertToDouble(rawValue);

            if (numericValue == null) {
                log.warn("Cannot convert value to double: {} (type: {})",
                        rawValue, rawValue.getClass().getSimpleName());
                return;
            }

            // Calculer la qualité basée sur le status code OPC UA
            int quality = value.getStatusCode().isGood() ? 100 : 0;

            // Créer l'objet SensorData
            SensorData sensorData = SensorData.builder()
                    .timestamp(value.getSourceTime() != null ? Instant.ofEpochMilli(value.getSourceTime().getJavaTime())
                            : Instant.now())
                    .assetId(metadata.assetId)
                    .sensorId(metadata.sensorId)
                    .sensorCode(metadata.sensorCode)
                    .sensorType(metadata.sensorType)
                    .value(numericValue)
                    .unit(metadata.unit)
                    .quality(quality)
                    .source("opcua")
                    .metadata(Map.of(
                            "nodeId", nodeId.toParseableString(),
                            "serverTimestamp", value.getServerTime() != null ? value.getServerTime().getJavaTime() : 0))
                    .build();

            // Envoyer vers Kafka
            kafkaProducerService.sendSensorData(sensorData);
            dataPointsCounter.increment();

            log.debug("OPC UA data published: sensorCode={}, value={}", metadata.sensorCode, numericValue);

        } catch (Exception e) {
            log.error("Error processing OPC UA value change: {}", e.getMessage(), e);
            subscriptionErrorsCounter.increment();
        }
    }

    /**
     * Convertit une valeur OPC UA en Double.
     */
    private Double convertToDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        } else if (value instanceof Boolean) {
            return ((Boolean) value) ? 1.0 : 0.0;
        }
        return null;
    }

    /**
     * Vérifie si le connecteur est connecté.
     */
    public boolean isConnected() {
        return connected.get() && client != null;
    }

    /**
     * Lit la valeur actuelle d'un nœud OPC UA.
     */
    public CompletableFuture<DataValue> readNode(String nodeId) {
        if (!connected.get()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("OPC UA client not connected"));
        }

        NodeId node = NodeId.parse(nodeId);
        return client.readValue(0, TimestampsToReturn.Both, node);
    }

    @PreDestroy
    public void disconnect() {
        if (client != null && connected.get()) {
            try {
                log.info("Disconnecting OPC UA client...");
                client.disconnect().get();
                connected.set(false);
                log.info("OPC UA client disconnected");
            } catch (Exception e) {
                log.error("Error disconnecting OPC UA client: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Classe interne pour stocker les métadonnées d'un nœud.
     */
    private static class NodeMetadata {
        final UUID assetId;
        final UUID sensorId;
        final String sensorCode;
        final String sensorType;
        final String unit;

        NodeMetadata(UUID assetId, UUID sensorId, String sensorCode, String sensorType, String unit) {
            this.assetId = assetId;
            this.sensorId = sensorId;
            this.sensorCode = sensorCode;
            this.sensorType = sensorType;
            this.unit = unit;
        }
    }
}
