#!/bin/bash
# Script de validation complète du projet MANTIS

set -e

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}╔═══════════════════════════════════════╗${NC}"
echo -e "${BLUE}║   MANTIS - Validation du Projet      ║${NC}"
echo -e "${BLUE}╚═══════════════════════════════════════╝${NC}\n"

ERRORS=0
WARNINGS=0

# 1. Structure des répertoires
echo -e "${YELLOW}[1/8]${NC} Vérification de la structure des répertoires..."

REQUIRED_DIRS=(
    "services"
    "infrastructure/docker"
    "data"
    "scripts"
    "tests"
    "docs"
)

for dir in "${REQUIRED_DIRS[@]}"; do
    if [ -d "$dir" ]; then
        echo -e "${GREEN}✓${NC} $dir"
    else
        echo -e "${RED}✗${NC} $dir (manquant)"
        ERRORS=$((ERRORS + 1))
    fi
done

# 2. Fichiers de configuration
echo -e "\n${YELLOW}[2/8]${NC} Vérification des fichiers de configuration..."

REQUIRED_FILES=(
    "README.md"
    "ARCHITECTURE.md"
    "CONTRIBUTING.md"
    "Makefile"
    ".gitignore"
    "requirements.txt"
)

for file in "${REQUIRED_FILES[@]}"; do
    if [ -f "$file" ]; then
        echo -e "${GREEN}✓${NC} $file"
    else
        echo -e "${RED}✗${NC} $file (manquant)"
        ERRORS=$((ERRORS + 1))
    fi
done

# 3. Services Java
echo -e "\n${YELLOW}[3/8]${NC} Validation des services Java..."

JAVA_SERVICES=$(find services -name "pom.xml" -type f | sed 's|/pom.xml||')

if [ -z "$JAVA_SERVICES" ]; then
    echo -e "${YELLOW}⚠${NC} Aucun service Java trouvé"
    WARNINGS=$((WARNINGS + 1))
else
    for service in $JAVA_SERVICES; do
        echo -e "\n  Service: ${BLUE}$service${NC}"

        # Vérifier la structure
        if [ -d "$service/src/main/java" ]; then
            echo -e "    ${GREEN}✓${NC} Structure Maven correcte"
        else
            echo -e "    ${RED}✗${NC} Structure Maven incorrecte"
            ERRORS=$((ERRORS + 1))
        fi

        # Compilation
        if command -v mvn &> /dev/null; then
            echo -e "    Compilation en cours..."
            if (cd "$service" && mvn clean compile -q); then
                echo -e "    ${GREEN}✓${NC} Compilation réussie"
            else
                echo -e "    ${RED}✗${NC} Échec de la compilation"
                ERRORS=$((ERRORS + 1))
            fi

            # Tests
            echo -e "    Tests en cours..."
            if (cd "$service" && mvn test -q); then
                echo -e "    ${GREEN}✓${NC} Tests passés"
            else
                echo -e "    ${RED}✗${NC} Tests échoués"
                ERRORS=$((ERRORS + 1))
            fi
        else
            echo -e "    ${YELLOW}⚠${NC} Maven non installé, validation ignorée"
            WARNINGS=$((WARNINGS + 1))
        fi
    done
fi

# 4. Services Python
echo -e "\n${YELLOW}[4/8]${NC} Validation des services Python..."

PYTHON_SERVICES=$(find services -name "requirements.txt" -type f | sed 's|/requirements.txt||' | grep -v "^requirements.txt$" || true)

if [ -z "$PYTHON_SERVICES" ]; then
    echo -e "${YELLOW}⚠${NC} Aucun service Python trouvé (normal si en développement)"
else
    for service in $PYTHON_SERVICES; do
        echo -e "\n  Service: ${BLUE}$service${NC}"

        # Vérifier la structure
        if [ -f "$service/main.py" ] || [ -d "$service/src" ]; then
            echo -e "    ${GREEN}✓${NC} Structure Python correcte"
        else
            echo -e "    ${YELLOW}⚠${NC} Pas de main.py ou src/"
            WARNINGS=$((WARNINGS + 1))
        fi

        # Vérifier les dépendances
        if command -v pip &> /dev/null; then
            echo -e "    ${GREEN}✓${NC} pip disponible"
        else
            echo -e "    ${YELLOW}⚠${NC} pip non disponible"
            WARNINGS=$((WARNINGS + 1))
        fi
    done
