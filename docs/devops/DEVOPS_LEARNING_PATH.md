# 🎓 Parcours d'Apprentissage DevOps/Data Engineer - MANTIS

> **Votre rôle**: Infrastructure, Monitoring, Data Engineering
> **Durée estimée**: 90 heures sur 10 semaines
> **Niveau**: Intermédiaire à Avancé

---

## 📊 Progression Actuelle

| Phase | Statut | Temps prévu | Temps réel | Completion |
|-------|--------|-------------|------------|------------|
| Phase 1: Docker Compose | 🟡 En cours | 10h | - | 60% |
| Phase 2: Bases de données | ⚪ À faire | 12h | - | 0% |
| Phase 3: Prometheus | ⚪ À faire | 10h | - | 0% |
| Phase 4: Grafana Dashboards | ⚪ À faire | 15h | - | 0% |
| Phase 5: Jaeger Tracing | ⚪ À faire | 8h | - | 0% |
| Phase 6: Scripts ETL | ⚪ À faire | 12h | - | 0% |
| Phase 7: CI/CD | ⚪ À faire | 10h | - | 0% |
| Phase 8: Documentation | ⚪ À faire | 8h | - | 0% |
| Phase 9: Tests | ⚪ À faire | 5h | - | 0% |

**Total**: 90 heures | **Complété**: ~15h (~17%)

---

## 🎯 Phase 1: Configuration Docker Compose Infrastructure

### Ce que vous avez déjà ✅

```yaml
# 12 services déjà configurés:
✅ Zookeeper          # Coordination Kafka
✅ Kafka              # Message broker
✅ Kafka UI           # Interface Kafka
✅ PostgreSQL         # Métadonnées
✅ TimescaleDB        # Time series
✅ InfluxDB           # High frequency data
✅ Redis              # Cache + Feature store
✅ MinIO              # Object storage
✅ MLflow             # ML tracking
✅ Prometheus         # Métriques
✅ Grafana            # Dashboards
✅ Jaeger             # Tracing distribué
```

### Ce qu'il faut améliorer 🔄

#### 1.1 Ajouter Health Checks Robustes

**Pourquoi?** Les health checks permettent à Docker de:
- Vérifier qu'un service est vraiment prêt (pas juste démarré)
- Redémarrer automatiquement les services en échec
- Bloquer les dépendances jusqu'à ce qu'un service soit healthy

**Exemple à implémenter pour Kafka**:

```yaml
kafka:
  # ... configuration existante ...
  healthcheck:
    test: ["CMD-SHELL", "kafka-broker-api-versions --bootstrap-server localhost:9092 || exit 1"]
    interval: 10s
    timeout: 5s
    retries: 5
    start_period: 30s
```

**📝 Exercice 1**: Ajoutez des health checks pour tous les services

#### 1.2 Améliorer les depends_on avec conditions

**Problème actuel**: `depends_on` démarre les services dans l'ordre mais ne vérifie pas qu'ils sont prêts.

**Solution**: Utiliser `condition: service_healthy`

```yaml
kafka:
  depends_on:
    zookeeper:
      condition: service_healthy  # ← Attend que Zookeeper soit healthy
```

**📝 Exercice 2**: Mettez à jour toutes les dépendances avec `condition`

#### 1.3 Configurer les Restart Policies

**Apprentissage**: Comprendre les différentes politiques:

| Policy | Description | Cas d'usage |
|--------|-------------|-------------|
| `no` | Ne jamais redémarrer | Développement |
| `always` | Toujours redémarrer | Services critiques |
| `on-failure` | Redémarrer si erreur | Services non-critiques |
| `unless-stopped` | Redémarrer sauf si arrêt manuel | Production |

```yaml
kafka:
  restart: unless-stopped  # ← Production
  # restart: on-failure:3  # ← Max 3 tentatives
```

**📝 Exercice 3**: Ajoutez des restart policies appropriées

#### 1.4 Optimiser les Ressources (Limits & Reservations)

**Concept**: Éviter qu'un service consomme toutes les ressources

```yaml
kafka:
  deploy:
    resources:
      limits:
        cpus: '2.0'      # Max 2 CPUs
        memory: 4G       # Max 4GB RAM
      reservations:
        cpus: '0.5'      # Min réservé
        memory: 1G       # Min réservé
```

**📝 Exercice 4**: Définissez des limites pour chaque service

#### 1.5 Sécuriser avec Secrets

**Problème**: Mots de passe en clair dans le fichier

**Solution**: Utiliser Docker secrets ou .env

```yaml
# Créer un fichier .env.infrastructure
POSTGRES_PASSWORD=changeme123
REDIS_PASSWORD=secret456
MINIO_ACCESS_KEY=minioadmin
```

