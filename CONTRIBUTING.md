# Guide de contribution - MANTIS

Merci de votre intérêt pour contribuer à MANTIS ! Ce document explique comment contribuer au projet.

## Code de conduite

Ce projet adhère à un code de conduite basé sur le respect, la collaboration et l'inclusivité. En participant, vous vous engagez à respecter ce code.

## Comment contribuer

### 1. Signaler un bug

Si vous trouvez un bug :

1. Vérifiez qu'il n'a pas déjà été signalé dans les [Issues](../../issues)
2. Créez une nouvelle issue avec le template "Bug Report"
3. Incluez :
   - Description claire du problème
   - Étapes pour reproduire
   - Comportement attendu vs. actuel
   - Version de MANTIS
   - Logs/captures d'écran si pertinent

### 2. Proposer une fonctionnalité

Pour proposer une nouvelle fonctionnalité :

1. Créez une issue avec le template "Feature Request"
2. Décrivez :
   - Le problème que cela résout
   - La solution proposée
   - Les alternatives considérées
   - Impact sur l'architecture existante

### 3. Soumettre des modifications

#### Setup environnement de développement

```bash
# 1. Fork et cloner
git clone https://github.com/votre-username/MANTIS.git
cd MANTIS

# 2. Créer une branche
git checkout -b feature/ma-fonctionnalite

# 3. Setup environnement Python
python -m venv venv
source venv/bin/activate
pip install -r requirements-dev.txt

# 4. Installer pre-commit hooks
pre-commit install

# 5. Lancer l'infrastructure pour tests
make start
```

#### Conventions de code

**Python**:
- Style: PEP 8
- Formatage: Black (line length 120)
- Import ordering: isort
- Type hints: Obligatoires pour les fonctions publiques
- Docstrings: Google style

Exemple :
```python
def calculate_rul(
    sensor_data: pd.DataFrame,
    model: torch.nn.Module,
    confidence_level: float = 0.95
) -> Tuple[float, float, float]:
    """
    Calcule la Remaining Useful Life avec intervalle de confiance.

    Args:
        sensor_data: DataFrame avec colonnes sensor_1 à sensor_21
        model: Modèle PyTorch entraîné
        confidence_level: Niveau de confiance pour l'intervalle (défaut: 0.95)

    Returns:
        Tuple contenant (rul_hours, lower_bound, upper_bound)

    Raises:
        ValueError: Si sensor_data est vide ou mal formé
    """
    # Implementation
    pass
```

**JavaScript/TypeScript** (Dashboard) :
- Style: Airbnb
- Formatage: Prettier
- Linter: ESLint

#### Tests

Tous les nouveaux codes doivent inclure des tests :

```bash
# Tests unitaires
pytest tests/unit/test_mon_module.py -v

# Tests d'intégration
pytest tests/integration/test_mon_service.py -v

# Couverture (minimum 80%)
pytest --cov=services/mon-service tests/
```

Exemple de test :
```python
# tests/unit/services/rul_prediction/test_models.py
import pytest
import torch
from services.rul_prediction.models import LSTMRULModel

def test_lstm_forward_pass():
    """Test que le forward pass du LSTM fonctionne."""
    model = LSTMRULModel(input_size=21, hidden_size=50, num_layers=2)
    batch_size = 16
    seq_length = 30
    input_size = 21

    x = torch.randn(batch_size, seq_length, input_size)
    output = model(x)

    assert output.shape == (batch_size, 1)
    assert not torch.isnan(output).any()
```

#### Workflow Git

1. **Créer une branche** depuis `main`:
   ```bash
   git checkout -b feature/nom-fonctionnalite
   # ou
   git checkout -b fix/nom-bug
   ```

2. **Commits atomiques** avec messages clairs :
   ```bash
   git commit -m "feat(rul-prediction): ajoute modèle TCN"
   git commit -m "fix(ingestion): corrige reconnexion MQTT"
   git commit -m "docs(readme): met à jour installation"
   ```

   Préfixes conventionnels :
   - `feat`: Nouvelle fonctionnalité
   - `fix`: Correction de bug
   - `docs`: Documentation
   - `style`: Formatage, pas de changement de code
   - `refactor`: Refactoring
   - `test`: Ajout/modification de tests
   - `chore`: Tâches de maintenance

3. **Push et Pull Request** :
   ```bash
   git push origin feature/nom-fonctionnalite
   ```

   Dans GitHub :
   - Créer la PR vers `main`
   - Décrire les changements
   - Lier les issues concernées
   - Demander une review

4. **Revue de code** :
   - Au moins 1 approbation requise
   - CI doit passer (tests, lint)
   - Résoudre les commentaires

5. **Merge** :
   - Squash and merge pour garder l'historique propre

#### Checklist avant de soumettre une PR

