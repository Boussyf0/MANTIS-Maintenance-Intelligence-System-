#!/bin/bash
# Script pour démarrer tous les services MANTIS

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "=== Démarrage de MANTIS ==="

# Vérifier Docker
if ! command -v docker &> /dev/null; then
    echo "Erreur: Docker n'est pas installé"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "Erreur: Docker Compose n'est pas installé"
    exit 1
fi

echo "✓ Docker et Docker Compose détectés"

# Naviguer vers le dossier infrastructure
cd "$PROJECT_ROOT/infrastructure/docker"

# Étape 1: Lancer l'infrastructure
echo ""
echo "Étape 1/3: Démarrage de l'infrastructure..."
echo "  - Kafka + Zookeeper"
echo "  - PostgreSQL + TimescaleDB"
echo "  - InfluxDB"
echo "  - MinIO"
echo "  - Redis"
echo "  - MLflow"
echo "  - Grafana + Prometheus"
echo "  - Jaeger"

docker-compose -f docker-compose.infrastructure.yml up -d

echo "Attente du démarrage de l'infrastructure (30s)..."
sleep 30

# Vérifier la santé
echo ""
echo "Vérification de la santé des services..."
docker-compose -f docker-compose.infrastructure.yml ps

# Étape 2: Initialiser les bases de données
echo ""
echo "Étape 2/3: Initialisation des bases de données..."
echo "  Les scripts SQL sont exécutés automatiquement au premier démarrage"
echo "  Vérification de la connectivité PostgreSQL..."

max_retries=30
retry_count=0

while [ $retry_count -lt $max_retries ]; do
    if docker exec mantis-postgres pg_isready -U mantis > /dev/null 2>&1; then
        echo "✓ PostgreSQL prêt"
        break
    fi
    retry_count=$((retry_count + 1))
    echo "  Attente PostgreSQL... ($retry_count/$max_retries)"
    sleep 2
done

if [ $retry_count -eq $max_retries ]; then
    echo "✗ Erreur: PostgreSQL n'a pas démarré à temps"
    exit 1
fi

# Vérifier TimescaleDB
retry_count=0
while [ $retry_count -lt $max_retries ]; do
    if docker exec mantis-timescaledb pg_isready -U mantis > /dev/null 2>&1; then
        echo "✓ TimescaleDB prêt"
        break
    fi
    retry_count=$((retry_count + 1))
    echo "  Attente TimescaleDB... ($retry_count/$max_retries)"
    sleep 2
done

if [ $retry_count -eq $max_retries ]; then
    echo "✗ Erreur: TimescaleDB n'a pas démarré à temps"
    exit 1
fi

# Étape 3: Lancer les microservices (si docker-compose.services.yml existe)
if [ -f "docker-compose.services.yml" ]; then
    echo ""
    echo "Étape 3/3: Démarrage des microservices..."
    docker-compose -f docker-compose.services.yml up -d
    echo "✓ Microservices démarrés"
else
    echo ""
    echo "Étape 3/3: docker-compose.services.yml non trouvé, skip démarrage microservices"
    echo "  Vous pouvez démarrer les services individuellement"
fi

# Afficher les URLs
echo ""
echo "=== MANTIS démarré avec succès! ==="
echo ""
echo "URLs d'accès:"
echo "  - Dashboard Usine:    http://localhost:3000"
echo "  - Ingestion IIoT API: http://localhost:8001"
echo "  - Grafana:            http://localhost:3001 (admin/admin)"
echo "  - MLflow:             http://localhost:5002"
echo "  - Kafka UI:           http://localhost:8082"
echo "  - MinIO Console:      http://localhost:9001 (minioadmin/minioadmin)"
echo "  - Prometheus:         http://localhost:9091"
echo "  - Jaeger UI:          http://localhost:16686"
echo ""
echo "Base de données:"
echo "  - PostgreSQL:   localhost:5432 (mantis/mantis_password)"
echo "  - TimescaleDB:  localhost:5433 (mantis/mantis_password)"
echo "  - InfluxDB:     localhost:8086"
echo "  - Redis:        localhost:6380"
echo ""
echo "Pour voir les logs:"
echo "  docker-compose -f docker-compose.infrastructure.yml logs -f [service]"
echo ""
echo "Pour arrêter:"
echo "  ./scripts/stop-services.sh"
echo ""
