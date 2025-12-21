# MANTIS - Guide de Démarrage

Ce guide détaille les étapes pour lancer l'ensemble de la plateforme MANTIS sur votre machine locale.

## Prérequis

Assurez-vous d'avoir installé :
- **Docker** et **Docker Compose**
- **Python 3.10+** (pour le simulateur et le serveur frontend)
- **Java 17+** (optionnel, si vous souhaitez compiler hors Docker)

---

## Étape 1 : Démarrer l'Infrastructure

L'infrastructure comprend Kafka, Zookeeper, les bases de données (Postgres, InfluxDB) et les outils de monitoring (Prometheus, Grafana).

1. Ouvrez un terminal à la racine du projet.
2. Naviguez vers le dossier docker :
   ```bash
   cd infrastructure/docker
   ```
3. Lancez l'infrastructure :
   ```bash
   docker-compose -f docker-compose.infrastructure.yml up -d
   ```
4. Attendez que tous les conteneurs soient "healthy" (environ 30-60 secondes).

---

## Étape 2 : Démarrer les Microservices

Les microservices incluent l'ingestion, le prétraitement, l'extraction de features, la prédiction RUL, la détection d'anomalies, l'orchestrateur et l'API dashboard.

1. Toujours dans le dossier `infrastructure/docker` :
   ```bash
   docker-compose -f docker-compose.services.yml up -d --build
   ```
   *(L'option `--build` assure que vous utilisez la dernière version du code)*

2. Vérifiez que les services tournent :
   ```bash
   docker ps
   ```

---

## Étape 3 : Démarrer le Simulateur de Capteurs

Le simulateur génère des données réalistes pour 3 machines et les envoie à Kafka.

1. Ouvrez un **nouveau terminal** à la racine du projet.
2. Installez les dépendances Python (si ce n'est pas déjà fait) :
   ```bash
   pip install kafka-python
   ```
3. Lancez le simulateur :
   ```bash
   export KAFKA_BROKER=localhost:9093
   python scripts/sensor-simulator.py
   ```
   *Vous devriez voir des logs défiler avec des données de capteurs.*

---

## Étape 4 : Démarrer le Dashboard Frontend

Pour éviter les problèmes de sécurité (CORS) liés à l'ouverture directe des fichiers HTML, nous servons le frontend via un petit serveur HTTP local.

1. Ouvrez un **nouveau terminal** à la racine du projet.
2. Naviguez vers le dossier du frontend :
   ```bash
   cd services/dashboard-frontend
   ```
3. Lancez le serveur HTTP Python sur le port 8081 :
   ```bash
   python -m http.server 8081
   ```

---

## Étape 5 : Accéder à l'Application

Tout est prêt ! Voici les liens pour accéder aux différentes interfaces :

### 🏭 Dashboard Principal
👉 **[http://localhost:8081](http://localhost:8081)**
*Visualisation temps-réel de l'état des machines, RUL et anomalies.*

### 📊 Grafana (Monitoring Technique)
👉 **[http://localhost:3001](http://localhost:3001)**
*Login: `admin` / Password: `admin`*
*Dashboards disponibles :*
- *MANTIS / Application Performance*
- *MANTIS / ML Metrics*
- *MANTIS / Sensor Data*

### 🔍 Autres Interfaces
- **Prometheus** : [http://localhost:9091](http://localhost:9091)
- **Kafka UI** : [http://localhost:8080](http://localhost:8080)
- **API Dashboard (Backend)** : [http://localhost:8007/api/machines](http://localhost:8007/api/machines)

---

## Arrêt de l'Application

Pour tout arrêter proprement :

```bash
cd infrastructure/docker
docker-compose -f docker-compose.services.yml down
docker-compose -f docker-compose.infrastructure.yml down
```