- [ ] Le code suit les conventions de style
- [ ] Tous les tests passent (`make test`)
- [ ] Le linter ne rapporte aucune erreur (`make lint`)
- [ ] Le code est formaté (`make format`)
- [ ] La documentation est à jour
- [ ] Les nouvelles fonctionnalités ont des tests
- [ ] La couverture de tests est >= 80%
- [ ] Les changements sont documentés dans le CHANGELOG
- [ ] Pas de secrets/credentials dans le code
- [ ] Docker build réussit (`make docker-build`)

## Structure du projet

```
MANTIS/
├── services/              # Microservices
│   ├── ingestion-iiot/   # Service d'ingestion IIoT
│   │   ├── main.py       # Point d'entrée
│   │   ├── config.py     # Configuration
│   │   ├── connectors/   # Connecteurs protocoles
│   │   ├── requirements.txt
│   │   ├── Dockerfile
│   │   └── README.md
│   └── ...
├── infrastructure/
│   ├── docker/           # Docker Compose
│   └── kubernetes/       # Manifests K8s
├── tests/
│   ├── unit/             # Tests unitaires
│   └── integration/      # Tests d'intégration
├── notebooks/            # Jupyter notebooks
├── scripts/              # Scripts utilitaires
└── docs/                 # Documentation
```

## Développement de nouveaux services

Pour créer un nouveau microservice :

1. **Copier le template** :
   ```bash
   cp -r services/ingestion-iiot services/mon-nouveau-service
   ```

2. **Adapter** :
   - `main.py` - Point d'entrée FastAPI
   - `config.py` - Configuration (environnement)
   - `requirements.txt` - Dépendances
   - `Dockerfile` - Image Docker
   - `README.md` - Documentation du service

3. **Suivre les patterns** :
   - Configuration via variables d'environnement
   - Logging avec loguru
   - Métriques Prometheus
   - Health check endpoint
   - Graceful shutdown
   - Retry logic avec tenacity

4. **Intégration** :
   - Ajouter au `docker-compose.services.yml`
   - Configurer les topics Kafka
   - Ajouter les tests
   - Documenter dans `ARCHITECTURE.md`

## Communication Kafka

Toutes les communications inter-services passent par Kafka :

**Topics standards** :
- `sensor.raw` - Données brutes capteurs
- `sensor.preprocessed` - Données nettoyées
- `features.computed` - Features calculées
- `anomalies.detected` - Anomalies détectées
- `rul.predictions` - Prédictions RUL
- `maintenance.actions` - Actions de maintenance

**Format de message** :
```json
{
  "timestamp": "2025-01-15T10:30:45.123Z",
  "asset_id": "550e8400-e29b-41d4-a716-446655440000",
  "sensor_id": "660e8400-e29b-41d4-a716-446655440001",
  "data": { ... },
  "metadata": {
    "source": "service-name",
    "version": "1.0.0"
  }
}
```

## Base de données

**PostgreSQL** (métadonnées) :
- Migrations : Alembic
- ORM : SQLAlchemy (optionnel)
- Connexion : asyncpg

**TimescaleDB** (séries temporelles) :
- Hypertables pour partitionnement automatique
- Continuous aggregates pour performances
- Compression après 7 jours

## MLOps

**MLflow** :
- Tracking : Tous les runs d'entraînement
- Registry : Modèles versionnés
- Artifacts : Stockage dans MinIO

**Feast** (Feature Store) :
- Online : Redis (faible latence)
- Offline : Parquet dans MinIO
- Feature definitions dans `features/`

Exemple :
```python
from feast import FeatureView, Field, Entity
from feast.types import Float32, String

asset = Entity(name="asset", join_keys=["asset_id"])

vibration_features = FeatureView(
    name="vibration_features",
    entities=[asset],
    schema=[
        Field(name="rms", dtype=Float32),
        Field(name="kurtosis", dtype=Float32),
        Field(name="crest_factor", dtype=Float32),
    ],
    source=kafka_source,
    ttl=timedelta(days=365),
)
```

## Documentation

- **Code** : Docstrings Google style
- **API** : FastAPI auto-génère avec /docs
- **Architecture** : `ARCHITECTURE.md`
- **Utilisateur** : `docs/user-guide/`
- **Notebooks** : Jupyter avec markdown explicatif

## Releases

Versioning : SemVer (MAJOR.MINOR.PATCH)

- MAJOR : Breaking changes
- MINOR : Nouvelles fonctionnalités (backward compatible)
- PATCH : Bug fixes

## Questions ?

- 📧 Email : O.ouedrhiri@emsi.ma, H.Tabbaa@emsi.ma, lachgar.m@gmail.com
- 💬 Discussions : GitHub Discussions
- 🐛 Bugs : GitHub Issues

Merci de contribuer à MANTIS ! 🚀
