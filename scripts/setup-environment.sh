#!/bin/bash
# Script de configuration d'environnement MANTIS

set -e

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${BLUE}╔════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║    MANTIS - Configuration d'Environnement         ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════╝${NC}\n"

# Vérifier qu'on est à la racine du projet
if [ ! -f "README.md" ]; then
    echo -e "${RED}✗ Erreur: Exécutez ce script depuis la racine du projet${NC}"
    exit 1
fi

# Fonction d'aide
function show_help {
    echo -e "${CYAN}Usage:${NC}"
    echo -e "  ./scripts/setup-environment.sh <environment>"
    echo -e ""
    echo -e "${CYAN}Environnements disponibles:${NC}"
    echo -e "  ${GREEN}development${NC}  - Environnement de développement local"
    echo -e "  ${YELLOW}staging${NC}      - Environnement de pré-production"
    echo -e "  ${RED}production${NC}   - Environnement de production"
    echo -e ""
    echo -e "${CYAN}Exemples:${NC}"
    echo -e "  ./scripts/setup-environment.sh development"
    echo -e "  ./scripts/setup-environment.sh staging"
    echo -e ""
}

# Vérifier les arguments
if [ $# -eq 0 ]; then
    show_help
    exit 1
fi

ENV=$1

# Valider l'environnement
case $ENV in
    development|dev)
        ENV="development"
        ENV_COLOR=$GREEN
        ;;
    staging|stg)
        ENV="staging"
        ENV_COLOR=$YELLOW
        ;;
    production|prod)
        ENV="production"
        ENV_COLOR=$RED
        ;;
    *)
        echo -e "${RED}✗ Environnement invalide: $ENV${NC}"
        show_help
        exit 1
        ;;
esac

echo -e "${ENV_COLOR}Environnement sélectionné: $ENV${NC}\n"

# Confirmation pour production
if [ "$ENV" = "production" ]; then
    echo -e "${RED}⚠️  ATTENTION: Configuration pour PRODUCTION${NC}"
    read -p "Êtes-vous sûr de vouloir configurer l'environnement de PRODUCTION? (yes/no) " -r
    echo
    if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
        echo -e "${YELLOW}Configuration annulée${NC}"
        exit 0
    fi
fi

# 1. Vérifier les fichiers d'environnement
echo -e "${YELLOW}[1/6]${NC} Vérification des fichiers d'environnement..."

ENV_DIR="environments/$ENV"
ENV_FILE="$ENV_DIR/.env"

if [ ! -d "$ENV_DIR" ]; then
    echo -e "${RED}✗ Répertoire d'environnement non trouvé: $ENV_DIR${NC}"
    exit 1
fi

if [ "$ENV" = "production" ]; then
    ENV_FILE="$ENV_DIR/.env.template"
    if [ ! -f "$ENV_FILE" ]; then
        echo -e "${RED}✗ Fichier d'environnement non trouvé: $ENV_FILE${NC}"
        exit 1
    fi
    echo -e "${YELLOW}⚠ Production: utiliser le template et configurer les secrets${NC}"
else
    if [ ! -f "$ENV_FILE" ]; then
        echo -e "${RED}✗ Fichier d'environnement non trouvé: $ENV_FILE${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓${NC} Fichier d'environnement trouvé"
fi

# 2. Copier le fichier .env à la racine
echo -e "\n${YELLOW}[2/6]${NC} Configuration du fichier .env..."

# Backup de l'ancien .env si existe
if [ -f ".env" ]; then
    BACKUP_FILE=".env.backup.$(date +%Y%m%d_%H%M%S)"
    mv .env "$BACKUP_FILE"
    echo -e "${YELLOW}⚠${NC} Ancien .env sauvegardé: $BACKUP_FILE"
fi

# Copier le nouveau
cp "$ENV_FILE" .env
echo -e "${GREEN}✓${NC} Fichier .env configuré pour $ENV"

# 3. Vérifier les dépendances
echo -e "\n${YELLOW}[3/6]${NC} Vérification des dépendances..."

# Docker
if command -v docker &> /dev/null; then
    DOCKER_VERSION=$(docker --version)
    echo -e "${GREEN}✓${NC} Docker: $DOCKER_VERSION"
else
    echo -e "${RED}✗${NC} Docker non installé"
fi

# Docker Compose
if command -v docker-compose &> /dev/null; then
    COMPOSE_VERSION=$(docker-compose --version)
    echo -e "${GREEN}✓${NC} Docker Compose: $COMPOSE_VERSION"
else
    echo -e "${RED}✗${NC} Docker Compose non installé"
fi

# Python
if command -v python3 &> /dev/null; then
    PYTHON_VERSION=$(python3 --version)
    echo -e "${GREEN}✓${NC} Python: $PYTHON_VERSION"
else
    echo -e "${YELLOW}⚠${NC} Python non installé (optionnel)"
fi

# Java/Maven
if command -v mvn &> /dev/null; then
    MAVEN_VERSION=$(mvn -version | head -n1)
    echo -e "${GREEN}✓${NC} Maven: $MAVEN_VERSION"
