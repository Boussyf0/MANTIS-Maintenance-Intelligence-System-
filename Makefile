# Makefile pour MANTIS

.PHONY: help install start stop clean test lint format docker-build docker-up docker-down

# Variables
DOCKER_COMPOSE_INFRA = infrastructure/docker/docker-compose.infrastructure.yml
DOCKER_COMPOSE_SERVICES = infrastructure/docker/docker-compose.services.yml
PYTHON = python3
PIP = pip3

help:
	@echo "MANTIS - Commandes disponibles:"
	@echo ""
	@echo "=== Développement ==="
	@echo "  make install          - Installer les dépendances Python"
	@echo "  make start            - Démarrer l'infrastructure"
	@echo "  make stop             - Arrêter tous les services"
	@echo "  make clean            - Nettoyer (containers, volumes, cache)"
	@echo "  make dataset          - Télécharger le dataset C-MAPSS"
	@echo "  make notebook         - Lancer Jupyter Notebook"
	@echo ""
	@echo "=== Qualité du code ==="
	@echo "  make test             - Lancer les tests"
	@echo "  make lint             - Vérifier le code (flake8, pylint)"
	@echo "  make format           - Formater le code (black, isort)"
	@echo "  make validate         - Valider la structure du projet"
	@echo "  make install-hooks    - Installer les Git hooks"
	@echo "  make test-validation  - Démonstration du système de validation"
	@echo ""
	@echo "=== Docker ==="
	@echo "  make docker-build     - Construire les images Docker"
	@echo "  make docker-up        - Démarrer avec Docker Compose"
	@echo "  make docker-down      - Arrêter Docker Compose"
	@echo ""

install:
	@echo "Installation des dépendances..."
	$(PIP) install -r requirements.txt
	$(PIP) install -r requirements-dev.txt
	@echo "✓ Installation terminée"

