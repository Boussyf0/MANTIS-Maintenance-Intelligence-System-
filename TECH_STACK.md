# Stack technologique MANTIS

## 🎯 Architecture hybride Java/Spring Boot + Python

MANTIS utilise une approche **polyglotte** optimisée pour chaque cas d'usage.

## ☕ Services Java/Spring Boot

### 1. Ingestion IIoT
- **Framework**: Spring Boot 3.2
- **Protocoles**: OPC UA (Eclipse Milo), MQTT (Paho), Modbus
- **Streaming**: Spring Kafka
- **Résilience**: Resilience4j
- **Monitoring**: Micrometer + Prometheus

### 2. Preprocessing (à développer)
- **Framework**: Spring Boot 3.2
- **Stream Processing**: Kafka Streams
- **Performance**: Reactive (WebFlux)

### 3. Orchestrator (à développer)
- **Framework**: Spring Boot 3.2
- **Règles métier**: Drools
- **Optimisation**: OR-Tools (via JNI)
- **Workflow**: Spring Integration

### 4. API Gateway (à développer)
- **Framework**: Spring Cloud Gateway
- **Sécurité**: Spring Security, OAuth2, JWT
- **Rate Limiting**: Bucket4j

## 🐍 Services Python

### 1. Feature Extraction (à développer)
- **Framework**: FastAPI
- **Features TS**: tsfresh, tsflex
- **Signal Processing**: scipy.signal, PyWavelets
- **Feature Store**: Feast

### 2. Anomaly Detection (à développer)
- **Framework**: FastAPI
- **Algorithms**: PyOD (40+ algorithms)
- **ML**: scikit-learn (Isolation Forest, One-Class SVM)
- **DL**: PyTorch (Autoencoders)

### 3. RUL Prediction (à développer)
- **Framework**: FastAPI
- **Deep Learning**: PyTorch (LSTM, GRU, TCN)
- **ML**: XGBoost (baseline)
- **MLOps**: MLflow (tracking, registry)
- **Explainability**: SHAP, LIME

### 4. Notebooks
- **Jupyter**: Exploration, expérimentation
- **Libs**: pandas, numpy, matplotlib, seaborn

## 🗄️ Bases de données

### PostgreSQL
- **Version**: 15
- **Usage**: Métadonnées (assets, sensors, work orders)
- **ORM Java**: Spring Data JPA
- **ORM Python**: SQLAlchemy

### TimescaleDB
- **Version**: Latest (PostgreSQL 15)
- **Usage**: Séries temporelles
- **Features**: Hypertables, compression, continuous aggregates

### InfluxDB
- **Version**: 2.7
- **Usage**: Métriques haute fréquence (>100Hz)

### Redis
- **Version**: 7
- **Usage**: Feature Store online (Feast), cache

### MinIO
- **Version**: Latest
- **Usage**: Object storage (modèles ML, données brutes)

## 🔄 Streaming & Messaging

### Apache Kafka
- **Version**: 3.6
- **Usage**: Communication asynchrone inter-services
- **Topics**:
  - `sensor.raw` - Données brutes
  - `sensor.preprocessed` - Données nettoyées
  - `features.computed` - Features
  - `anomalies.detected` - Anomalies
  - `rul.predictions` - Prédictions RUL
  - `maintenance.actions` - Actions maintenance

### Clients
- **Java**: Spring Kafka, Kafka Streams
- **Python**: aiokafka, faust-streaming

## 🤖 MLOps

### MLflow
- **Version**: 2.5
- **Usage**:
  - Experiment tracking
  - Model registry
  - Model serving
- **Backend**: PostgreSQL
- **Artifact Store**: MinIO

### Feast
- **Version**: 0.32
- **Usage**: Feature Store
- **Online Store**: Redis
- **Offline Store**: Parquet (MinIO)

## 📊 Monitoring & Observability

### Prometheus
- **Version**: 2.45
- **Usage**: Métriques time series
- **Exporters**:
  - Java: Micrometer
  - Python: prometheus-client

### Grafana
- **Version**: 10.0
- **Usage**: Visualisation métriques
- **Datasources**: Prometheus, TimescaleDB, InfluxDB

### Jaeger
- **Version**: 1.48
- **Usage**: Distributed tracing
- **Protocol**: OpenTelemetry

## 🐳 Infrastructure

### Docker
- **Version**: 20.10+
- **Compose**: 2.0+

### Kubernetes (production)
- **Version**: 1.24+
- **Ingress**: NGINX
- **Service Mesh**: Istio (optionnel)

## 🔧 Outils de développement

### Java
- **JDK**: 17 (LTS)
- **Build**: Maven 3.9
- **IDE**: IntelliJ IDEA, Eclipse
- **Testing**: JUnit 5, Mockito, Testcontainers
- **Code Quality**: SonarQube, SpotBugs
- **Formatting**: spring-javaformat

