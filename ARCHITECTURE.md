# Architecture MANTIS - Documentation technique

## Vue d'ensemble

MANTIS (MAiNtenance prédictive Temps-réel pour usines Intelligentes) est une plateforme microservices modulaire pour la maintenance prédictive industrielle.

## Architecture globale

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Usine / Factory Floor                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐            │
│  │ PLC/SCADA│  │ Moteurs  │  │ Pompes   │  │   CNC    │            │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘            │
│       │ OPC UA      │ MQTT        │ Modbus      │                   │
└───────┼─────────────┼─────────────┼─────────────┼───────────────────┘
        │             │             │             │
        └─────────────┴─────────────┴─────────────┘
                          │
        ┌─────────────────▼──────────────────┐
        │   1. INGESTION IIoT SERVICE        │
        │   - OPC UA Connector               │
        │   - MQTT Connector                 │
        │   - Modbus TCP Connector           │
        │   - Edge Buffer (resilience)       │
        └─────────────┬──────────────────────┘
                      │ Kafka (sensor.raw)
        ┌─────────────▼──────────────────────┐
        │   2. PREPROCESSING SERVICE         │
        │   - Cleaning & Alignment           │
        │   - Resampling & Denoising         │
        │   - Windowing (sliding windows)    │
        │   - Imputation                     │
        └─────────────┬──────────────────────┘
                      │ Kafka (sensor.preprocessed)
        ┌─────────────▼──────────────────────┐
        │   3. FEATURE EXTRACTION SERVICE    │
        │   - Time domain (RMS, kurtosis...)│
        │   - Frequency domain (FFT, STFT)  │
        │   - Wavelets                       │
        │   - Feature Store (Feast)          │
        └─────────────┬──────────────────────┘
                      │ Kafka (features.computed)
        ┌─────────────▼──────────────────────┐
        │   4. ANOMALY DETECTION SERVICE     │
        │   - Isolation Forest               │
        │   - One-Class SVM                  │
        │   - Autoencoders (PyTorch)         │
        │   - Adaptive thresholds            │
        └─────────────┬──────────────────────┘
                      │ Kafka (anomalies.detected)
        ┌─────────────▼──────────────────────┐
        │   5. RUL PREDICTION SERVICE        │
        │   - LSTM/GRU/TCN models            │
        │   - Transfer learning (C-MAPSS)    │
        │   - Uncertainty quantification     │
        │   - MLflow tracking                │
        └─────────────┬──────────────────────┘
                      │ Kafka (rul.predictions)
        ┌─────────────▼──────────────────────┐
        │   6. MAINTENANCE ORCHESTRATOR      │
        │   - Business rules (Drools)        │
        │   - Optimization (OR-Tools)        │
        │   - Work order generation          │
        │   - Inventory management           │
        └─────────────┬──────────────────────┘
                      │ REST API
        ┌─────────────▼──────────────────────┐
        │   7. DASHBOARD USINE (React)       │
        │   - Real-time visualizations       │
        │   - Asset health monitoring        │
        │   - Alerts & recommendations       │
        │   - KPIs (MTBF, MTTR, OEE)        │
        └────────────────────────────────────┘
