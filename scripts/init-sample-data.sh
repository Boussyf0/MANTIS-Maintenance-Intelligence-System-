#!/bin/bash
# Script pour initialiser la base de données avec des données d'exemple

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQL_FILE="$SCRIPT_DIR/populate-sample-data.sql"

echo "=== Initialisation des données d'exemple MANTIS ==="

# Vérifier que le fichier SQL existe
if [ ! -f "$SQL_FILE" ]; then
    echo "Erreur: $SQL_FILE non trouvé"
    exit 1
fi

# Vérifier que le container PostgreSQL est en cours d'exécution
if ! docker ps | grep -q mantis-postgres; then
    echo "Erreur: Le container mantis-postgres n'est pas en cours d'exécution"
    echo "Veuillez démarrer l'infrastructure avec: ./scripts/start-services.sh"
    exit 1
fi

# Vérifier la connectivité PostgreSQL
echo "Vérification de la connectivité PostgreSQL..."
max_retries=10
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
    echo "✗ Erreur: PostgreSQL n'est pas accessible"
    exit 1
fi

# Exécuter le script SQL
echo ""
echo "Insertion des données d'exemple..."
docker exec -i mantis-postgres psql -U mantis -d mantis < "$SQL_FILE"

if [ $? -eq 0 ]; then
    echo ""
    echo "=== ✓ Données d'exemple insérées avec succès! ==="
    echo ""
    echo "Données insérées:"
    echo "  - 8 Assets (moteurs, pompes, convoyeurs, CNC)"
    echo "  - 13 Capteurs (température, vibration, courant, pression, etc.)"
    echo "  - 7 Pièces de rechange"
    echo "  - 4 Règles de maintenance"
    echo "  - 3 Entrées historique maintenance"
    echo "  - 3 Modèles ML"
    echo ""
    echo "Vous pouvez maintenant:"
    echo "  1. Visualiser les données dans Grafana: http://localhost:3001"
    echo "  2. Explorer avec psql:"
    echo "     docker exec -it mantis-postgres psql -U mantis -d mantis"
    echo "  3. Requête exemple:"
    echo "     SELECT * FROM assets_health_dashboard;"
    echo ""
else
    echo ""
    echo "✗ Erreur lors de l'insertion des données"
    exit 1
fi
