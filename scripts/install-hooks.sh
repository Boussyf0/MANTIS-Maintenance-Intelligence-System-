#!/bin/bash
# Script d'installation des hooks Git pour MANTIS

set -e

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}╔═══════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  MANTIS - Installation des Git Hooks ║${NC}"
echo -e "${BLUE}╚═══════════════════════════════════════╝${NC}\n"

# Vérifier qu'on est dans un repo Git
if [ ! -d ".git" ]; then
    echo -e "${RED}✗ Erreur: Ce script doit être exécuté à la racine du repository Git${NC}"
    exit 1
fi

# Configurer le répertoire des hooks
echo -e "${YELLOW}[1/4]${NC} Configuration du répertoire des hooks..."
git config core.hooksPath .githooks
echo -e "${GREEN}✓${NC} Répertoire configuré: .githooks"

# Rendre les hooks exécutables
echo -e "\n${YELLOW}[2/4]${NC} Permissions des hooks..."
chmod +x .githooks/*
echo -e "${GREEN}✓${NC} Hooks rendus exécutables"

# Vérifier les dépendances
echo -e "\n${YELLOW}[3/4]${NC} Vérification des dépendances..."

# Java & Maven
if command -v mvn &> /dev/null; then
    echo -e "${GREEN}✓${NC} Maven installé: $(mvn -version | head -n1)"
else
    echo -e "${YELLOW}⚠${NC} Maven non installé (requis pour tests Java)"
fi

# Python
if command -v python3 &> /dev/null; then
    echo -e "${GREEN}✓${NC} Python installé: $(python3 --version)"
else
    echo -e "${YELLOW}⚠${NC} Python non installé (requis pour tests Python)"
fi

# flake8
if command -v flake8 &> /dev/null; then
    echo -e "${GREEN}✓${NC} flake8 installé"
else
    echo -e "${YELLOW}⚠${NC} flake8 non installé (pip install flake8)"
fi

# black
if command -v black &> /dev/null; then
    echo -e "${GREEN}✓${NC} black installé"
else
    echo -e "${YELLOW}⚠${NC} black non installé (pip install black)"
fi

# pytest
if command -v pytest &> /dev/null; then
    echo -e "${GREEN}✓${NC} pytest installé"
else
    echo -e "${YELLOW}⚠${NC} pytest non installé (pip install pytest)"
fi

# Résumé
echo -e "\n${YELLOW}[4/4]${NC} Hooks installés:"
echo -e "  ${GREEN}✓${NC} pre-commit  : Validation avant commit"
echo -e "  ${GREEN}✓${NC} commit-msg  : Validation du message de commit"
echo -e "  ${GREEN}✓${NC} pre-push    : Tests avant push"

echo -e "\n${BLUE}═══════════════════════════════════════${NC}"
echo -e "${GREEN}✓ Installation terminée avec succès!${NC}"
echo -e "${BLUE}═══════════════════════════════════════${NC}\n"

echo -e "${YELLOW}Pour désactiver les hooks temporairement:${NC}"
echo -e "  git commit --no-verify"
echo -e "  git push --no-verify"
echo -e ""
echo -e "${YELLOW}Format de commit attendu:${NC}"
echo -e "  <type>(<scope>): <description>"
echo -e ""
echo -e "${YELLOW}Types valides:${NC}"
echo -e "  feat, fix, docs, style, refactor, perf, test, build, ci, chore"
echo -e ""
echo -e "${YELLOW}Exemple:${NC}"
echo -e "  ${GREEN}feat(ingestion): ajouter support pour Modbus TCP${NC}"
echo -e ""
