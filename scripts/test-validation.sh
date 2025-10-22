#!/bin/bash
# Script de démonstration du système de validation MANTIS

set -e

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${BLUE}╔════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  MANTIS - Démonstration du Système de Validation  ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════╝${NC}\n"

# Vérifier qu'on est dans le bon répertoire
if [ ! -f "README.md" ]; then
    echo -e "${RED}✗ Erreur: Exécutez ce script depuis la racine du projet${NC}"
    exit 1
fi

echo -e "${CYAN}Ce script va démontrer:${NC}"
echo -e "  1. Validation de la structure du projet"
echo -e "  2. Vérification des hooks Git"
echo -e "  3. Test du format de commit message"
echo -e "  4. Résumé de la CI/CD GitHub Actions\n"

read -p "Appuyez sur Entrée pour continuer..." -r
echo

# 1. Validation de la structure du projet
echo -e "\n${BLUE}═══════════════════════════════════════${NC}"
echo -e "${YELLOW}[1/4] Validation de la structure du projet${NC}"
echo -e "${BLUE}═══════════════════════════════════════${NC}\n"

if [ -f "scripts/validate-project.sh" ]; then
    ./scripts/validate-project.sh
else
    echo -e "${RED}✗ Script de validation non trouvé${NC}"
fi

read -p "Appuyez sur Entrée pour continuer..." -r
echo

# 2. Vérification des hooks Git
echo -e "\n${BLUE}═══════════════════════════════════════${NC}"
echo -e "${YELLOW}[2/4] Vérification des hooks Git${NC}"
echo -e "${BLUE}═══════════════════════════════════════${NC}\n"

HOOKS_PATH=$(git config core.hooksPath)
echo -e "${CYAN}Chemin des hooks configuré:${NC} ${GREEN}$HOOKS_PATH${NC}\n"

echo -e "${CYAN}Hooks installés:${NC}"
for hook in .githooks/*; do
    if [ -f "$hook" ]; then
        HOOK_NAME=$(basename "$hook")
        if [ -x "$hook" ]; then
            echo -e "  ${GREEN}✓${NC} $HOOK_NAME (exécutable)"
        else
            echo -e "  ${RED}✗${NC} $HOOK_NAME (non exécutable)"
        fi
    fi
done

read -p "Appuyez sur Entrée pour continuer..." -r
echo

# 3. Test du format de commit message
echo -e "\n${BLUE}═══════════════════════════════════════${NC}"
echo -e "${YELLOW}[3/4] Test du format de commit message${NC}"
echo -e "${BLUE}═══════════════════════════════════════${NC}\n"

echo -e "${CYAN}Exemples de messages VALIDES:${NC}"
VALID_MESSAGES=(
    "feat(ingestion): ajouter support pour Modbus TCP"
    "fix(rul): corriger prédiction pour RUL < 24h"
    "docs(readme): mettre à jour les instructions"
    "refactor(preprocessing): optimiser le pipeline"
    "perf(features): réduire temps de calcul FFT"
    "test(anomaly): ajouter tests Isolation Forest"
)

for msg in "${VALID_MESSAGES[@]}"; do
    echo -e "  ${GREEN}✓${NC} $msg"
done

echo -e "\n${CYAN}Exemples de messages INVALIDES:${NC}"
INVALID_MESSAGES=(
    "Added new feature|Pas de type/scope"
    "feat: Added feature.|Point final interdit"
    "FEAT(ingestion): add support|Type en majuscule"
    "feat add support|Manque les deux-points"
    "fix|Description trop courte"
)

for entry in "${INVALID_MESSAGES[@]}"; do
    msg="${entry%|*}"
    reason="${entry#*|}"
    echo -e "  ${RED}✗${NC} $msg"
    echo -e "    ${YELLOW}→ $reason${NC}"
done

read -p "Appuyez sur Entrée pour continuer..." -r
echo

# 4. Résumé CI/CD
echo -e "\n${BLUE}═══════════════════════════════════════${NC}"
echo -e "${YELLOW}[4/4] Résumé de la CI/CD GitHub Actions${NC}"
echo -e "${BLUE}═══════════════════════════════════════${NC}\n"

if [ -f ".github/workflows/ci.yml" ]; then
    echo -e "${GREEN}✓${NC} Workflow CI/CD configuré\n"

    echo -e "${CYAN}Jobs automatiques à chaque push/PR:${NC}"
    echo -e "  ${GREEN}✓${NC} Validation des messages de commit"
    echo -e "  ${GREEN}✓${NC} Vérification de la qualité du code"
    echo -e "  ${GREEN}✓${NC} Tests Java (Maven + JaCoCo)"
    echo -e "  ${GREEN}✓${NC} Tests Python (pytest + coverage)"
    echo -e "  ${GREEN}✓${NC} Scan de sécurité (Trivy)"
    echo -e "  ${GREEN}✓${NC} Build Docker"
    echo -e "  ${GREEN}✓${NC} Tests d'intégration"

    echo -e "\n${CYAN}Fichier de configuration:${NC} .github/workflows/ci.yml"
else
    echo -e "${RED}✗${NC} Workflow CI/CD non trouvé"
fi

# Résumé final
echo -e "\n${BLUE}╔════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║                    RÉSUMÉ                          ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════╝${NC}\n"

echo -e "${GREEN}✓ Système de validation MANTIS configuré avec succès!${NC}\n"

echo -e "${CYAN}Protection à 3 niveaux:${NC}"
echo -e "  ${YELLOW}1.${NC} Hooks Git locaux (pre-commit, commit-msg, pre-push)"
echo -e "  ${YELLOW}2.${NC} GitHub Actions CI/CD (automatique sur push/PR)"
echo -e "  ${YELLOW}3.${NC} Scripts de validation manuelle\n"

echo -e "${CYAN}Prochaines étapes:${NC}"
echo -e "  ${YELLOW}•${NC} Faire un commit pour tester les hooks"
echo -e "  ${YELLOW}•${NC} Créer une PR pour tester GitHub Actions"
echo -e "  ${YELLOW}•${NC} Consulter README_VALIDATION.md pour plus de détails\n"

echo -e "${CYAN}Documentation:${NC}"
echo -e "  ${YELLOW}•${NC} README_VALIDATION.md - Guide complet du système"
echo -e "  ${YELLOW}•${NC} CONTRIBUTING.md - Guide de contribution"
echo -e "  ${YELLOW}•${NC} .gitmessage - Template de commit message\n"

echo -e "${GREEN}Tout est prêt pour garantir la qualité du code! 🚀${NC}\n"