```yaml
# Dans docker-compose
postgres:
  environment:
    POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}  # ← Lit depuis .env
```

**📝 Exercice 5**: Externalisez tous les secrets

---

### 🛠️ Travaux Pratiques Phase 1

#### TP1.1: Tester le Démarrage Actuel

```bash
# 1. Démarrer l'infrastructure
cd infrastructure/docker
docker-compose -f docker-compose.infrastructure.yml up -d

# 2. Vérifier l'état des services
docker-compose ps

# 3. Vérifier les logs
docker-compose logs kafka | tail -20

# 4. Tester la connexion Kafka
docker exec -it mantis-kafka kafka-topics --bootstrap-server localhost:9092 --list
```

**Question**: Combien de temps faut-il pour que tous les services soient ready?

#### TP1.2: Améliorer le Health Check de PostgreSQL

```yaml
postgres:
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U mantis"]
    interval: 5s
    timeout: 3s
    retries: 5
    start_period: 10s
```

**Question**: Pourquoi `start_period: 10s`?
<details>
<summary>Réponse</summary>
PostgreSQL met du temps à initialiser la base. Pendant ce temps, on ne compte pas les échecs.
</details>

#### TP1.3: Créer un Script de Health Check Global

Créez `scripts/check-infrastructure-health.sh`:

```bash
#!/bin/bash

echo "🔍 Vérification santé infrastructure MANTIS..."

services=(
    "zookeeper:2181"
    "kafka:9092"
    "postgres:5432"
    "timescaledb:5433"
    "influxdb:8086"
    "redis:6379"
    "minio:9000"
    "mlflow:5000"
    "prometheus:9090"
    "grafana:3001"
)

for service in "${services[@]}"; do
    name="${service%%:*}"
    port="${service##*:}"

    if nc -z localhost "$port" 2>/dev/null; then
        echo "✅ $name (port $port) - OK"
    else
        echo "❌ $name (port $port) - KO"
    fi
done
```

**📝 Exercice**: Exécutez ce script et identifiez les services en échec

---

## 🎯 Phase 2: Configuration Bases de Données (À Venir)

### Objectifs d'Apprentissage

1. **PostgreSQL**:
   - Créer schéma initial
   - Configurer migrations (Flyway)
   - Optimiser performance (indexes, partitioning)
   - Backup automatique

2. **TimescaleDB**:
   - Créer hypertables
   - Configurer continuous aggregates
   - Policies de compression
   - Policies de rétention

3. **InfluxDB**:
   - Créer buckets
   - Configurer downsampling
   - Optimiser write throughput

4. **Redis**:
   - Configurer persistence (RDB + AOF)
   - Configurer eviction policies
   - Setup Redis Cluster (optionnel)

### Prérequis Théoriques

Avant de commencer la Phase 2, vous devez comprendre:

#### 📖 Concept 1: ACID vs BASE

| Propriété | PostgreSQL (ACID) | Redis (BASE) |
|-----------|-------------------|--------------|
| **Atomicity** | ✅ Transactions complètes ou rien | ⚠️ Atomicité limitée |
| **Consistency** | ✅ Contraintes respectées | ⚠️ Eventually consistent |
| **Isolation** | ✅ Transactions isolées | ⚠️ Pas d'isolation |
| **Durability** | ✅ Données persisted | ⚠️ Optionnel (RDB/AOF) |

**Cas d'usage**:
- PostgreSQL → Données critiques (work orders, assets)
- Redis → Cache, sessions, feature store temporaire

#### 📖 Concept 2: Time Series Databases

**Pourquoi TimescaleDB et InfluxDB?**

| Critère | TimescaleDB | InfluxDB |
|---------|-------------|----------|
| **Langage** | SQL (PostgreSQL) | InfluxQL / Flux |
| **Write speed** | ~100K rows/s | ~500K rows/s |
| **Queries complexes** | ✅ Excellent (JOINs) | ⚠️ Limité |
| **Compression** | ✅ Oui (columnar) | ✅ Oui |
| **Cas d'usage MANTIS** | Features agrégées | Raw sensor data |

**Règle de décision**:
- Fréquence > 100Hz → InfluxDB
- Fréquence < 100Hz + JOINs nécessaires → TimescaleDB

---

## 🎯 Phase 3: Prometheus & Alerting (À Venir)

### Ce que vous allez apprendre

1. **Métriques**:
   - Types: Counter, Gauge, Histogram, Summary
   - Labels et cardinality
   - PromQL (langage de requêtes)

2. **Scraping**:
   - Service discovery
   - Relabeling
   - Métriques JVM (JMX)
   - Métriques Python (prometheus-client)