```

## Infrastructure de données

### Kafka Topics

| Topic | Description | Producteur | Consommateur |
|-------|-------------|------------|--------------|
| `sensor.raw` | Données brutes capteurs | Ingestion IIoT | Preprocessing |
| `sensor.preprocessed` | Données nettoyées | Preprocessing | Feature Extraction |
| `features.computed` | Features calculées | Feature Extraction | Anomaly Detection, RUL Prediction |
| `anomalies.detected` | Anomalies détectées | Anomaly Detection | Orchestrator |
| `rul.predictions` | Prédictions RUL | RUL Prediction | Orchestrator |
| `maintenance.actions` | Actions de maintenance | Orchestrator | Dashboard, CMMS |

### Bases de données

#### PostgreSQL (Métadonnées)
- **Tables principales**:
  - `assets` - Équipements et machines
  - `sensors` - Configuration capteurs
  - `spare_parts` - Pièces de rechange
  - `work_orders` - Ordres de travail
  - `anomalies` - Journal d'anomalies
  - `rul_predictions` - Historique prédictions RUL
  - `maintenance_rules` - Règles métier
  - `ml_models` - Registry modèles ML

#### TimescaleDB (Séries temporelles)
- **Hypertables**:
  - `sensor_data_raw` - Données brutes (rétention 90j, compression 7j)
  - `sensor_data_windowed` - Données fenêtrées (rétention 180j)
  - `sensor_features` - Features (rétention 1 an)
  - `anomaly_scores` - Scores anomalies (rétention 180j)
  - `rul_predictions_ts` - Time series RUL (rétention 1 an)
  - `system_events` - Événements système (rétention 90j)

- **Vues continues**:
  - `sensor_data_hourly` - Agrégation horaire
  - `sensor_data_daily` - Agrégation quotidienne
  - `anomalies_hourly` - Compteurs anomalies

#### InfluxDB (Haute fréquence)
- Métriques haute fréquence (>100Hz)
- Données vibratoires brutes
- Rétention: 30 jours

#### MinIO (Object Storage)
- `raw-data/` - Données brutes archivées (Parquet)
- `processed-data/` - Données traitées
- `models/` - Artifacts modèles ML
- `mlflow/` - Artifacts MLflow
- `feast/` - Registry Feature Store

#### Redis
- Feature Store online (Feast)
- Cache requêtes fréquentes
- Session management

## Services détaillés

### 1. Ingestion IIoT Service

**Port**: 8001
**Langage**: Python 3.11
**Framework**: FastAPI

**Responsabilités**:
- Collecte données depuis protocoles industriels (OPC UA, MQTT, Modbus)
- Normalisation métadonnées (timestamps UTC, unités)
- Edge buffering en cas de panne réseau
- Publication vers Kafka

**Connecteurs**:
- **OPC UA**: Souscription à nodes avec callback asynchrone
- **MQTT**: Client Paho MQTT avec reconnexion automatique
- **Modbus TCP**: Polling périodique de registres

**APIs principales**:
```
POST /opcua/subscribe        # Souscrire à nodes OPC UA
POST /mqtt/subscribe         # Souscrire à topics MQTT
GET  /status                 # État connecteurs
GET  /metrics                # Métriques Prometheus
```

**Métriques**:
- `mantis_ingestion_messages_total{source, sensor_type}`
- `mantis_ingestion_errors_total{source, error_type}`
- `mantis_ingestion_active_connections{source}`
- `mantis_ingestion_latency_seconds{source}`

### 2. Preprocessing Service

**Port**: 8002
**Langage**: Python 3.11
**Framework**: FastAPI + Kafka Streams (Faust)

**Responsabilités**:
- Nettoyage données (outliers, missing values)
- Rééchantillage et alignement temporel
- Débruitage (filtres passe-bande, Savitzky-Golay)
- Fenêtrage glissant pour ML
- Synchronisation multi-capteurs

**Traitements**:
1. **Quality Check**: Validation ranges, timestamps
2. **Resampling**: Interpolation/downsampling
3. **Denoising**: Filtres Butterworth, médian
4. **Windowing**: Fenêtres glissantes (overlap 50%)
5. **Normalization**: Z-score, MinMax par asset type

**Configuration**:
```yaml
window_size: 512       # samples
overlap: 0.5           # 50%
sampling_rate: 1000    # Hz (target)
filters:
  - type: butterworth
    order: 5
    lowcut: 1
    highcut: 500
```

### 3. Feature Extraction Service

**Port**: 8003
**Langage**: Python 3.11
**Technologies**: tsfresh, PyWavelets, SciPy

**Features temps**:
- RMS, Mean, Std, Variance
- Kurtosis, Skewness
- Peak-to-peak, Crest factor
- Zero crossing rate

**Features fréquence**:
- FFT spectrum
- STFT (Short-Time Fourier Transform)
- Spectral centroids, rolloff, flux
- Power spectral density
- Order tracking (pour moteurs)
- Envelope analysis

**Features ondelettes**:
- Décomposition multi-résolution
- Coefficients détails/approximation
- Énergie par bande

**Feature Store (Feast)**:
```python
# Exemple de feature definition
@feast.feature_view(
    entities=["asset"],
    ttl=timedelta(days=365),
    source=kafka_source
)
def vibration_features(df: pd.DataFrame):
    return df[['rms', 'kurtosis', 'crest_factor', 'spectral_centroid']]
```

### 4. Anomaly Detection Service

**Port**: 8004
**Langage**: Python 3.11
**Technologies**: PyOD, PyTorch, scikit-learn

**Modèles**:
- **Isolation Forest**: Détection outliers statistiques
- **One-Class SVM**: Apprentissage frontière normale
- **Autoencoder**: Deep learning (reconstruction error)
- **LOF**: Local Outlier Factor
- **Ensemble**: Vote majoritaire

**Pipeline**:
1. Chargement features depuis Feast
2. Inférence multi-modèles
3. Agrégation scores (weighted average)
4. Seuils adaptatifs par criticité asset
5. Génération événements anomalies

**Configuration seuils**:
```python
thresholds = {
    'critical': 0.7,   # Assets critiques
    'high': 0.8,
    'medium': 0.85,
    'low': 0.9
}
```

### 5. RUL Prediction Service

**Port**: 8005
**Langage**: Python 3.11
**Technologies**: PyTorch, XGBoost, MLflow

**Modèles**:
- **LSTM**: Séquences temporelles
- **GRU**: Variant LSTM plus léger
- **TCN**: Temporal Convolutional Networks
- **XGBoost**: Baseline sur features agrégées

**Transfer Learning**:
1. Pré-entraînement sur NASA C-MAPSS (4 datasets)
2. Fine-tuning sur données usine
3. Domain adaptation

**Sorties**:
- RUL point estimate (heures)
- Intervalle de confiance (quantiles 0.05, 0.95)
- Probabilité défaillance à 24h, 7j, 30j
- Health index (0-100)

**MLflow tracking**:
```python
with mlflow.start_run():
    mlflow.log_params({"model": "lstm", "layers": 3})
    mlflow.log_metrics({"rmse": 12.5, "mae": 8.3})
    mlflow.pytorch.log_model(model, "rul_model")
