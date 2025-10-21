# MANTIS - Résumé du projet

## 📋 Vue d'ensemble

**MANTIS** (MAiNtenance prédictive Temps-réel pour usines Intelligentes) est une plateforme complète de maintenance prédictive pour l'industrie 4.0, développée dans le cadre d'un projet académique EMSI.

### Contexte industriel

- **Problème**: Arrêts non planifiés coûteux (~50 Md USD/an dans le manufacturing)
- **Coût médian d'un arrêt**: > 125 000 USD/heure
- **Solution**: Passage de la maintenance corrective/préventive à la maintenance prédictive

### Objectifs

1. ✅ Détection précoce des anomalies
2. ✅ Estimation de la Remaining Useful Life (RUL)
3. ✅ Planification optimale des interventions
4. ✅ Intégration native aux systèmes OT/IT (SCADA/MES/CMMS/ERP)

## 🏗️ Architecture

### Microservices (7 au total)

```
1. IngestionIIoT      → Collecte données (OPC UA, MQTT, Modbus)
2. Preprocessing      → Nettoyage et fenêtrage
3. FeatureExtraction  → Extraction de caractéristiques (temps, fréquence, ondelettes)
4. AnomalyDetection   → Détection d'anomalies (ML)
5. RULPrediction      → Prédiction RUL (Deep Learning)
6. Orchestrator       → Règles métier et planification
7. Dashboard          → Interface utilisateur React
```

### Stack technologique

#### Backend (Hybride Java + Python)
- **Services Java**: Spring Boot 3.2 (Ingestion, Preprocessing, Orchestrator)
- **Services Python**: FastAPI (Feature Extraction, Anomaly Detection, RUL Prediction)
- **Streaming**: Apache Kafka
- **ML/DL**: PyTorch, XGBoost, scikit-learn, PyOD
- **MLOps**: MLflow, Feast (Feature Store)

#### Bases de données
- **PostgreSQL**: Métadonnées (assets, work orders, etc.)
- **TimescaleDB**: Séries temporelles (capteurs, features)
- **InfluxDB**: Haute fréquence (>100Hz)
- **MinIO**: Object storage (modèles, données brutes)
- **Redis**: Feature Store online

#### Infrastructure
- **Conteneurisation**: Docker, Docker Compose
- **Orchestration**: Kubernetes
- **Monitoring**: Prometheus, Grafana
- **Tracing**: Jaeger (OpenTelemetry)

#### Frontend
- **Framework**: React.js, Next.js
- **UI**: TailwindCSS
- **Visualisation**: Plotly.js, Recharts, D3.js
- **Temps-réel**: WebSockets

## 📊 Dataset de référence

### NASA C-MAPSS (Commercial Modular Aero-Propulsion System Simulation)

- **Source**: NASA Prognostics Center of Excellence
- **Description**: Simulation de dégradation de moteurs turbofan
- **Données**:
  - 4 sous-datasets (FD001-FD004)
  - 21 capteurs (température, pression, vitesse, débit)
  - 3 réglages opérationnels
  - Scénarios single/multi fault modes

**Utilisation**:
1. Entraînement modèles RUL baseline
2. Transfer learning vers actifs usine
3. Benchmark de performance

## 📁 Structure du projet

```
MANTIS/
├── services/                      # 🎯 Microservices
│   ├── ingestion-iiot/           # ☕ JAVA - 40% complété
│   │   ├── pom.xml
│   │   ├── src/main/java/com/mantis/ingestion/
│   │   │   ├── IngestionApplication.java
│   │   │   ├── model/SensorData.java
│   │   │   ├── service/KafkaProducerService.java
│   │   │   ├── config/           # À développer
│   │   │   ├── connector/        # À développer (OPC UA, MQTT, Modbus)
│   │   │   └── controller/       # À développer
│   │   └── src/main/resources/application.yml
│   ├── preprocessing/            # ☕ JAVA - À développer
│   ├── feature-extraction/       # 🐍 PYTHON - À développer
│   ├── anomaly-detection/        # 🐍 PYTHON - À développer
│   ├── rul-prediction/           # 🐍 PYTHON - À développer
│   ├── maintenance-orchestrator/ # ☕ JAVA - À développer
│   └── dashboard/                # ⚛️ REACT - À développer
│
├── infrastructure/               # ✅ COMPLÉTÉ
│   ├── docker/
│   │   ├── docker-compose.infrastructure.yml  # Kafka, DBs, MLflow, etc.
│   │   ├── init-scripts/
│   │   │   ├── postgres/         # Schéma PostgreSQL
│   │   │   └── timescaledb/      # Hypertables TimescaleDB
│   │   ├── grafana/
│   │   │   └── provisioning/     # Datasources Grafana
│   │   └── prometheus/
│   │       └── prometheus.yml
│   ├── kubernetes/               # 🚧 À développer
│   └── terraform/                # 🚧 À développer
│
├── data/                         # Données
│   ├── raw/                      # NASA C-MAPSS (via script)
│   ├── processed/                # Données préparées
│   └── models/                   # Modèles ML entraînés
│
├── notebooks/                    # ✅ Jupyter notebooks
│   └── 01-cmapss-exploration.ipynb
│
├── scripts/                      # ✅ Scripts utilitaires
│   ├── download-cmapss.sh       # Télécharge dataset NASA
│   ├── start-services.sh        # Démarre l'infrastructure
│   └── stop-services.sh         # Arrête tout
│
├── tests/                        # Tests
│   ├── unit/
│   └── integration/
│
├── docs/                         # Documentation
│
├── README.md                     # ✅ README principal
├── ARCHITECTURE.md               # ✅ Documentation architecture
├── QUICKSTART.md                 # ✅ Guide démarrage rapide
├── CONTRIBUTING.md               # ✅ Guide de contribution
├── Makefile                      # ✅ Commandes make
├── requirements.txt              # ✅ Dépendances Python
└── .gitignore                    # ✅ Gitignore
```

