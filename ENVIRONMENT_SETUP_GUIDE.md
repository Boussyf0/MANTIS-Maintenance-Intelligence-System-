# Guide de Configuration des Environnements MANTIS

## 🎯 Vue d'ensemble

Ce guide explique comment configurer les différents environnements (développement, staging, production) pour le projet MANTIS.

## 📁 Structure des Environnements

```
MANTIS/
├── .env.example                    # Template général
├── environments/
│   ├── development/.env            # Config développement
│   ├── staging/.env                # Config staging
│   ├── production/.env.template    # Template production (secrets à compléter)
│   └── README.md                   # Documentation détaillée
└── scripts/
    └── setup-environment.sh        # Script de configuration automatique
```

## 🚀 Démarrage Rapide

### 1. Configurer l'environnement de développement

```bash
# Méthode automatique (recommandée)
./scripts/setup-environment.sh development

# Ou manuellement
cp environments/development/.env .env

# Démarrer les services
make docker-up
```

### 2. Vérifier que tout fonctionne

```bash
# Vérifier les containers
docker-compose -f infrastructure/docker/docker-compose.infrastructure.yml ps

# Accéder aux interfaces
open http://localhost:3001  # Grafana (admin/admin)
open http://localhost:5000  # MLflow
open http://localhost:8080  # Kafka UI
```

## 🌍 Environnements Disponibles

### Development (Développement Local)

**Caractéristiques** :
- ✅ Démarrage rapide
- ✅ Données de test
- ✅ Logs DEBUG
- ✅ Mots de passe simples
- ✅ Hot reload activé

**Usage** :
```bash
./scripts/setup-environment.sh development
make docker-up
```

**Services** :
| Service | URL | Credentials |
|---------|-----|-------------|
| PostgreSQL | localhost:5432 | mantis/mantis_dev_password |
| TimescaleDB | localhost:5433 | mantis/mantis_dev_password |
| Kafka | localhost:9092 | - |
| Redis | localhost:6379 | redis_dev_password |
| MinIO | localhost:9000 | minioadmin/minioadmin |
| MLflow | http://localhost:5000 | - |
| Grafana | http://localhost:3001 | admin/admin |

### Staging (Pré-production)

**Caractéristiques** :
- ✅ Miroir de production
- ✅ TLS/SSL activé
- ✅ Mots de passe forts
- ✅ Tests d'intégration
- ✅ Monitoring complet

**Usage** :
```bash
./scripts/setup-environment.sh staging

# ⚠️ Remplacer tous les <CHANGE_ME_*> dans .env
nano .env

make docker-up
```

### Production

**Caractéristiques** :
- 🔒 Sécurité maximale
- 🔒 Secrets depuis Vault
- 🔒 TLS/SSL obligatoire
- 🔒 Haute disponibilité
- 🔒 Backups automatiques
- 🔒 Conformité GDPR

**Usage** :
```bash
./scripts/setup-environment.sh production

# ⚠️⚠️⚠️ CRITIQUE ⚠️⚠️⚠️
# NE PAS utiliser directement le template
# Utiliser HashiCorp Vault ou AWS Secrets Manager
```

## 🔑 Variables d'Environnement Principales

### Générales

```bash
ENVIRONMENT=development|staging|production
PROJECT_NAME=mantis
LOG_LEVEL=DEBUG|INFO|WARNING|ERROR
DEBUG=true|false
```

### Bases de Données

```bash
# PostgreSQL
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=mantis_dev
POSTGRES_USER=mantis
POSTGRES_PASSWORD=mantis_dev_password

# TimescaleDB
TIMESCALEDB_HOST=localhost
TIMESCALEDB_PORT=5433
TIMESCALEDB_DB=mantis_timeseries_dev
TIMESCALEDB_USER=mantis
TIMESCALEDB_PASSWORD=mantis_dev_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=redis_dev_password
```

### Kafka

```bash
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_GROUP_ID=mantis-dev-consumer

# Topics
KAFKA_TOPIC_SENSOR_RAW=sensor.raw
KAFKA_TOPIC_SENSOR_PREPROCESSED=sensor.preprocessed
KAFKA_TOPIC_FEATURES_COMPUTED=features.computed
KAFKA_TOPIC_ANOMALIES_DETECTED=anomalies.detected
KAFKA_TOPIC_RUL_PREDICTIONS=rul.predictions
KAFKA_TOPIC_MAINTENANCE_ACTIONS=maintenance.actions
```