```

### 6. Maintenance Orchestrator

**Port**: 8006
**Langage**: Python 3.11
**Technologies**: Drools (règles), OR-Tools (optimisation)

**Règles métier** (exemples):
```drools
rule "RUL Critique"
when
    $asset: Asset(rul < 120)  # < 120h
    $part: SparePart(stock == 0)
then
    createPurchaseOrder($part);
    scheduleInspection($asset, within="24h");
end

rule "Anomalie Persistante"
when
    $asset: Asset()
    Number(intValue >= 3) from accumulate(
        Anomaly(asset == $asset, timestamp > now-24h),
        count(1)
    )
then
    createWorkOrder($asset, priority="high");
end
```

**Optimisation planning**:
- Contraintes: Fenêtres d'arrêt, main-d'œuvre, pièces
- Objectif: Minimiser downtime + coût
- Solveur: OR-Tools CP-SAT

### 7. Dashboard Usine

**Port**: 3000
**Stack**: React.js, Next.js, TailwindCSS
**Visualisation**: Plotly.js, Recharts, D3.js
**Temps-réel**: WebSockets, Server-Sent Events

**Vues principales**:

1. **Overview**:
   - Heatmap criticité assets
   - KPIs globaux (OEE, MTBF, MTTR)
   - Alertes temps-réel

2. **Asset Detail**:
   - Graphes tendances (RUL, health index)
   - Spectres vibrations
   - Historique maintenance

3. **Anomalies**:
   - Liste anomalies non-acknowledged
   - Drill-down features contributives (SHAP)

4. **Maintenance**:
   - Backlog work orders
   - Planning Gantt
   - Inventaire pièces

5. **Analytics**:
   - Courbes ROI (downtime évité)
   - Benchmarks par ligne/actif
   - Rapports PDF/CSV

## Déploiement

### Docker Compose (Development)

```bash
# Lancer infrastructure
cd infrastructure/docker
docker-compose -f docker-compose.infrastructure.yml up -d

# Lancer services
docker-compose -f docker-compose.services.yml up -d

# Vérifier santé
docker-compose ps
```

### Kubernetes (Production)

```bash
# Appliquer manifests
kubectl apply -f infrastructure/kubernetes/namespace.yaml
kubectl apply -f infrastructure/kubernetes/configmaps/
kubectl apply -f infrastructure/kubernetes/deployments/
kubectl apply -f infrastructure/kubernetes/services/

# Vérifier
kubectl get pods -n mantis
kubectl get svc -n mantis
```

**Scaling**:
- HPA (Horizontal Pod Autoscaler) sur CPU/mémoire
- KEDA pour scaling Kafka consumer lag

## Monitoring & Observabilité

### Prometheus + Grafana
- Dashboards par service
- Alerting (PagerDuty, Slack)

### Jaeger (Distributed Tracing)
- Trace complète: Ingestion → Prédiction → Action

### Logs (ELK Stack)
- Logstash pour agrégation
- Elasticsearch pour indexation
- Kibana pour visualisation

## Sécurité

- **Authentification**: OAuth2/OIDC (Keycloak)
- **Autorisation**: RBAC (Role-Based Access Control)
- **Secrets**: HashiCorp Vault
- **TLS**: Chiffrement communications inter-services
- **Network policies**: Kubernetes NetworkPolicy

## Performance

- **Latence E2E**: < 5 secondes (ingestion → alerte)
- **Débit**: > 100 000 points/sec
- **Availability**: 99.9% (SLA)
- **Scalabilité**: Horizontale (tous services stateless)

## Dataset de référence

### NASA C-MAPSS (Turbofan Engine Degradation)

- **4 sous-datasets**: FD001, FD002, FD003, FD004
- **21 capteurs**: Température, pression, vitesse, débit, etc.
- **3 réglages opérationnels**
- **Scénarios**: Single/multi fault modes

**Utilisation**:
1. Entraînement modèles RUL baseline
2. Transfer learning vers actifs usine
3. Benchmark performance

## Prochaines étapes

1. ✅ Infrastructure Docker Compose
2. ✅ Service Ingestion IIoT
3. ⏳ Service Preprocessing
4. ⏳ Service Feature Extraction
5. ⏳ Service Anomaly Detection
6. ⏳ Service RUL Prediction
7. ⏳ Service Orchestrator
8. ⏳ Dashboard React
9. ⏳ Scripts données C-MAPSS
10. ⏳ Tests end-to-end
11. ⏳ Documentation utilisateur
12. ⏳ Déploiement Kubernetes

## Références

- **OPC UA**: https://opcfoundation.org/
- **NASA C-MAPSS**: https://ti.arc.nasa.gov/tech/dash/groups/pcoe/prognostic-data-repository/
- **MLflow**: https://mlflow.org/
- **Feast**: https://feast.dev/
- **TimescaleDB**: https://docs.timescale.com/
