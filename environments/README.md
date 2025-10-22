# Gestion des Environnements MANTIS

Ce répertoire contient les configurations spécifiques pour chaque environnement du projet MANTIS.

## 📁 Structure

```
environments/
├── development/          # Environnement de développement local
│   └── .env             # Configuration développement
├── staging/             # Environnement de pré-production
│   └── .env             # Configuration staging
├── production/          # Environnement de production
│   └── .env.template    # Template pour production (secrets à compléter)
└── README.md            # Ce fichier
```

## 🌍 Environnements Disponibles

### 1. Development (Développement)

**Usage** : Développement local sur les machines des développeurs

**Caractéristiques** :
- Services sur localhost
- Données de test / mock data
- Logs en mode DEBUG
- Pas de sécurité stricte (mots de passe simples)
- Petits volumes de données
- Démarrage rapide

**Configuration** :
```bash
./scripts/setup-environment.sh development
```

**Services accessibles** :
- PostgreSQL: localhost:5432
- TimescaleDB: localhost:5433
- Kafka: localhost:9092
- Redis: localhost:6379
- MinIO: localhost:9000
- MLflow: localhost:5000
- Grafana: localhost:3001

### 2. Staging (Pré-production)

**Usage** : Tests d'intégration, validation avant production

**Caractéristiques** :
- Miroir de la production
- Données de test réalistes
- Logs en mode INFO
- Sécurité renforcée (TLS, auth)
- Volumes de données moyens
- Tests de performance

**Configuration** :
```bash
./scripts/setup-environment.sh staging
```

**⚠️ Important** :
- Remplacer tous les `<CHANGE_ME_*>` par de vraies valeurs
- Utiliser des mots de passe forts
- Activer TLS/SSL

### 3. Production

**Usage** : Environnement de production

**Caractéristiques** :
- Haute disponibilité (HA)
- Logs en mode WARNING/ERROR
- Sécurité maximale
- Volumes de données réels
- Monitoring complet
- Backups automatiques
- Disaster Recovery

**Configuration** :
```bash
./scripts/setup-environment.sh production
```

**🔒 SÉCURITÉ CRITIQUE** :
- **NE JAMAIS** utiliser directement le template
- Utiliser HashiCorp Vault ou AWS Secrets Manager
- Tous les secrets doivent venir de gestionnaires sécurisés
- TLS/SSL obligatoire partout
- Authentification stricte
- Audit logging activé
- Conformité GDPR/réglementations

## 🚀 Démarrage Rapide

### Option 1 : Script automatique

```bash
# Configurer l'environnement
./scripts/setup-environment.sh development

# Démarrer les services
make docker-up
# ou
./scripts/start-services.sh

# Vérifier que tout fonctionne
docker-compose -f infrastructure/docker/docker-compose.infrastructure.yml ps
```

### Option 2 : Manuel

```bash
# 1. Copier le fichier d'environnement
cp environments/development/.env .env

# 2. Démarrer Docker Compose
cd infrastructure/docker
docker-compose -f docker-compose.infrastructure.yml up -d

# 3. Vérifier les services
docker-compose ps
```

## 📝 Variables d'Environnement

### Variables Principales

| Variable | Description | Dev | Staging | Prod |
|----------|-------------|-----|---------|------|
| `ENVIRONMENT` | Nom de l'environnement | development | staging | production |
| `LOG_LEVEL` | Niveau de logging | DEBUG | INFO | WARNING |
| `DEBUG` | Mode debug | true | false | false |
| `ENABLE_MOCK_DATA` | Générer données test | true | false | false |

### Bases de Données

| Variable | Description | Exemple |
|----------|-------------|---------|
| `POSTGRES_HOST` | Hôte PostgreSQL | localhost |
| `POSTGRES_PORT` | Port PostgreSQL | 5432 |
| `POSTGRES_DB` | Nom de la base | mantis_dev |
| `POSTGRES_USER` | Utilisateur | mantis |
| `POSTGRES_PASSWORD` | Mot de passe | mantis_dev_password |

### Kafka

| Variable | Description | Exemple |
|----------|-------------|---------|
| `KAFKA_BOOTSTRAP_SERVERS` | Serveurs Kafka | localhost:9092 |
| `KAFKA_GROUP_ID` | Consumer group ID | mantis-dev-consumer |
| `KAFKA_TOPIC_*` | Noms des topics | sensor.raw, features.computed |

### Services

| Variable | Description | Défaut |
|----------|-------------|--------|
| `PORT_INGESTION_IIOT` | Port service ingestion | 8001 |
| `PORT_PREPROCESSING` | Port service preprocessing | 8002 |
| `PORT_FEATURE_EXTRACTION` | Port extraction features | 8003 |
| `PORT_ANOMALY_DETECTION` | Port détection anomalies | 8004 |
| `PORT_RUL_PREDICTION` | Port prédiction RUL | 8005 |
| `PORT_ORCHESTRATOR` | Port orchestrateur | 8006 |
| `PORT_DASHBOARD` | Port dashboard | 3000 |

## 🔒 Sécurité

### Fichiers .env

**✅ À FAIRE** :
- Garder `.env` local (dans .gitignore)
- Utiliser `.env.example` comme template
- Documenter toutes les variables
- Versionner `.env.example`