### Object Storage (MinIO/S3)

```bash
MINIO_ENDPOINT=localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET_RAW_DATA=raw-data-dev
MINIO_BUCKET_MODELS=models-dev
```

### ML/MLOps

```bash
# MLflow
MLFLOW_TRACKING_URI=http://localhost:5000
MLFLOW_ARTIFACT_ROOT=s3://mlflow-dev

# Feast
FEAST_ONLINE_STORE=redis
FEAST_OFFLINE_STORE=parquet

# Modèles
MODEL_RUL_PATH=models/rul_model_dev
MODEL_ANOMALY_PATH=models/anomaly_detector_dev
```

### Services Ports

```bash
PORT_INGESTION_IIOT=8001
PORT_PREPROCESSING=8002
PORT_FEATURE_EXTRACTION=8003
PORT_ANOMALY_DETECTION=8004
PORT_RUL_PREDICTION=8005
PORT_ORCHESTRATOR=8006
PORT_DASHBOARD=3000
```

## 🔒 Gestion des Secrets

### Développement

```bash
# Mots de passe simples OK
POSTGRES_PASSWORD=mantis_dev_password
```

### Staging/Production

**❌ NE JAMAIS** :
- Hardcoder des mots de passe
- Commiter `.env` dans Git
- Partager des secrets par email/Slack

**✅ UTILISER** :
- HashiCorp Vault (recommandé)
- AWS Secrets Manager
- Azure Key Vault
- Google Secret Manager

**Exemple avec Vault** :
```bash
# 1. Stocker le secret
vault kv put secret/mantis/production/postgres \
  password="$(openssl rand -base64 32)"

# 2. Récupérer dans le script de démarrage
export POSTGRES_PASSWORD=$(vault kv get -field=password \
  secret/mantis/production/postgres)

# 3. Démarrer le service
docker-compose up -d
```

## 📊 Comparaison des Environnements

| Aspect | Development | Staging | Production |
|--------|-------------|---------|------------|
| **CPU** | 2-4 cores | 8 cores | 16+ cores |
| **RAM** | 8 GB | 32 GB | 64+ GB |
| **Storage** | 50 GB | 500 GB | 2+ TB |
| **Replicas** | 1 | 2 | 3+ |
| **TLS/SSL** | Non | Oui | Oui |
| **Auth** | Basique | Forte | Maximale |
| **Monitoring** | Optional | Recommandé | Obligatoire |
| **Backups** | Non | Quotidiens | Multiple/jour |
| **Logs** | DEBUG | INFO | WARNING |
| **Retention** | 7 jours | 30 jours | 90-365 jours |

## 🔄 Migration entre Environnements

### Dev → Staging

```bash
# 1. Tests locaux
make test
make lint
make docker-build

# 2. Configuration staging
./scripts/setup-environment.sh staging

# 3. Déploiement
docker-compose up -d

# 4. Validation
make test-integration
./scripts/smoke-tests.sh

# 5. Monitoring
# Vérifier Grafana, logs, métriques
```

### Staging → Production

```bash
# 1. ✅ Tous les tests passent en staging
make test-e2e
make test-load

# 2. ✅ Review sécurité
./scripts/security-audit.sh

# 3. ✅ Backup production actuelle
./scripts/backup-production.sh

# 4. ✅ Configuration secrets (Vault)
./scripts/configure-vault-secrets.sh

# 5. 🚀 Déploiement progressif (Blue-Green / Canary)
kubectl apply -f infrastructure/kubernetes/production/

# 6. 👀 Monitoring intensif
# - Métriques Prometheus
# - Dashboards Grafana
# - Logs centralisés
# - Alertes actives

# 7. 🔄 Rollback si nécessaire
kubectl rollout undo deployment/mantis-ingestion-iiot
```

## 📋 Checklist de Configuration

### Development ✓

- [ ] Fichier `.env` copié depuis `environments/development/.env`
- [ ] Docker et Docker Compose installés (versions récentes)
- [ ] Services démarrent sans erreur
- [ ] Accès aux interfaces web (Grafana, MLflow, Kafka UI)
- [ ] Connexion aux bases de données OK
- [ ] Git hooks installés (`./scripts/install-hooks.sh`)
- [ ] Tests passent (`make test`)

### Staging ✓

