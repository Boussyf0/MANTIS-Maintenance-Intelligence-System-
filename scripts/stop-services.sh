#!/bin/bash
# Script pour arrêter tous les services MANTIS

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "=== Arrêt de MANTIS ==="

cd "$PROJECT_ROOT/infrastructure/docker"

# Arrêter les microservices
if [ -f "docker-compose.services.yml" ]; then
    echo "Arrêt des microservices..."
    docker-compose -f docker-compose.services.yml down
fi

# Arrêter l'infrastructure
echo "Arrêt de l'infrastructure..."
docker-compose -f docker-compose.infrastructure.yml down

echo ""
echo "✓ MANTIS arrêté"
echo ""
echo "Pour supprimer également les volumes (ATTENTION: perte de données):"
echo "  docker-compose -f docker-compose.infrastructure.yml down -v"
echo ""
