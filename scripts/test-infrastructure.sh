#!/bin/bash
# MANTIS - Script de Test Infrastructure
# Vérifie que tous les services sont healthy et fonctionnels

set -e

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}╔═══════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  MANTIS Infrastructure Health Check          ║${NC}"
echo -e "${BLUE}╚═══════════════════════════════════════════════╝${NC}\n"

DOCKER_COMPOSE_FILE="infrastructure/docker/docker-compose.infrastructure.enhanced.yml"
ERRORS=0
WARNINGS=0

# Fonction pour vérifier un service
check_service() {
    local service_name=$1
    local container_name=$2
    local port=$3
    local check_command=$4

    echo -e "${YELLOW}Checking $service_name...${NC}"

    # Vérifier si le conteneur existe
    if ! docker ps --format '{{.Names}}' | grep -q "^${container_name}$"; then
        echo -e "${RED}✗ Container $container_name not running${NC}"
        ((ERRORS++))
        return 1
    fi

    # Vérifier le health status
    health_status=$(docker inspect --format='{{.State.Health.Status}}' "$container_name" 2>/dev/null || echo "no-healthcheck")

    case $health_status in
        "healthy")
            echo -e "${GREEN}  ✓ Container: running${NC}"
            echo -e "${GREEN}  ✓ Health: healthy${NC}"
            ;;
        "unhealthy")
            echo -e "${GREEN}  ✓ Container: running${NC}"
            echo -e "${RED}  ✗ Health: unhealthy${NC}"
            ((ERRORS++))
            return 1
            ;;
        "starting")
            echo -e "${GREEN}  ✓ Container: running${NC}"
            echo -e "${YELLOW}  ⚠ Health: starting (waiting...)${NC}"
            ((WARNINGS++))
            ;;
        "no-healthcheck")
            echo -e "${GREEN}  ✓ Container: running${NC}"
            echo -e "${YELLOW}  ⚠ Health: no healthcheck configured${NC}"
            ((WARNINGS++))
            ;;
    esac

    # Vérifier le port si spécifié
    if [ -n "$port" ]; then
        if nc -z localhost "$port" 2>/dev/null; then
            echo -e "${GREEN}  ✓ Port $port: accessible${NC}"
        else
            echo -e "${RED}  ✗ Port $port: not accessible${NC}"
            ((ERRORS++))
        fi
    fi

    # Exécuter une commande de vérification supplémentaire si fournie
    if [ -n "$check_command" ]; then
        if eval "$check_command" >/dev/null 2>&1; then
            echo -e "${GREEN}  ✓ Additional check: passed${NC}"
        else
            echo -e "${YELLOW}  ⚠ Additional check: failed (non-critical)${NC}"
            ((WARNINGS++))
        fi
    fi

    echo ""
    return 0
}

# Liste des services à vérifier
echo -e "${BLUE}═══════════════════════════════════════════════${NC}"
echo -e "${BLUE}Vérification des services...${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════${NC}\n"

# Message Broker
check_service "Zookeeper" "mantis-zookeeper" "2181" "echo 'ruok' | nc localhost 2181 | grep -q imok"
check_service "Kafka" "mantis-kafka" "9092" "docker exec mantis-kafka kafka-topics --bootstrap-server localhost:9092 --list"
check_service "Kafka UI" "mantis-kafka-ui" "8082"

# Databases
check_service "PostgreSQL" "mantis-postgres" "5432" "docker exec mantis-postgres pg_isready -U mantis"
check_service "TimescaleDB" "mantis-timescaledb" "5433" "docker exec mantis-timescaledb pg_isready -U mantis"
check_service "InfluxDB" "mantis-influxdb" "8086"
check_service "Redis" "mantis-redis" "6380" "docker exec mantis-redis redis-cli --raw incr ping"

# Storage
check_service "MinIO" "mantis-minio" "9000"

# MLOps
check_service "MLflow" "mantis-mlflow" "5002"

# Monitoring
check_service "Prometheus" "mantis-prometheus" "9091"
check_service "Grafana" "mantis-grafana" "3001"
check_service "Jaeger" "mantis-jaeger" "16686"

# Résumé
echo -e "${BLUE}═══════════════════════════════════════════════${NC}"
echo -e "${BLUE}Résumé des vérifications${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════${NC}\n"

TOTAL_SERVICES=12
RUNNING_SERVICES=$((TOTAL_SERVICES - ERRORS))

echo -e "Services running: ${GREEN}$RUNNING_SERVICES${NC}/$TOTAL_SERVICES"
echo -e "Errors: ${RED}$ERRORS${NC}"
echo -e "Warnings: ${YELLOW}$WARNINGS${NC}\n"

if [ $ERRORS -eq 0 ]; then
    echo -e "${GREEN}✓ Tous les services critiques sont opérationnels!${NC}"

    if [ $WARNINGS -gt 0 ]; then
        echo -e "${YELLOW}⚠ Il y a $WARNINGS avertissement(s) non-critiques${NC}"
    fi

    echo -e "\n${BLUE}URLs d'accès:${NC}"
    echo -e "  • Kafka UI:        ${GREEN}http://localhost:8082${NC}"
    echo -e "  • PostgreSQL:      ${GREEN}localhost:5432${NC} (user: mantis)"
    echo -e "  • TimescaleDB:     ${GREEN}localhost:5433${NC} (user: mantis)"
    echo -e "  • InfluxDB:        ${GREEN}http://localhost:8086${NC}"
    echo -e "  • Redis:           ${GREEN}localhost:6380${NC}"
    echo -e "  • MinIO Console:   ${GREEN}http://localhost:9001${NC} (minioadmin/minioadmin)"
    echo -e "  • MLflow:          ${GREEN}http://localhost:5002${NC}"
    echo -e "  • Prometheus:      ${GREEN}http://localhost:9091${NC}"
    echo -e "  • Grafana:         ${GREEN}http://localhost:3001${NC} (admin/admin)"
    echo -e "  • Jaeger UI:       ${GREEN}http://localhost:16686${NC}"

    exit 0
else
    echo -e "${RED}✗ $ERRORS service(s) en échec${NC}"
    echo -e "\n${YELLOW}Pour débugger:${NC}"
    echo -e "  1. Voir les logs: ${BLUE}docker-compose -f $DOCKER_COMPOSE_FILE logs [service-name]${NC}"
    echo -e "  2. Vérifier l'état: ${BLUE}docker-compose -f $DOCKER_COMPOSE_FILE ps${NC}"
    echo -e "  3. Redémarrer: ${BLUE}docker-compose -f $DOCKER_COMPOSE_FILE restart [service-name]${NC}"

    exit 1
fi