### Python
- **Version**: 3.11
- **Package Manager**: pip
- **Env Management**: venv, conda
- **IDE**: PyCharm, VS Code
- **Testing**: pytest, pytest-cov
- **Code Quality**: pylint, flake8, black, isort

### Frontend (Dashboard)
- **Framework**: React.js 18, Next.js 14
- **UI**: TailwindCSS, shadcn/ui
- **Charts**: Recharts, Plotly.js, D3.js
- **State**: Zustand, React Query
- **Build**: Vite
- **Testing**: Jest, React Testing Library

## 📦 Dépendances principales

### Java (pom.xml)
```xml
- spring-boot-starter-web: 3.2.1
- spring-boot-starter-data-jpa: 3.2.1
- spring-kafka: 3.1.0
- eclipse-milo (OPC UA): 0.6.10
- eclipse-paho (MQTT): 1.2.5
- resilience4j-spring-boot3: 2.1.0
- micrometer-registry-prometheus
- postgresql: latest
```

### Python (requirements.txt)
```python
- fastapi: 0.100.1
- uvicorn[standard]: 0.23.2
- aiokafka: 0.8.1
- asyncpg: 0.28.0
- sqlalchemy: 2.0.19
- torch: 2.0.1
- xgboost: 1.7.6
- pyod: 1.1.0
- tsfresh: 0.20.1
- mlflow: 2.5.0
- feast: 0.32.0
- shap: 0.42.1
```

## 🔐 Sécurité

### Authentication & Authorization
- **Java**: Spring Security, OAuth2, JWT
- **Python**: FastAPI Security, python-jose

### Secrets Management
- **Development**: .env files
- **Production**: HashiCorp Vault, Kubernetes Secrets

### Network Security
- **TLS**: Certificates pour tous les services
- **Network Policies**: Kubernetes NetworkPolicy
- **Firewall**: iptables, cloud firewall

## 🚀 CI/CD

### GitHub Actions
```yaml
- Build: Maven (Java), pip (Python)
- Test: JUnit, pytest
- Coverage: JaCoCo, pytest-cov
- Security: Snyk, OWASP Dependency Check
- Docker Build: multi-stage builds
- Deploy: ArgoCD, Helm
```

### Quality Gates
- **Tests**: Coverage > 80%
- **Security**: No critical vulnerabilities
- **Performance**: Benchmarks pass
- **Code Quality**: SonarQube quality gate

## 📏 Standards & Conventions

### API Design
- **REST**: OpenAPI 3.0 (Swagger)
- **gRPC**: Proto3 (services critiques)
- **GraphQL**: Dashboard queries (optionnel)

### Versioning
- **Services**: SemVer (MAJOR.MINOR.PATCH)
- **APIs**: URI versioning (/api/v1/...)
- **Models ML**: MLflow registry

### Naming
- **Services**: kebab-case (ingestion-iiot)
- **Classes Java**: PascalCase (SensorData)
- **Variables**: camelCase (sensorCode)
- **Python**: snake_case (sensor_data)
- **SQL**: snake_case (sensor_data_raw)

## 🎓 Pourquoi cette stack ?

### Java/Spring Boot
✅ **Performance** : Throughput élevé, latence faible
✅ **Enterprise-grade** : Standard industrie
✅ **Thread safety** : Gestion native concurrence
✅ **Écosystème mature** : Librairies robustes

### Python
✅ **ML/Data Science** : Écosystème inégalé
✅ **Productivité** : Développement rapide
✅ **Recherche** : State-of-the-art algorithms
✅ **Communauté** : Support actif

### Communication Kafka
✅ **Découplage** : Services indépendants
✅ **Scalabilité** : Horizontal scaling
✅ **Résilience** : Retry, replay
✅ **Performance** : Millions msg/sec

## 📊 Comparaison performance

| Métrique | Java/Spring | Python/FastAPI |
|----------|-------------|----------------|
| Throughput | 50K req/s | 15K req/s |
| Latence P99 | 10ms | 30ms |
| Mémoire | 200MB | 50MB |
| Démarrage | 3s | 0.5s |
| CPU intensif | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |

*Voir `docs/JAVA_VS_PYTHON.md` pour analyse complète*

## 🔄 Évolution future

### Court terme
- [ ] Completer tous les services Java/Python
- [ ] Tests E2E complets
- [ ] CI/CD pipeline
- [ ] Documentation API complète

### Moyen terme
- [ ] GraalVM pour Java (native images, démarrage rapide)
- [ ] Rust pour services critiques (ultra performance)
- [ ] GraphQL federation
- [ ] Event Sourcing (CQRS)

### Long terme
- [ ] Service Mesh (Istio)
- [ ] Serverless functions (AWS Lambda, Knative)
- [ ] Edge Computing (K3s)
- [ ] AI/AutoML automatisation

---

**Dernière mise à jour**: 2025-01-21
**Maintenu par**: MANTIS Team - EMSI
