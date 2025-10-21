# Guide de démarrage rapide - MANTIS

Ce guide vous permettra de lancer MANTIS en quelques minutes.

## Prérequis

### Obligatoire
- **Docker** >= 20.10
- **Docker Compose** >= 2.0
- **Git**

### Optionnel (pour développement)
- **Python** >= 3.10
- **Node.js** >= 18
- **kubectl** (pour déploiement Kubernetes)

## Installation

### 1. Cloner le repository

```bash
git clone <repo-url>
cd MANTIS
```

### 2. Vérifier les prérequis

```bash
docker --version
docker-compose --version
```

### 3. Lancer l'infrastructure

```bash
./scripts/start-services.sh
```

Ce script va :
- ✅ Démarrer Kafka, Zookeeper
- ✅ Démarrer PostgreSQL, TimescaleDB, InfluxDB
- ✅ Démarrer MinIO (object storage)
- ✅ Démarrer MLflow, Feast
- ✅ Démarrer Grafana, Prometheus, Jaeger
- ✅ Initialiser les bases de données
- ✅ Créer les buckets MinIO

**Temps d'attente**: ~2-3 minutes

### 4. Vérifier le démarrage

```bash
cd infrastructure/docker
docker-compose -f docker-compose.infrastructure.yml ps
```

Tous les services doivent être "Up" et "healthy".

### 5. Accéder aux interfaces

Ouvrez votre navigateur :

| Service | URL | Credentials |
|---------|-----|-------------|
| **Grafana** | http://localhost:3001 | admin / admin |
| **MLflow** | http://localhost:5000 | - |
| **Kafka UI** | http://localhost:8080 | - |
| **MinIO Console** | http://localhost:9001 | minioadmin / minioadmin |
| **Prometheus** | http://localhost:9090 | - |
| **Jaeger** | http://localhost:16686 | - |

## Première utilisation

### 1. Télécharger le dataset NASA C-MAPSS

```bash
./scripts/download-cmapss.sh
```

Ceci télécharge le dataset de référence pour l'entraînement des modèles RUL.

### 2. Lancer le service Ingestion IIoT

```bash
cd services/ingestion-iiot

# Créer environnement virtuel
python -m venv venv
source venv/bin/activate  # Linux/Mac
# ou venv\Scripts\activate sur Windows

# Installer dépendances
pip install -r requirements.txt

# Copier la configuration
cp .env.example .env

# Lancer le service
python main.py
```

Le service démarre sur http://localhost:8001

### 3. Tester l'ingestion de données

Ouvrir http://localhost:8001/docs pour voir la documentation API.

Injecter des données de test :

```bash
curl -X POST "http://localhost:8001/test/inject" \
  -H "Content-Type: application/json" \
  -d '{
    "asset_id": "550e8400-e29b-41d4-a716-446655440000",
    "sensor_id": "660e8400-e29b-41d4-a716-446655440001",
    "sensor_type": "temperature",
    "value": 75.5
  }'
```

### 4. Vérifier dans Kafka

Ouvrir Kafka UI : http://localhost:8080

- Naviguer vers **Topics** → **sensor.raw**
- Voir les messages injectés

### 5. Explorer les données dans TimescaleDB

```bash
# Se connecter à TimescaleDB
docker exec -it mantis-timescaledb psql -U mantis -d mantis_timeseries

# Requête exemple
SELECT * FROM sensor_data_raw ORDER BY time DESC LIMIT 10;

# Sortir
\q
```

### 6. Visualiser dans Grafana

1. Ouvrir http://localhost:3001 (admin/admin)
2. Aller dans **Connections** → **Data sources**
3. Ajouter TimescaleDB :
   - Type: PostgreSQL
   - Host: `timescaledb:5432`
   - Database: `mantis_timeseries`
   - User: `mantis`
   - Password: `mantis_password`
   - TLS/SSL Mode: disable

4. Créer un dashboard et ajouter un panel avec cette requête :
```sql
SELECT
  time AS "time",
  sensor_code,
  value
FROM sensor_data_raw
WHERE $__timeFilter(time)
ORDER BY time
```

## Développement

### Structure du projet

```
MANTIS/
├── services/              # Microservices
│   ├── ingestion-iiot/   # ✅ Collecte données IIoT
│   ├── preprocessing/    # 🚧 Nettoyage et fenêtrage
│   ├── feature-extraction/   # 🚧 Extraction features
│   ├── anomaly-detection/    # 🚧 Détection anomalies
│   ├── rul-prediction/       # 🚧 Prédiction RUL
│   ├── maintenance-orchestrator/  # 🚧 Orchestration
│   └── dashboard/        # 🚧 Interface React
├── infrastructure/
│   ├── docker/           # ✅ Docker Compose
│   ├── kubernetes/       # 🚧 Manifests K8s
│   └── terraform/        # 🚧 IaC
├── data/
│   ├── raw/             # Données brutes
│   ├── processed/       # Données traitées
│   └── models/          # Modèles ML
├── notebooks/           # Jupyter notebooks
├── scripts/             # ✅ Scripts utilitaires
└── tests/              # Tests unitaires/intégration
```