- [ ] Fichier `.env` configuré avec mots de passe forts
- [ ] Tous les `<CHANGE_ME_*>` remplacés
- [ ] TLS/SSL activé et certificats valides
- [ ] Firewall configuré (ports nécessaires uniquement)
- [ ] Données de test réalistes chargées
- [ ] Monitoring opérationnel (Prometheus, Grafana, Jaeger)
- [ ] Logs centralisés (ELK ou équivalent)
- [ ] Tests automatiques en CI/CD
- [ ] Backups quotidiens configurés et testés
- [ ] Plan de rollback documenté

### Production 🔒

- [ ] **Secrets depuis gestionnaire sécurisé** (Vault/AWS Secrets)
- [ ] Tous les `<VAULT_SECRET>` remplacés par vraies valeurs
- [ ] TLS/SSL activé **partout** avec certificats valides
- [ ] Mots de passe forts (min 32 caractères aléatoires)
- [ ] Clés JWT générées : `openssl rand -hex 32`
- [ ] Firewalls strictement configurés
- [ ] Authentification activée sur **tous** les services
- [ ] Rate limiting configuré
- [ ] Haute disponibilité (HA) - 3+ replicas
- [ ] Monitoring et alerting 24/7 (PagerDuty/OpsGenie)
- [ ] Logs centralisés avec rétention conforme
- [ ] Audit logging activé
- [ ] Backups automatiques testés (RTO < 1h, RPO < 5min)
- [ ] Plan de Disaster Recovery testé
- [ ] Tests de charge effectués et validés
- [ ] Conformité GDPR/ISO27001/SOC2
- [ ] Encryption at rest activée
- [ ] Encryption in transit (TLS 1.3)
- [ ] Secrets rotation policy (90 jours)
- [ ] Vulnerability scanning automatique
- [ ] Penetration testing annuel
- [ ] Incident response plan documenté
- [ ] Documentation complète et à jour

## 🆘 Dépannage

### `.env` non pris en compte

```bash
# 1. Vérifier existence
ls -la .env

# 2. Vérifier format (pas de BOM, LF not CRLF)
file .env
# Devrait afficher: .env: ASCII text

# 3. Tester chargement
source .env && echo $POSTGRES_HOST

# 4. Redémarrer services
docker-compose down
docker-compose up -d
```

### Services ne démarrent pas

```bash
# Logs détaillés
docker-compose logs --tail=100 -f

# Vérifier une config spécifique
docker-compose config | grep POSTGRES

# Tester connexion DB
docker exec -it mantis-postgres psql -U mantis -d mantis_dev
```

### Conflits de ports

```bash
# Trouver processus utilisant le port
lsof -i :5432
# ou
netstat -an | grep 5432

# Modifier le port dans .env
POSTGRES_PORT=5433
```

### Mémoire insuffisante

```bash
# Ajuster les limites Docker
# Docker Desktop → Preferences → Resources
# RAM: 8 GB minimum pour dev, 32+ GB pour staging/prod

# Ou dans docker-compose.yml
services:
  postgres:
    mem_limit: 2g
```

## 💡 Bonnes Pratiques

### Général

✅ **À FAIRE** :
- Utiliser le script `setup-environment.sh`
- Documenter toutes les variables
- Versionner `.env.example`
- Tester les changements en dev d'abord
- Monitoring dès le début

❌ **À ÉVITER** :
- Hardcoder des configs dans le code
- Commiter `.env`
- Réutiliser les mêmes secrets partout
- Déployer en prod sans tests

### Sécurité

✅ **À FAIRE** :
- Rotation régulière des secrets
- Principe du moindre privilège
- Logs d'audit
- Chiffrement at rest et in transit
- Scans de vulnérabilités

❌ **À ÉVITER** :
- Mots de passe faibles
- Ports ouverts inutilement
- Logs contenant des secrets
- Services non authentifiés en prod

### Performance

✅ **À FAIRE** :
- Profiler avant d'optimiser
- Monitorer les métriques
- Ajuster selon la charge réelle
- Tests de charge réguliers

❌ **À ÉVITER** :
- Over-provisioning systématique
- Ignorer les warnings mémoire
- Sous-dimensionner la prod

## 📚 Ressources

- [Documentation environnements](environments/README.md)
- [12-Factor App](https://12factor.net/)
- [HashiCorp Vault](https://www.vaultproject.io/)
- [Docker Environment Variables](https://docs.docker.com/compose/environment-variables/)
- [Kubernetes Secrets](https://kubernetes.io/docs/concepts/configuration/secret/)

---

**Maintenu par** : MANTIS Team - EMSI
**Version** : 1.0.0
**Dernière mise à jour** : 2025-01-22