else
    echo -e "${YELLOW}⚠${NC} Maven non installé (optionnel pour services Java)"
fi

# 4. Créer les répertoires nécessaires
echo -e "\n${YELLOW}[4/6]${NC} Création des répertoires de données..."

mkdir -p data/raw
mkdir -p data/processed
mkdir -p data/models
mkdir -p logs
mkdir -p backups

echo -e "${GREEN}✓${NC} Répertoires créés"

# 5. Configurer Docker Compose
echo -e "\n${YELLOW}[5/6]${NC} Configuration Docker Compose..."

COMPOSE_FILE="infrastructure/docker/docker-compose.infrastructure.yml"

if [ -f "$COMPOSE_FILE" ]; then
    echo -e "${GREEN}✓${NC} Docker Compose trouvé: $COMPOSE_FILE"
else
    echo -e "${RED}✗${NC} Docker Compose non trouvé"
fi

# 6. Afficher le résumé
echo -e "\n${YELLOW}[6/6]${NC} Résumé de la configuration..."

echo -e "\n${CYAN}Configuration:${NC}"
echo -e "  Environnement: ${ENV_COLOR}$ENV${NC}"
echo -e "  Fichier .env: ${GREEN}.env${NC} (copié depuis $ENV_FILE)"
echo -e "  Docker Compose: ${GREEN}$COMPOSE_FILE${NC}"

# Lire quelques variables importantes du .env
if [ -f ".env" ]; then
    echo -e "\n${CYAN}Variables principales:${NC}"
    echo -e "  ENVIRONMENT: $(grep "^ENVIRONMENT=" .env | cut -d'=' -f2)"
    echo -e "  LOG_LEVEL: $(grep "^LOG_LEVEL=" .env | cut -d'=' -f2)"
    echo -e "  KAFKA_BOOTSTRAP_SERVERS: $(grep "^KAFKA_BOOTSTRAP_SERVERS=" .env | cut -d'=' -f2)"
    echo -e "  POSTGRES_HOST: $(grep "^POSTGRES_HOST=" .env | cut -d'=' -f2)"
fi

# Instructions suivantes
echo -e "\n${BLUE}╔════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║         CONFIGURATION TERMINÉE ✓                   ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════╝${NC}\n"

echo -e "${CYAN}Prochaines étapes:${NC}\n"

if [ "$ENV" = "development" ]; then
    echo -e "  1. Démarrer l'infrastructure:"
    echo -e "     ${GREEN}make docker-up${NC}"
    echo -e "     ${GREEN}# ou${NC}"
    echo -e "     ${GREEN}./scripts/start-services.sh${NC}"
    echo -e ""
    echo -e "  2. Vérifier les services:"
    echo -e "     ${GREEN}docker-compose -f infrastructure/docker/docker-compose.infrastructure.yml ps${NC}"
    echo -e ""
    echo -e "  3. Accéder aux interfaces:"
    echo -e "     - Grafana: ${BLUE}http://localhost:3001${NC} (admin/admin)"
    echo -e "     - MLflow: ${BLUE}http://localhost:5000${NC}"
    echo -e "     - Kafka UI: ${BLUE}http://localhost:8080${NC}"
    echo -e ""
elif [ "$ENV" = "staging" ]; then
    echo -e "  1. Vérifier les mots de passe dans .env"
    echo -e "     ${YELLOW}⚠  Remplacer <CHANGE_ME_*> par de vraies valeurs${NC}"
    echo -e ""
    echo -e "  2. Démarrer les services"
    echo -e "     ${GREEN}make docker-up${NC}"
    echo -e ""
    echo -e "  3. Vérifier les logs"
    echo -e "     ${GREEN}docker-compose logs -f${NC}"
    echo -e ""
elif [ "$ENV" = "production" ]; then
    echo -e "  ${RED}⚠️  ATTENTION PRODUCTION ⚠️${NC}"
    echo -e ""
    echo -e "  1. ${RED}NE PAS${NC} utiliser directement le fichier .env.template"
    echo -e "     - Utiliser HashiCorp Vault ou AWS Secrets Manager"
    echo -e "     - Remplacer ${RED}TOUS${NC} les <VAULT_SECRET>"
    echo -e ""
    echo -e "  2. Vérifier la checklist de sécurité dans .env.template"
    echo -e "     - TLS/SSL activé partout"
    echo -e "     - Mots de passe forts (min 32 caractères)"
    echo -e "     - Clés JWT générées (openssl rand -hex 32)"
    echo -e "     - Firewalls configurés"
    echo -e "     - Backups automatiques"
    echo -e ""
    echo -e "  3. Déployer via Kubernetes"
    echo -e "     ${GREEN}kubectl apply -f infrastructure/kubernetes/${NC}"
    echo -e ""
    echo -e "  4. Configurer monitoring et alerting"
    echo -e "     - Prometheus & Grafana"
    echo -e "     - PagerDuty / Slack"
    echo -e "     - Logs centralisés (ELK)"
    echo -e ""
fi

echo -e "${GREEN}Configuration d'environnement terminée! 🎉${NC}\n"
