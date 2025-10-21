# MANTIS - MAiNtenance prédictive Temps-réel pour usines Intelligentes

## Vue d'ensemble

Plateforme temps-réel de maintenance prédictive pour usines intelligentes, capable de détecter précocement les anomalies, estimer la Remaining Useful Life (RUL) des équipements, et planifier des interventions optimales.

## Contexte

Les usines subissent des arrêts non planifiés coûteux (~50 Md USD/an dans le manufacturing ; coût médian > 125 000 USD/heure). Cette solution permet de passer de la maintenance corrective/préventive à une maintenance prédictive basée sur l'analyse de flux IIoT en continu.

## Architecture Microservices

Le système est composé de 7 microservices :

1. **IngestionIIoT** - Collecte des flux capteurs depuis PLC/SCADA/edge
2. **Prétraitement** - Nettoyage, alignement et fenêtrage des données
3. **ExtractionFeatures** - Calcul de descripteurs temps/fréquence
4. **DétectionAnomalies** - Détection de déviations par rapport au fonctionnement nominal
5. **PrédictionRUL** - Estimation de la Remaining Useful Life
6. **OrchestrateurMaintenance** - Application de politiques et génération d'actions
7. **DashboardUsine** - Visualisation temps-réel de l'état des lignes

## Technologies

### Backend
- **Streaming**: Apache Kafka, Kafka Streams
- **Bases de données**: TimescaleDB, PostgreSQL, InfluxDB, MinIO
- **ML/AI**: PyTorch, XGBoost, PyOD, tsfresh
- **MLOps**: MLflow, Feast
- **IIoT**: OPC UA (Eclipse Milo), MQTT, Telegraf

### Frontend
- **Framework**: React.js
- **Visualisation**: Grafana, Plotly
- **Communication**: REST/gRPC, WebSockets

### Infrastructure
- **Conteneurisation**: Docker, Docker Compose
- **Orchestration**: Kubernetes
- **Observabilité**: OpenTelemetry, Prometheus, Grafana

## Jeux de données

- **NASA C-MAPSS**: Dataset de référence pour l'entraînement des modèles RUL
  - 21 capteurs
  - 3 réglages moteur
  - 4 scénarios de défaillance

## Installation

### Prérequis
- Docker >= 20.10
- Docker Compose >= 2.0
- Kubernetes >= 1.24 (optionnel, pour production)
- Python >= 3.10
- Node.js >= 18

### Installation rapide

```bash
# Cloner le repository
git clone <repo-url>
cd MANTIS

# Lancer l'infrastructure
docker-compose up -d

# Initialiser les bases de données
./scripts/init-databases.sh

# Télécharger et préparer les datasets
./scripts/download-cmapss.sh

# Lancer les services
./scripts/start-services.sh
```

## Structure du projet

```
MANTIS/
├── services/
│   ├── ingestion-iiot/
│   ├── preprocessing/
│   ├── feature-extraction/
│   ├── anomaly-detection/
│   ├── rul-prediction/
│   ├── maintenance-orchestrator/
│   └── dashboard/
├── infrastructure/
│   ├── docker/
│   ├── kubernetes/
│   └── terraform/
├── data/
│   ├── raw/
│   ├── processed/
│   └── models/
├── notebooks/
├── scripts/
├── tests/
└── docs/
```

## Démarrage rapide

### 1. Lancer l'infrastructure de base

```bash
cd infrastructure/docker
docker-compose -f docker-compose.infrastructure.yml up -d
```

Cela démarre :
- Kafka + Zookeeper
- TimescaleDB
- PostgreSQL
- InfluxDB
- MinIO
- MLflow
- Feast

### 2. Lancer les microservices

```bash
docker-compose -f docker-compose.services.yml up -d
```

### 3. Accéder aux interfaces

- **Dashboard Usine**: http://localhost:3000
- **Grafana**: http://localhost:3001 (admin/admin)
- **MLflow**: http://localhost:5000
- **Kafka UI**: http://localhost:8080
- **MinIO Console**: http://localhost:9001

## Utilisation

### Ingestion de données

```python
from mantis.ingestion import OPCUAConnector

# Connexion à un serveur OPC UA
connector = OPCUAConnector("opc.tcp://localhost:4840")
connector.subscribe_nodes([
    "ns=2;s=Temperature",
    "ns=2;s=Vibration",
    "ns=2;s=Current"
])
connector.start()
```

### Prédiction RUL

```python
from mantis.prediction import RULPredictor

predictor = RULPredictor.load("models/rul_model_v1")
rul, confidence = predictor.predict(sensor_data)
print(f"RUL estimée: {rul} heures (confiance: {confidence}%)")
```

### Détection d'anomalies

```python
from mantis.anomaly import AnomalyDetector

detector = AnomalyDetector.load("models/anomaly_detector_v1")
anomaly_score = detector.detect(features)
if anomaly_score > 0.8:
    print("Anomalie détectée!")
```

## API REST

### Endpoints principaux

- `GET /api/assets` - Liste des actifs
- `GET /api/assets/{id}/health` - État de santé d'un actif
- `GET /api/assets/{id}/rul` - RUL prédite
- `GET /api/anomalies` - Anomalies détectées
- `GET /api/maintenance/recommendations` - Recommandations de maintenance
- `POST /api/maintenance/work-orders` - Créer un ordre de travail

Documentation complète : http://localhost:8000/docs

## Développement

### Setup environnement de développement

```bash
# Créer un environnement virtuel
python -m venv venv
source venv/bin/activate  # Linux/Mac
# ou venv\Scripts\activate  # Windows

# Installer les dépendances
pip install -r requirements-dev.txt

# Installer les pre-commit hooks
pre-commit install
```

### Tests

```bash
# Tests unitaires
pytest tests/unit

# Tests d'intégration
pytest tests/integration

# Couverture
pytest --cov=mantis tests/
```

## Performance

- Latence ingestion → alerte : < 5 secondes
- Débit : > 100 000 points/seconde
- Disponibilité : 99.9%
- Scalabilité horizontale : Tous les services sont stateless

## Cas d'usage

### Exemple 1: Moteur électrique
- Capteurs: vibration, température, courant
- RUL médiane: 240 heures avant défaillance roulement
- Économie: ~150 K€ d'arrêt évité

### Exemple 2: Pompe centrifuge
- Détection de cavitation précoce
- Anomalie détectée 72h avant défaillance
- Économie: ~80 K€ + remplacement planifié

## Roadmap

- [x] MVP avec NASA C-MAPSS
- [x] Intégration OPC UA
- [ ] Support Modbus TCP
- [ ] Edge computing (processing local)
- [ ] Maintenance collaborative (mobile app)
- [ ] Digital twin integration
- [ ] Prédiction multi-assets
- [ ] AutoML pour adaptation automatique

## Licence

MIT License

## Contact

Pour toute question ou collaboration :
- Pr. Oumayma OUEDRHIRI (O.ouedrhiri@emsi.ma)
- Pr. Hiba TABBAA (H.Tabbaa@emsi.ma)
- Pr. Mohamed LACHGAR (lachgar.m@gmail.com)

## Citation

Si vous utilisez MANTIS dans vos recherches, veuillez citer :

```bibtex
@software{mantis2025,
  title={MANTIS: Maintenance prédictive temps-réel pour usines intelligentes},
  author={EMSI Engineering School},
  year={2025},
  url={https://github.com/...}
}
```