## 🎯 État d'avancement

### ✅ Complété (40%)

1. **Infrastructure**
   - ✅ Docker Compose complet (Kafka, PostgreSQL, TimescaleDB, InfluxDB, MinIO, MLflow, Redis, Grafana, Prometheus, Jaeger)
   - ✅ Schémas de base de données (PostgreSQL + TimescaleDB)
   - ✅ Scripts d'initialisation
   - ✅ Configuration Prometheus et Grafana

2. **Service Ingestion IIoT (Java/Spring Boot)**
   - ✅ Architecture Spring Boot 3.2
   - ✅ Configuration Maven (pom.xml)
   - ✅ Modèle de données (SensorData.java)
   - ✅ Service Kafka Producer avec métriques
   - ✅ Configuration application.yml
   - 🚧 Connecteurs à compléter:
     - Connecteur OPC UA (Eclipse Milo)
     - Connecteur MQTT (Eclipse Paho)
     - Connecteur Modbus TCP
   - ✅ Documentation complète

3. **Documentation**
   - ✅ README principal
   - ✅ Architecture détaillée
   - ✅ Guide de démarrage rapide
   - ✅ Guide de contribution
   - ✅ Notebook exploration C-MAPSS

4. **Scripts & Outils**
   - ✅ Scripts de démarrage/arrêt
   - ✅ Script téléchargement dataset
   - ✅ Makefile avec commandes utiles

### 🚧 En cours / À développer (60%)

5. **Service Preprocessing**
   - 🚧 Kafka Consumer
   - 🚧 Nettoyage données (outliers, missing values)
   - 🚧 Rééchantillage et alignement
   - 🚧 Fenêtrage glissant
   - 🚧 Filtres (Butterworth, Savitzky-Golay)

6. **Service Feature Extraction**
   - 🚧 Features temps (RMS, kurtosis, crest factor)
   - 🚧 Features fréquence (FFT, STFT, spectral)
   - 🚧 Features ondelettes (PyWavelets)
   - 🚧 Intégration Feast (Feature Store)
   - 🚧 tsfresh pour extraction automatique

7. **Service Anomaly Detection**
   - 🚧 Isolation Forest (PyOD)
   - 🚧 One-Class SVM
   - 🚧 Autoencoder (PyTorch)
   - 🚧 Seuils adaptatifs par criticité
   - 🚧 Agrégation multi-modèles

8. **Service RUL Prediction**
   - 🚧 Chargement dataset C-MAPSS
   - 🚧 Modèle LSTM/GRU (PyTorch)
   - 🚧 Modèle TCN (Temporal Convolutional Network)
   - 🚧 Modèle XGBoost (baseline)
   - 🚧 Transfer learning
   - 🚧 Tracking MLflow
   - 🚧 Uncertainty quantification

9. **Service Maintenance Orchestrator**
   - 🚧 Règles métier (Drools ou Python)
   - 🚧 Optimisation planning (OR-Tools)
   - 🚧 Génération work orders
   - 🚧 Gestion inventaire pièces
   - 🚧 Intégration CMMS/ERP

10. **Dashboard React**
    - 🚧 Setup Next.js + TailwindCSS
    - 🚧 Vue Overview (heatmap assets)
    - 🚧 Vue Asset Detail (RUL, graphes)
    - 🚧 Vue Anomalies
    - 🚧 Vue Maintenance (work orders)
    - 🚧 KPIs (MTBF, MTTR, OEE)
    - 🚧 WebSocket temps-réel

11. **MLOps**
    - 🚧 Pipelines entraînement modèles
    - 🚧 CI/CD modèles (MLflow)
    - 🚧 Monitoring drift
    - 🚧 A/B testing modèles