### Développer un nouveau service

1. Copier le template :
```bash
cp -r services/ingestion-iiot services/mon-service
cd services/mon-service
```

2. Adapter :
   - `main.py` - Point d'entrée
   - `config.py` - Configuration
   - `requirements.txt` - Dépendances
   - `Dockerfile` - Image Docker

3. Ajouter au docker-compose :
```yaml
# infrastructure/docker/docker-compose.services.yml
mon-service:
  build: ../../services/mon-service
  ports:
    - "8007:8007"
  environment:
    - KAFKA_BOOTSTRAP_SERVERS=kafka:9092
  networks:
    - mantis-network
```

### Tests

```bash
# Tests unitaires
pytest tests/unit

# Tests d'intégration
pytest tests/integration

# Coverage
pytest --cov=services tests/
```

## Scénarios d'utilisation

### Scénario 1: Simuler une usine complète

```bash
# 1. Insérer des assets dans PostgreSQL
docker exec -it mantis-postgres psql -U mantis -d mantis -c "
INSERT INTO assets (asset_code, name, type, criticality, location_line)
VALUES
  ('MOTOR-001', 'Moteur Ligne 1', 'motor', 'critical', 'line-1'),
  ('PUMP-001', 'Pompe Principale', 'pump', 'high', 'line-1'),
  ('CONV-001', 'Convoyeur A', 'conveyor', 'medium', 'line-2');
"

# 2. Insérer des capteurs
docker exec -it mantis-postgres psql -U mantis -d mantis -c "
INSERT INTO sensors (asset_id, sensor_code, sensor_type, unit)
SELECT
  id,
  asset_code || '_TEMP',
  'temperature',
  '°C'
FROM assets;
"

# 3. Simuler des données temps-réel (script Python)
python scripts/simulate-factory-data.py --assets 3 --duration 3600
```

### Scénario 2: Entraîner un modèle RUL sur C-MAPSS

```bash
# 1. Télécharger dataset
./scripts/download-cmapss.sh

# 2. Lancer notebook d'entraînement
jupyter notebook notebooks/02-rul-model-training.ipynb

# 3. Le modèle sera enregistré dans MLflow
# Voir http://localhost:5000
```

### Scénario 3: Détecter des anomalies

```bash
# 1. Envoyer des données normales
for i in {1..100}; do
  curl -X POST "http://localhost:8001/test/inject" \
    -d '{"asset_id":"...","sensor_type":"vibration","value":'$((50 + RANDOM % 10))'}'
  sleep 0.1
done

# 2. Envoyer une anomalie
curl -X POST "http://localhost:8001/test/inject" \
  -d '{"asset_id":"...","sensor_type":"vibration","value":250}'

# 3. Le service anomaly-detection devrait la détecter
# Vérifier dans Kafka topic "anomalies.detected"
```

## Troubleshooting

### Kafka ne démarre pas

```bash
# Vérifier les logs
docker logs mantis-kafka

# Nettoyer et redémarrer
docker-compose -f infrastructure/docker/docker-compose.infrastructure.yml down -v
./scripts/start-services.sh
```

### PostgreSQL n'accepte pas les connexions

```bash
# Vérifier que le container est up
docker ps | grep postgres

# Tester la connexion
docker exec -it mantis-postgres pg_isready -U mantis

# Voir les logs
docker logs mantis-postgres
```

### MinIO buckets non créés

```bash
# Recréer les buckets manuellement
docker exec -it mantis-minio mc alias set local http://localhost:9000 minioadmin minioadmin
docker exec -it mantis-minio mc mb local/raw-data
docker exec -it mantis-minio mc mb local/models
```

### Service Python plante au démarrage

```bash
# Vérifier les variables d'environnement
cat services/ingestion-iiot/.env

# Vérifier la connexion Kafka
telnet kafka 9092

# Voir les logs détaillés
python main.py  # Mode debug
```

## Arrêter MANTIS

```bash
# Arrêter tous les services
./scripts/stop-services.sh

# Arrêter ET supprimer les volumes (⚠️ perte de données)
cd infrastructure/docker
docker-compose -f docker-compose.infrastructure.yml down -v
```

## Prochaines étapes

1. ✅ Explorer les données dans Grafana
2. ✅ Lancer un notebook Jupyter pour analyser C-MAPSS
3. 📖 Lire [ARCHITECTURE.md](ARCHITECTURE.md) pour comprendre le système complet
4. 🔨 Contribuer au développement des autres services
5. 🚀 Déployer en production avec Kubernetes

## Support

- 📧 Email: O.ouedrhiri@emsi.ma, H.Tabbaa@emsi.ma, lachgar.m@gmail.com
- 📚 Documentation: [docs/](docs/)
- 🐛 Issues: GitHub Issues
- 💬 Discussion: GitHub Discussions

## Licence

MIT License - voir [LICENSE](LICENSE)