**❌ NE JAMAIS** :
- Commiter `.env` dans Git
- Partager `.env` par email/Slack
- Hardcoder des secrets dans le code
- Utiliser les mêmes mots de passe partout

### Gestion des Secrets

#### Développement
- Mots de passe simples OK
- Stocker dans `.env` local

#### Staging/Production
- **HashiCorp Vault** (recommandé)
- AWS Secrets Manager
- Azure Key Vault
- Kubernetes Secrets

**Exemple avec Vault** :
```bash
# Stocker un secret
vault kv put secret/mantis/production/postgres password=<STRONG_PASSWORD>

# Récupérer dans l'application
POSTGRES_PASSWORD=$(vault kv get -field=password secret/mantis/production/postgres)
```

### Rotation des Secrets

**Fréquence recommandée** :
- Mots de passe DB : tous les 90 jours
- API Keys : tous les 90 jours
- JWT secrets : tous les 180 jours
- Certificats SSL : avant expiration

## 📊 Différences par Environnement

### Ressources

| Aspect | Development | Staging | Production |
|--------|-------------|---------|------------|
| **CPU** | 2-4 cores | 8 cores | 16+ cores |
| **RAM** | 8 GB | 32 GB | 64+ GB |
| **Storage** | 50 GB | 500 GB | 2+ TB |
| **Kafka Partitions** | 3 | 6 | 12 |
| **DB Connections** | 50 | 100 | 200 |
| **Replicas** | 1 | 2 | 3+ |

### Performance

| Métrique | Development | Staging | Production |
|----------|-------------|---------|------------|
| **Batch Size** | 50 | 200 | 500 |
| **Workers** | 2 | 8 | 16 |
| **Timeout (s)** | 30 | 60 | 120 |
| **Retention (days)** | 7 | 30 | 90-365 |

### Monitoring

| Service | Development | Staging | Production |
|---------|-------------|---------|------------|
| **Prometheus** | Optional | Recommandé | Obligatoire |
| **Grafana** | Optional | Recommandé | Obligatoire |
| **Jaeger** | Optional | Recommandé | Obligatoire |
| **ELK Stack** | Non | Optional | Obligatoire |
| **Alerting** | Non | Email | PagerDuty/Slack |

## 🔄 Migration entre Environnements

### Dev → Staging

```bash
# 1. Tester localement
make test
make docker-build

# 2. Configurer staging
./scripts/setup-environment.sh staging

# 3. Déployer
docker-compose up -d

# 4. Smoke tests
./scripts/run-smoke-tests.sh
```

### Staging → Production

```bash
# 1. Valider tests complets en staging
make test-integration
make test-e2e

# 2. Review sécurité
./scripts/security-audit.sh

# 3. Backup production
./scripts/backup.sh

# 4. Déploiement progressif (Blue-Green ou Canary)
kubectl apply -f infrastructure/kubernetes/production/

# 5. Vérifier monitoring
# - Check Grafana dashboards
# - Vérifier logs
# - Tester endpoints critiques

# 6. Rollback si problème
kubectl rollout undo deployment/mantis-service
```

## 📋 Checklist de Configuration

### Development
- [ ] `.env` copié depuis `environments/development/.env`
- [ ] Docker et Docker Compose installés
- [ ] Services démarrent correctement
- [ ] Accès aux interfaces web (Grafana, MLflow, etc.)
- [ ] Git hooks installés

### Staging
- [ ] `.env` configuré avec mots de passe forts
- [ ] TLS/SSL activé
- [ ] Données de test chargées
- [ ] Monitoring configuré
- [ ] Tests automatiques passent
- [ ] Backups configurés

### Production
- [ ] Secrets dans Vault/Secrets Manager
- [ ] Tous les `<VAULT_SECRET>` remplacés
- [ ] TLS/SSL activé et certificats valides
- [ ] Firewalls configurés
- [ ] Haute disponibilité (HA) activée
- [ ] Monitoring et alerting opérationnels
- [ ] Backups automatiques testés
- [ ] Plan de Disaster Recovery en place
- [ ] Conformité GDPR/réglementations
- [ ] Audit logging activé
- [ ] Tests de charge effectués
- [ ] Documentation à jour

## 🆘 Dépannage

### Variables d'environnement non prises en compte

```bash
# Vérifier que .env existe
ls -la .env

# Vérifier le contenu
cat .env | grep POSTGRES_HOST

# Redémarrer les services
docker-compose down
docker-compose up -d
```

### Services ne démarrent pas

```bash
# Vérifier les logs
docker-compose logs

# Vérifier une variable spécifique
docker-compose config | grep POSTGRES_PASSWORD

# Valider le fichier .env
source .env && echo $POSTGRES_HOST
```

### Conflits de ports

```bash
# Vérifier les ports utilisés
lsof -i :5432
lsof -i :9092

# Modifier dans .env
# PORT_POSTGRES=5433
```

## 📚 Ressources

- [12-Factor App Methodology](https://12factor.net/)
- [HashiCorp Vault](https://www.vaultproject.io/)
- [Docker Compose Environment Variables](https://docs.docker.com/compose/environment-variables/)
- [Kubernetes Secrets](https://kubernetes.io/docs/concepts/configuration/secret/)

---

**Maintenu par** : MANTIS Team - EMSI
**Version** : 1.0.0
**Dernière mise à jour** : 2025-01-22
