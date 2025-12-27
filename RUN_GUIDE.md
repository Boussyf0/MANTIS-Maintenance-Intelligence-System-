# MANTIS - Guide de Démarrage (Mise à jour)

Ce guide détaille les étapes pour lancer l'ensemble de la plateforme MANTIS.

## 🚀 Démarrage Rapide (Recommandé)

Nous avons un script qui gère tout (arrêt, nettoyage, build, démarrage) :

1.  **Lancer tout le projet :**
    ```bash
    ./start_all.sh
    ```
    *Ce script lance Kafka, bases de données, monitoring (Prometheus/Grafana), MLflow et tous les microservices.*

2.  **Lancer le simulateur de données :** (Dans un nouveau terminal, requis pour avoir des données)
    ```bash
    export KAFKA_BROKER=localhost:9093
    python3 scripts/sensor-simulator.py
    ```

---

## 🛠 Accès aux Interfaces

| Service | URL | Identifiants |
| :--- | :--- | :--- |
| **Grafana** (Dashboards) | [http://localhost:3000](http://localhost:3000) | `admin` / `admin` |
| **MLflow** (Modèles IA) | [http://localhost:5002](http://localhost:5002) | - |
| **Frontend App** | [http://localhost:3001](http://localhost:3001) | - |
| **MinIO** (Stockage S3) | [http://localhost:9001](http://localhost:9001) | `minioadmin` / `minioadmin` |
| **Prometheus** | [http://localhost:9095](http://localhost:9095) | - |
| **Kafka UI** | [http://localhost:8082](http://localhost:8082) | - |

---

## 🧠 Workflow AI / ML (Optionnel)

Si vous souhaitez ré-entraîner les modèles et peupler le registre MLflow :

1.  **Entraîner & Enregistrer le modèle d'Anomalie :**
    ```bash
    # Dépendances requises : pip install mlflow==2.5.0 boto3 pyod torch torchvision
    export AWS_ACCESS_KEY_ID=minioadmin
    export AWS_SECRET_ACCESS_KEY=minioadmin
    export MLFLOW_S3_ENDPOINT_URL=http://localhost:9000
    
    python3 notebooks/train_and_log_mlflow.py
    ```

2.  **Entraîner & Enregistrer le modèle RUL (LSTM) :**
    ```bash
    export AWS_ACCESS_KEY_ID=minioadmin
    export AWS_SECRET_ACCESS_KEY=minioadmin
    export MLFLOW_S3_ENDPOINT_URL=http://localhost:9000
    
    python3 notebooks/train_lstm_and_log_mlflow.py
    ```

3.  **Optimisation des Hyperparamètres (Grid Search) :**
    ```bash
    python3 notebooks/optimize_lstm_mlflow.py
    ```

---

## 🛑 Arrêt
```bash
docker-compose -f infrastructure/docker/docker-compose.infrastructure.yml -f infrastructure/docker/docker-compose.services.yml down
```