12. **Déploiement**
    - 🚧 Manifests Kubernetes
    - 🚧 Helm charts
    - 🚧 Terraform (IaC)
    - 🚧 CI/CD GitHub Actions

13. **Tests**
    - 🚧 Tests unitaires (tous services)
    - 🚧 Tests d'intégration
    - 🚧 Tests E2E
    - 🚧 Couverture > 80%

## 🚀 Démarrage rapide

```bash
# 1. Cloner le repo
git clone <repo-url>
cd MANTIS

# 2. Démarrer l'infrastructure
./scripts/start-services.sh

# 3. Télécharger le dataset
./scripts/download-cmapss.sh

# 4. Lancer le service Ingestion IIoT (Java)
cd services/ingestion-iiot
mvn spring-boot:run

# 5. Accéder aux interfaces
# - Ingestion API: http://localhost:8001/swagger-ui.html
# - Grafana: http://localhost:3001 (admin/admin)
# - MLflow: http://localhost:5000
# - Kafka UI: http://localhost:8080
```

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [README.md](README.md) | Vue d'ensemble et installation |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Architecture détaillée des microservices |
| [QUICKSTART.md](QUICKSTART.md) | Guide de démarrage rapide |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Guide de contribution |
| [services/ingestion-iiot/README.md](services/ingestion-iiot/README.md) | Doc du service Ingestion |

## 🎓 Compétences mobilisées

### Techniques
- ✅ Architecture microservices
- ✅ Streaming temps-réel (Kafka)
- ✅ Bases de données (SQL, TimescaleDB, InfluxDB)
- ✅ Protocoles IIoT (OPC UA, MQTT, Modbus)
- 🚧 Machine Learning (anomaly detection, RUL)
- 🚧 Deep Learning (LSTM, TCN)
- 🚧 MLOps (MLflow, Feast, versioning)
- 🚧 Feature engineering (time series)
- ✅ Containerisation (Docker)
- 🚧 Orchestration (Kubernetes)
- ✅ Monitoring (Prometheus, Grafana)
- 🚧 Développement frontend (React)

### Métier
- ✅ Maintenance prédictive
- ✅ IIoT / Industrie 4.0
- ✅ Intégration OT/IT
- 🚧 Optimisation planning maintenance
- 🚧 Règles métier industrielles
- 🚧 KPIs industriels (OEE, MTBF, MTTR)

## 👥 Équipe pédagogique

- **Pr. Oumayma OUEDRHIRI** - O.ouedrhiri@emsi.ma
- **Pr. Hiba TABBAA** - H.Tabbaa@emsi.ma
- **Pr. Mohamed LACHGAR** - lachgar.m@gmail.com

## 📅 Planning suggéré

### Phase 1 - Infrastructure & Ingestion (2 semaines) ✅ COMPLÉTÉ
- ✅ Setup Docker Compose
- ✅ Bases de données
- ✅ Service Ingestion IIoT
- ✅ Documentation

### Phase 2 - Preprocessing & Features (2 semaines)
- Preprocessing service
- Feature extraction service
- Tests et validation

### Phase 3 - ML/DL (3 semaines)
- Anomaly detection service
- RUL prediction service (LSTM/TCN)
- Entraînement sur C-MAPSS
- MLflow tracking

### Phase 4 - Orchestration & Dashboard (2 semaines)
- Maintenance orchestrator
- Dashboard React
- Intégration E2E

### Phase 5 - Production & Docs (1 semaine)
- Kubernetes
- Tests complets
- Documentation utilisateur
- Vidéo démo

## 🎯 Livrables attendus

1. ✅ **Code source** (GitHub)
2. ✅ **Documentation technique** (Architecture, API)
3. 🚧 **Documentation utilisateur** (Guide d'installation, utilisation)
4. 🚧 **Notebooks Jupyter** (Exploration data, entraînement modèles)
5. 🚧 **Tests** (Unitaires, intégration, E2E)
6. 🚧 **Déploiement** (Docker Compose + Kubernetes)
7. 🚧 **Rapport final** (Méthodologie, résultats, ROI)
8. 🚧 **Présentation** (Slides + démo live)

## 📈 KPIs de succès

### Techniques
- Latence E2E < 5 secondes (ingestion → alerte)
- Débit > 100 000 points/seconde
- Disponibilité > 99.9%
- Couverture tests > 80%

### Métier
- Détection anomalies : Précision > 85%, Recall > 90%
- Prédiction RUL : RMSE < 15 cycles (sur C-MAPSS)
- Réduction downtime : > 30% (simulé)
- ROI estimé : Économies vs. coût système

## 📝 Licence

MIT License

---

**Statut projet**: 🟡 En développement actif (40% complété)

**Dernière mise à jour**: 2025-10-21
