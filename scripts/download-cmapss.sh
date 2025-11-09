#!/bin/bash
# Script pour télécharger le dataset NASA C-MAPSS
# Utilise Kaggle API via Python (kagglehub)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "=== Téléchargement du dataset NASA C-MAPSS ==="
echo ""
echo "Méthode recommandée: Utiliser Kaggle (plus fiable)"
echo ""
echo "Vérification de l'environnement conda..."

# Utiliser l'environnement mantis s'il existe
if [ -d "/opt/anaconda3/envs/mantis" ]; then
    echo "✓ Environnement conda 'mantis' détecté"
    echo "Activation de l'environnement..."
    source /opt/anaconda3/bin/activate
    conda activate mantis

    # Installer kagglehub si nécessaire
    if ! python -c "import kagglehub" 2>/dev/null; then
        echo "Installation de kagglehub..."
        pip install kagglehub --quiet
    fi

    # Exécuter le script Python
    echo "Téléchargement depuis Kaggle..."
    python "$SCRIPT_DIR/download_from_kaggle.py"
else
    echo "⚠️  Environnement conda 'mantis' non trouvé"
    echo ""
    echo "Veuillez:"
    echo "  1. Activer votre environnement conda"
    echo "  2. Installer kagglehub: pip install kagglehub"
    echo "  3. Exécuter: python scripts/download_from_kaggle.py"
    echo ""
    echo "Ou télécharger manuellement depuis:"
    echo "  https://www.kaggle.com/datasets/behrad3d/nasa-cmaps"
    exit 1
fi