install-services:
	@echo "Installation des dépendances pour tous les services..."
	@for service in services/*/; do \
		if [ -f "$$service/requirements.txt" ]; then \
			echo "Installing $$service..."; \
			$(PIP) install -r "$$service/requirements.txt"; \
		fi \
	done
	@echo "✓ Installation terminée"

start:
	@echo "Démarrage de l'infrastructure MANTIS..."
	./scripts/start-services.sh

stop:
	@echo "Arrêt de MANTIS..."
	./scripts/stop-services.sh

clean:
	@echo "Nettoyage..."
	find . -type d -name "__pycache__" -exec rm -rf {} + 2>/dev/null || true
	find . -type f -name "*.pyc" -delete 2>/dev/null || true
	find . -type f -name "*.pyo" -delete 2>/dev/null || true
	find . -type d -name "*.egg-info" -exec rm -rf {} + 2>/dev/null || true
	find . -type d -name ".pytest_cache" -exec rm -rf {} + 2>/dev/null || true
	find . -type d -name ".coverage" -exec rm -rf {} + 2>/dev/null || true
	@echo "✓ Nettoyage terminé"

clean-docker:
	@echo "Nettoyage Docker (ATTENTION: supprime les volumes)..."
	cd infrastructure/docker && docker-compose -f $(DOCKER_COMPOSE_INFRA) down -v
	docker system prune -f
	@echo "✓ Nettoyage Docker terminé"

test:
	@echo "Lancement des tests..."
	pytest tests/ -v

test-unit:
	@echo "Tests unitaires..."
	pytest tests/unit/ -v

test-integration:
	@echo "Tests d'intégration..."
	pytest tests/integration/ -v

test-coverage:
	@echo "Tests avec couverture..."
	pytest --cov=services --cov-report=html --cov-report=term tests/
	@echo "Rapport de couverture: htmlcov/index.html"

lint:
	@echo "Vérification du code..."
	flake8 services/ --max-line-length=120 --exclude=venv,__pycache__
	pylint services/ --max-line-length=120 --disable=C0111,R0903
	@echo "✓ Vérification terminée"

format:
	@echo "Formatage du code..."
	black services/ notebooks/ scripts/ --line-length=120
	isort services/ notebooks/ scripts/ --profile black
	@echo "✓ Formatage terminé"

docker-build:
	@echo "Construction des images Docker..."
	cd infrastructure/docker && docker-compose -f $(DOCKER_COMPOSE_INFRA) build
	@if [ -f "$(DOCKER_COMPOSE_SERVICES)" ]; then \
		docker-compose -f $(DOCKER_COMPOSE_SERVICES) build; \
	fi
	@echo "✓ Construction terminée"

docker-up:
	@echo "Démarrage avec Docker Compose..."
	cd infrastructure/docker && docker-compose -f $(DOCKER_COMPOSE_INFRA) up -d
	@if [ -f "$(DOCKER_COMPOSE_SERVICES)" ]; then \
		docker-compose -f $(DOCKER_COMPOSE_SERVICES) up -d; \
	fi
	@echo "✓ Services démarrés"

docker-down:
	@echo "Arrêt Docker Compose..."
	cd infrastructure/docker && docker-compose -f $(DOCKER_COMPOSE_INFRA) down
	@if [ -f "$(DOCKER_COMPOSE_SERVICES)" ]; then \
		docker-compose -f $(DOCKER_COMPOSE_SERVICES) down; \
	fi
	@echo "✓ Services arrêtés"

docker-logs:
	@echo "Logs de l'infrastructure..."
	cd infrastructure/docker && docker-compose -f $(DOCKER_COMPOSE_INFRA) logs -f

docker-ps:
	@echo "État des containers:"
	cd infrastructure/docker && docker-compose -f $(DOCKER_COMPOSE_INFRA) ps

dataset:
	@echo "Téléchargement du dataset NASA C-MAPSS..."
	./scripts/download-cmapss.sh

notebook:
	@echo "Lancement de Jupyter Notebook..."
	jupyter notebook notebooks/

db-shell-postgres:
	@echo "Connexion à PostgreSQL..."
	docker exec -it mantis-postgres psql -U mantis -d mantis

db-shell-timescale:
	@echo "Connexion à TimescaleDB..."
	docker exec -it mantis-timescaledb psql -U mantis -d mantis_timeseries

init-db:
	@echo "Initialisation des bases de données..."
	@echo "Les scripts SQL sont exécutés automatiquement au démarrage"
	@echo "Pour réinitialiser, arrêter et supprimer les volumes:"
	@echo "  make clean-docker"
	@echo "  make start"

logs-kafka:
	docker logs -f mantis-kafka

logs-mlflow:
	docker logs -f mantis-mlflow

logs-ingestion:
	docker logs -f mantis-ingestion-iiot 2>/dev/null || echo "Service non démarré"

backup-db:
	@echo "Backup de PostgreSQL..."
	@mkdir -p backups
	docker exec mantis-postgres pg_dump -U mantis -d mantis > backups/postgres_$(shell date +%Y%m%d_%H%M%S).sql
	docker exec mantis-timescaledb pg_dump -U mantis -d mantis_timeseries > backups/timescaledb_$(shell date +%Y%m%d_%H%M%S).sql
	@echo "✓ Backup terminé dans backups/"

restore-db:
	@echo "Restauration nécessite un fichier SQL:"
	@echo "  docker exec -i mantis-postgres psql -U mantis -d mantis < backups/your_backup.sql"

monitor:
	@echo "URLs de monitoring:"
	@echo "  Grafana:    http://localhost:3001"
	@echo "  Prometheus: http://localhost:9091"
	@echo "  Jaeger:     http://localhost:16686"
	@echo "  Kafka UI:   http://localhost:8082"
	@echo "  MLflow:     http://localhost:5002"

validate:
	@echo "Validation de la structure du projet..."
	./scripts/validate-project.sh

install-hooks:
	@echo "Installation des Git hooks..."
	./scripts/install-hooks.sh

test-validation:
	@echo "Démonstration du système de validation..."
	./scripts/test-validation.sh

.DEFAULT_GOAL := help