fi

# 5. Infrastructure Docker
echo -e "\n${YELLOW}[5/8]${NC} Validation de l'infrastructure Docker..."

if [ -f "infrastructure/docker/docker-compose.infrastructure.yml" ]; then
    echo -e "${GREEN}✓${NC} docker-compose.infrastructure.yml présent"

    # Valider le fichier YAML
    if command -v docker-compose &> /dev/null; then
        if docker-compose -f infrastructure/docker/docker-compose.infrastructure.yml config > /dev/null 2>&1; then
            echo -e "${GREEN}✓${NC} Configuration Docker Compose valide"
        else
            echo -e "${RED}✗${NC} Configuration Docker Compose invalide"
            ERRORS=$((ERRORS + 1))
        fi
    else
        echo -e "${YELLOW}⚠${NC} docker-compose non installé"
        WARNINGS=$((WARNINGS + 1))
    fi
else
    echo -e "${RED}✗${NC} docker-compose.infrastructure.yml manquant"
    ERRORS=$((ERRORS + 1))
fi

# 6. Scripts
echo -e "\n${YELLOW}[6/8]${NC} Validation des scripts..."

SCRIPTS=(
    "scripts/start-services.sh"
    "scripts/stop-services.sh"
    "scripts/download-cmapss.sh"
)

for script in "${SCRIPTS[@]}"; do
    if [ -f "$script" ]; then
        if [ -x "$script" ]; then
            echo -e "${GREEN}✓${NC} $script (exécutable)"
        else
            echo -e "${YELLOW}⚠${NC} $script (non exécutable)"
            WARNINGS=$((WARNINGS + 1))
        fi
    else
        echo -e "${RED}✗${NC} $script (manquant)"
        ERRORS=$((ERRORS + 1))
    fi
done

# 7. Git Hooks
echo -e "\n${YELLOW}[7/8]${NC} Validation des Git Hooks..."

HOOKS=(
    ".githooks/pre-commit"
    ".githooks/commit-msg"
    ".githooks/pre-push"
)

for hook in "${HOOKS[@]}"; do
    if [ -f "$hook" ]; then
        if [ -x "$hook" ]; then
            echo -e "${GREEN}✓${NC} $hook"
        else
            echo -e "${YELLOW}⚠${NC} $hook (non exécutable, exécutez: chmod +x $hook)"
            WARNINGS=$((WARNINGS + 1))
        fi
    else
        echo -e "${RED}✗${NC} $hook (manquant)"
        ERRORS=$((ERRORS + 1))
    fi
done

# Vérifier si les hooks sont configurés
HOOKS_PATH=$(git config core.hooksPath || echo "")
if [ "$HOOKS_PATH" = ".githooks" ]; then
    echo -e "${GREEN}✓${NC} Hooks Git configurés (.githooks)"
else
    echo -e "${YELLOW}⚠${NC} Hooks non configurés (exécutez: ./scripts/install-hooks.sh)"
    WARNINGS=$((WARNINGS + 1))
fi

# 8. GitHub Actions
echo -e "\n${YELLOW}[8/8]${NC} Validation des GitHub Actions..."

if [ -f ".github/workflows/ci.yml" ]; then
    echo -e "${GREEN}✓${NC} CI workflow configuré"
else
    echo -e "${YELLOW}⚠${NC} CI workflow manquant"
    WARNINGS=$((WARNINGS + 1))
fi

# Résumé
echo -e "\n${BLUE}═══════════════════════════════════════${NC}"
echo -e "${BLUE}         RÉSUMÉ DE LA VALIDATION       ${NC}"
echo -e "${BLUE}═══════════════════════════════════════${NC}"

if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo -e "${GREEN}✓ Projet entièrement valide!${NC}"
    echo -e "${GREEN}✓ 0 erreurs, 0 avertissements${NC}"
    exit 0
elif [ $ERRORS -eq 0 ]; then
    echo -e "${YELLOW}⚠ $WARNINGS avertissement(s)${NC}"
    echo -e "${GREEN}✓ 0 erreurs${NC}"
    exit 0
else
    echo -e "${RED}✗ $ERRORS erreur(s)${NC}"
    echo -e "${YELLOW}⚠ $WARNINGS avertissement(s)${NC}"
    echo -e "\n${RED}La validation a échoué. Corrigez les erreurs ci-dessus.${NC}"
    exit 1
fi
