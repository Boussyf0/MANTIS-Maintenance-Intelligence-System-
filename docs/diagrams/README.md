# 🎨 MANTIS - Diagrammes de Conception

Ce dossier contient tous les diagrammes professionnels pour l'architecture MANTIS.

## 📁 Contenu

### Fichiers principaux

#### Architecture

- **`architecture.puml`** - Diagramme PlantUML complet de l'architecture globale

#### Diagrammes de Classe (UML)

- **`class-diagram-ingestion.puml`** - Service Ingestion IIoT (Java/Spring Boot)
- **`class-diagram-rul-prediction.puml`** - Service RUL Prediction (Python/PyTorch)

#### Diagrammes de Cas d'Utilisation

- **`use-case-diagram.puml`** - Cas d'utilisation complets (8 packages, 75+ UC)

#### Diagrammes de Séquence Détaillés

- **`sequence-diagram-opcua-ingestion.puml`** - Ingestion données OPC UA avec résilience
- **`sequence-diagram-training-deployment.puml`** - Entraînement et déploiement modèle ML

#### Documentation Complète

- **`../DESIGN_DIAGRAMS.md`** - Documentation avec diagrammes Mermaid (20+ diagrammes)

## 🔧 Comment utiliser les diagrammes

### Option 1: Visualiser dans VS Code

**Installer l'extension PlantUML**:

```bash
# Dans VS Code
1. Ouvrir Extensions (Cmd+Shift+X)
2. Rechercher "PlantUML"
3. Installer "PlantUML" par jebbs
4. Installer Graphviz: brew install graphviz
```

**Visualiser**:

1. Ouvrir `architecture.puml`
2. Appuyer sur `Alt+D` pour prévisualiser
3. Exporter en PNG/SVG avec `Cmd+Shift+P` > "PlantUML: Export Current Diagram"

### Option 2: Visualiser en ligne

**PlantUML Online Server**:

1. Aller sur <http://www.plantuml.com/plantuml/uml>
2. Copier-coller le contenu de `architecture.puml`
3. Cliquer sur "Submit"
4. Télécharger PNG/SVG

**PlantText**:

- URL: <https://www.planttext.com>
- Même procédure que PlantUML Online

### Option 3: Visualiser les diagrammes Mermaid

**Dans GitHub**:

- Les fichiers `.md` avec diagrammes Mermaid s'affichent automatiquement
- Ouvrir `../DESIGN_DIAGRAMS.md` sur GitHub

**Mermaid Live Editor**:

1. Aller sur <https://mermaid.live>
2. Copier-coller le code Mermaid depuis `DESIGN_DIAGRAMS.md`
3. Éditer en temps réel
4. Exporter PNG/SVG

**Dans VS Code**:

```bash
# Installer l'extension Markdown Preview Mermaid Support
1. Ouvrir Extensions (Cmd+Shift+X)
2. Rechercher "Markdown Preview Mermaid Support"
3. Installer
4. Ouvrir DESIGN_DIAGRAMS.md et preview (Cmd+Shift+V)
```

## 📊 Types de diagrammes disponibles

| Type | Fichier | Outil | Description |
|------|---------|-------|-------------|
| Architecture complète | `architecture.puml` | PlantUML | Vue d'ensemble avec tous les composants |
| Architecture C4 | `DESIGN_DIAGRAMS.md` | Mermaid | Contexte, Conteneurs, Composants |
| Séquences | `DESIGN_DIAGRAMS.md` | Mermaid | Flux de données bout-en-bout |
| Déploiement K8s | `DESIGN_DIAGRAMS.md` | Mermaid | Architecture Kubernetes production |
| ERD | `DESIGN_DIAGRAMS.md` | Mermaid | Modèle de données relationnel |
| Topics Kafka | `DESIGN_DIAGRAMS.md` | Mermaid | Flux de messages et schémas |

## 🎯 Utilisation recommandée par rôle

### Développeur Backend (Java/Python)

**À consulter**:

- `DESIGN_DIAGRAMS.md` > Section 2.3 (Composants)
- `DESIGN_DIAGRAMS.md` > Section 3 (Diagrammes de séquence)
- `DESIGN_DIAGRAMS.md` > Section 5 (Modèle de données)
- `architecture.puml` (vue complète)

**Cas d'usage**:

- Comprendre les dépendances entre services
- Identifier les APIs à appeler
- Concevoir les schémas de BDD

### Développeur Frontend (React)

**À consulter**:

- `DESIGN_DIAGRAMS.md` > Section 2.2 (Conteneurs)
- `DESIGN_DIAGRAMS.md` > Section 3.1 (Flux complet)
- `DESIGN_DIAGRAMS.md` > Section 10.1 (API REST)

**Cas d'usage**:

- Identifier les endpoints API
- Comprendre le flux WebSocket
- Concevoir les vues Dashboard

### DevOps / SRE

**À consulter**:

- `DESIGN_DIAGRAMS.md` > Section 4 (Déploiement K8s)
- `DESIGN_DIAGRAMS.md` > Section 4.2 (Quotas ressources)
- `architecture.puml` (infrastructure complète)

**Cas d'usage**:

- Planifier le déploiement
- Dimensionner les ressources
- Configurer le monitoring

### Data Scientist / ML Engineer

**À consulter**:

- `DESIGN_DIAGRAMS.md` > Section 7 (Pipeline ML)
- `DESIGN_DIAGRAMS.md` > Section 3.1 (Flux RUL)
- `DESIGN_DIAGRAMS.md` > Section 5.2 (TimescaleDB schema)

**Cas d'usage**:

- Comprendre le pipeline ML
- Identifier les features disponibles
- Intégrer les modèles

### Chef de projet / Product Owner

**À consulter**:

- `DESIGN_DIAGRAMS.md` > Section 1 (Vue d'ensemble)
- `DESIGN_DIAGRAMS.md` > Section 2.1 (Contexte C4)
- `architecture.puml` (architecture globale)

**Cas d'usage**:

- Présenter l'architecture aux parties prenantes
- Planifier les sprints
- Identifier les dépendances

## 🔄 Génération automatique des diagrammes

### Exporter tous les diagrammes en PNG

**Script PlantUML**:

```bash
# Installer PlantUML CLI
brew install plantuml

# Générer tous les PNG
cd docs/diagrams
plantuml architecture.puml

# Ou générer en SVG
plantuml -tsvg architecture.puml
```

**Script pour diagrammes Mermaid**:

```bash
# Installer mermaid-cli
npm install -g @mermaid-js/mermaid-cli

# Extraire et générer les diagrammes depuis DESIGN_DIAGRAMS.md
mmdc -i ../DESIGN_DIAGRAMS.md -o output/
```

### Automatisation CI/CD

**GitHub Actions** (`.github/workflows/diagrams.yml`):

```yaml
name: Generate Diagrams

on:
  push:
    paths:
      - 'docs/diagrams/**'
      - 'docs/DESIGN_DIAGRAMS.md'

jobs:
  generate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Generate PlantUML
        uses: grassedge/generate-plantuml-action@v1.5
        with:
          path: docs/diagrams
          message: "Regenerated PlantUML diagrams"

      - name: Generate Mermaid
        uses: neenjaw/compile-mermaid-markdown-action@v0.3.3
        with:
          files: 'docs/DESIGN_DIAGRAMS.md'
          output: 'docs/diagrams/output'
```

## 📐 Conventions de diagrammes

### Couleurs standardisées

- **Java/Spring Boot**: Orange (#FFA726)
- **Python/FastAPI**: Vert (#66BB6A)
- **React/Next.js**: Bleu (#42A5F5)
- **Kafka**: Noir (#231F20)
- **Bases de données**: Bleu foncé (#0277BD)
- **Alertes critiques**: Rouge (#F44336)

### Icônes

- ☕ = Service Java
- 🐍 = Service Python
- ⚛️ = Service React
- 📊 = Base de données
- 🔄 = Event streaming
- ⚠️ = Alerte

### Nommage

- **Services**: `NomService` (PascalCase)
- **Topics Kafka**: `domain.event` (snake_case avec point)
- **Bases de données**: `nom_table` (snake_case)
- **APIs**: `/api/v1/resource` (kebab-case)

## 🛠️ Édition des diagrammes

### Ajouter un nouveau service

1. Ouvrir `architecture.puml`
2. Ajouter le rectangle dans le bon package
3. Ajouter les connexions
4. Régénérer le diagramme
5. Mettre à jour `DESIGN_DIAGRAMS.md` section 2.2

### Ajouter un nouveau flux

1. Ouvrir `DESIGN_DIAGRAMS.md`
2. Ajouter un diagramme de séquence Mermaid dans section 3
3. Documenter les topics Kafka impactés
4. Mettre à jour le tableau des topics (section 6.1)

### Ajouter une nouvelle table

1. Ouvrir `DESIGN_DIAGRAMS.md`
2. Ajouter la table dans l'ERD (section 5.1)
3. Ajouter les relations
4. Documenter les triggers et indexes

## 📚 Ressources

### Documentation officielle

- **PlantUML**: <https://plantuml.com>
- **Mermaid**: <https://mermaid.js.org>
- **C4 Model**: <https://c4model.com>

### Exemples et templates

- **C4-PlantUML**: <https://github.com/plantuml-stdlib/C4-PlantUML>
- **Mermaid Live**: <https://mermaid.live>
- **PlantUML Icons**: <https://github.com/awslabs/aws-icons-for-plantuml>

### Outils recommandés

- **VS Code Extensions**:
  - PlantUML (jebbs)
  - Markdown Preview Mermaid Support
  - Draw.io Integration

- **CLI Tools**:
  - `plantuml` - Générateur PlantUML
  - `mmdc` (mermaid-cli) - Générateur Mermaid
  - `graphviz` - Moteur de rendu

## 🤝 Contribution

### Workflow de mise à jour

1. Créer une branche: `git checkout -b docs/update-diagrams`
2. Éditer les fichiers `.puml` ou `DESIGN_DIAGRAMS.md`
3. Régénérer les images PNG/SVG
4. Commit avec message descriptif: `docs: update architecture diagram for new service X`
5. Créer une Pull Request
6. Faire valider par l'équipe architecture

### Standards de qualité

✅ **Bon diagramme**:

- Lisible (pas trop de détails)
- Légende claire
- Couleurs standardisées
- Annotations explicatives
- Format vectoriel (SVG) disponible

❌ **Mauvais diagramme**:

- Trop chargé (>30 éléments)
- Pas de légende
- Couleurs aléatoires
- Pas de documentation
- Uniquement en bitmap (PNG)

## 📅 Maintenance

### Fréquence de mise à jour

- **Architecture globale**: À chaque ajout de service (sprint review)
- **Diagrammes de séquence**: À chaque nouveau flux majeur
- **ERD**: À chaque modification de schéma
- **Déploiement K8s**: À chaque changement d'infra

### Checklist de mise à jour

- [ ] Diagramme mis à jour
- [ ] Documentation textuelle synchronisée
- [ ] Images PNG/SVG régénérées
- [ ] Tableau des versions mis à jour
- [ ] Pull Request créée
- [ ] Revue par l'équipe architecture
- [ ] Merge et publication

---

**Dernière mise à jour**: 2025-01-21
**Responsable**: MANTIS Architecture Team
**Contact**: Pour toute question, ouvrir une issue GitHub avec le label `documentation`
