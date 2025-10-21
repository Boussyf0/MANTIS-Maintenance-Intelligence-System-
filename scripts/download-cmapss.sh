#!/bin/bash
# Script pour télécharger le dataset NASA C-MAPSS

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
DATA_DIR="$PROJECT_ROOT/data/raw/NASA_CMAPSS"

echo "=== Téléchargement du dataset NASA C-MAPSS ==="

# Créer le répertoire de données
mkdir -p "$DATA_DIR"

# URL du dataset
DATASET_URL="https://ti.arc.nasa.gov/c/6/"

echo "Téléchargement depuis NASA PCoE Data Repository..."
echo "URL: $DATASET_URL"

# Télécharger le fichier ZIP
cd "$DATA_DIR"

if [ ! -f "CMAPSSData.zip" ]; then
    echo "Téléchargement de CMAPSSData.zip..."
    curl -L -o CMAPSSData.zip "$DATASET_URL" || \
    wget -O CMAPSSData.zip "$DATASET_URL" || \
    echo "Erreur: Impossible de télécharger. Veuillez télécharger manuellement depuis:"
    echo "$DATASET_URL"
    exit 1
else
    echo "CMAPSSData.zip existe déjà, skip téléchargement"
fi

# Extraire
if [ -f "CMAPSSData.zip" ]; then
    echo "Extraction..."
    unzip -o CMAPSSData.zip

    echo ""
    echo "=== Dataset téléchargé avec succès! ==="
    echo ""
    echo "Fichiers disponibles:"
    ls -lh "$DATA_DIR"

    echo ""
    echo "Structure du dataset C-MAPSS:"
    echo "  - train_FD001.txt : Training set, sous-dataset 1"
    echo "  - test_FD001.txt  : Test set, sous-dataset 1"
    echo "  - RUL_FD001.txt   : Ground truth RUL pour test set 1"
    echo "  - train_FD002.txt : Training set, sous-dataset 2"
    echo "  - test_FD002.txt  : Test set, sous-dataset 2"
    echo "  - RUL_FD002.txt   : Ground truth RUL pour test set 2"
    echo "  - train_FD003.txt : Training set, sous-dataset 3"
    echo "  - test_FD003.txt  : Test set, sous-dataset 3"
    echo "  - RUL_FD003.txt   : Ground truth RUL pour test set 3"
    echo "  - train_FD004.txt : Training set, sous-dataset 4"
    echo "  - test_FD004.txt  : Test set, sous-dataset 4"
    echo "  - RUL_FD004.txt   : Ground truth RUL pour test set 4"
    echo ""
    echo "Colonnes:"
    echo "  1. unit number"
    echo "  2. time, in cycles"
    echo "  3. operational setting 1"
    echo "  4. operational setting 2"
    echo "  5. operational setting 3"
    echo "  6-26. sensor measurements (21 sensors)"
    echo ""
else
    echo "Erreur: CMAPSSData.zip non trouvé"
    exit 1
fi

echo "Prêt pour l'analyse! Voir notebooks/01-cmapss-exploration.ipynb"