3. **Alerting**:
   - Alerting rules
   - Alertmanager
   - Routage par criticité
   - Notifications (Slack, Email)

### Exemple de Métrique à Créer

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'mantis-ingestion'
    static_configs:
      - targets: ['ingestion-iiot:8001']
    metrics_path: '/actuator/prometheus'  # Spring Boot Actuator
```

```java
// Dans le service Java
@Timed(value = "kafka.send.latency", description = "Kafka send latency")
public CompletableFuture<SendResult> sendSensorData(SensorData data) {
    // Micrometer va automatiquement mesurer la latence
}
```

---

## 🎯 Phase 4: Grafana Dashboards (À Venir)

### Les 5 Dashboards à Créer

1. **Dashboard Infrastructure** (Système)
   - CPU, RAM, Disk par conteneur
   - Network I/O
   - État des services

2. **Dashboard Kafka** (Message Broker)
   - Throughput (messages/s)
   - Consumer lag
   - Partition distribution

3. **Dashboard Bases de Données** (Storage)
   - Connections actives
   - Query latency
   - Cache hit ratio (Redis)

4. **Dashboard Applicatif** (Services)
   - Request rate par service
   - Latence P50/P95/P99
   - Error rate

5. **Dashboard Métier** (KPIs)
   - MTBF (Mean Time Between Failures)
   - MTTR (Mean Time To Repair)
   - OEE (Overall Equipment Effectiveness)

### Compétences à Acquérir

- [ ] PromQL avancé
- [ ] Variables dans Grafana
- [ ] Alerting dans Grafana
- [ ] Annotations
- [ ] Templating

---

## 📚 Ressources d'Apprentissage

### Documentation Officielle

| Technologie | URL | Priorité |
|-------------|-----|----------|
| Docker Compose | https://docs.docker.com/compose/ | 🔥🔥🔥 |
| Prometheus | https://prometheus.io/docs/ | 🔥🔥 |
| Grafana | https://grafana.com/docs/ | 🔥🔥 |
| TimescaleDB | https://docs.timescale.com/ | 🔥 |
| Kafka | https://kafka.apache.org/documentation/ | 🔥 |

### Tutoriels Recommandés

1. **Docker Health Checks**: https://docs.docker.com/engine/reference/builder/#healthcheck
2. **Prometheus Best Practices**: https://prometheus.io/docs/practices/naming/
3. **Grafana Provisioning**: https://grafana.com/docs/grafana/latest/administration/provisioning/

---

## ✅ Checklist de Compétences DevOps

Cochez au fur et à mesure:

### Docker & Compose
- [ ] Comprendre les images vs conteneurs
- [ ] Maîtriser le networking (bridge, host, overlay)
- [ ] Configurer volumes (bind mount vs named volumes)
- [ ] Écrire des health checks efficaces
- [ ] Optimiser les Dockerfiles (multi-stage builds)
- [ ] Utiliser docker-compose profiles

### Monitoring
- [ ] Comprendre les 4 Golden Signals (latency, traffic, errors, saturation)
- [ ] Écrire des requêtes PromQL
- [ ] Configurer des alerting rules
- [ ] Créer des dashboards Grafana
- [ ] Analyser les traces distribuées (Jaeger)

### Databases
- [ ] Optimiser PostgreSQL (indexes, vacuum, analyze)
- [ ] Configurer TimescaleDB hypertables
- [ ] Utiliser InfluxDB pour time series
- [ ] Configurer Redis persistence
- [ ] Faire des backups automatiques

### CI/CD
- [ ] Écrire des GitHub Actions workflows
- [ ] Configurer des tests automatisés
- [ ] Builder et pusher des Docker images
- [ ] Déployer automatiquement
- [ ] Rollback en cas d'échec

---

## 🎯 Prochaine Session

**Quand vous êtes prêt, nous allons:**

1. ✅ **Finaliser la Phase 1**: Améliorer le Docker Compose avec health checks
2. 🔧 **Démarrer la Phase 2**: Configurer les bases de données avec optimisations
3. 📊 **Créer votre premier dashboard Grafana** pour surveiller l'infrastructure

**Questions pour vous**:

1. Voulez-vous commencer par:
   - a) Améliorer le Docker Compose actuel
   - b) Passer directement à la Phase 2 (Databases)
   - c) Sauter à la Phase 4 (Grafana) car c'est plus visuel

2. Préférez-vous:
   - a) Code complet avec explications détaillées
   - b) Guidance + vous codez vous-même
   - c) Exercices progressifs avec corrections

3. Avez-vous des questions sur un concept en particulier?

---

**Prêt à devenir un expert DevOps?** 🚀

Dites-moi par où vous voulez commencer et je vous guide étape par étape!
