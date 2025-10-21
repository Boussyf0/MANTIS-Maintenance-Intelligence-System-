# 📊 MANTIS - Récapitulatif des Diagrammes de Conception

> **Créé le**: 2025-01-21
> **Objectif**: Fournir à toute l'équipe MANTIS une vision partagée et professionnelle de l'architecture

---

## 🎯 Pourquoi ces diagrammes ?

Les diagrammes de conception professionnels sont essentiels pour:

✅ **Aligner toute l'équipe** sur la même vision technique
✅ **Faciliter l'onboarding** des nouveaux développeurs
✅ **Communiquer** avec les parties prenantes non-techniques
✅ **Documenter** les décisions architecturales
✅ **Planifier** les sprints et identifier les dépendances
✅ **Débugger** en visualisant les flux de données

---

## 📁 Fichiers créés

### 1. **DESIGN_DIAGRAMS.md** (Principal)

**Chemin**: `docs/DESIGN_DIAGRAMS.md`

**Contenu** (10 sections):

1. ✅ **Vue d'ensemble** - Architecture globale avec légende des services
2. ✅ **Architecture C4** - Diagrammes Contexte, Conteneurs, Composants
3. ✅ **Diagrammes de séquence** - 3 flux principaux:
   - Flux complet (Ingestion → RUL → Alerte)
   - Détection d'anomalie avec fallback
   - Planification maintenance optimale (OR-Tools)
4. ✅ **Diagramme de déploiement** - Architecture Kubernetes production
5. ✅ **Modèle de données** - ERD PostgreSQL + Schéma TimescaleDB
6. ✅ **Flux de messages Kafka** - Topics, schémas Avro, partitionnement
7. ✅ **Diagrammes de flux de données** - Pipeline ML (training + inference)
8. ✅ **Matrices de décision** - Routage événements, criticité assets
9. ✅ **Patterns d'architecture** - Event Sourcing, Circuit Breaker, Saga
10. ✅ **Documentation des interfaces** - Spec OpenAPI pour API REST

**Format**: Markdown avec diagrammes **Mermaid** (natifs GitHub)

**Taille**: ~1200 lignes de documentation complète

---

### 2. **architecture.puml** (Diagramme PlantUML)

**Chemin**: `docs/diagrams/architecture.puml`

**Contenu**:

- Architecture complète MANTIS en un seul diagramme
- 7 packages principaux:
  - Niveau OT (PLC, SCADA, OPC UA)
  - Ingestion Layer (Java)
  - Processing Layer (Java + Python)
  - ML/AI Layer (Python)
  - Orchestration Layer (Java)
  - Presentation Layer (React)
  - Infrastructure (Kafka, DBs, MLOps)
- Connexions détaillées entre tous les composants
- Notes explicatives pour chaque service clé

**Format**: PlantUML (peut être rendu en PNG/SVG)

**Avantages**:

- Très détaillé et professionnel
- Exportable en haute résolution
- Utilisable dans des présentations PowerPoint

---

### 3. **README.md** (Guide d'utilisation)

**Chemin**: `docs/diagrams/README.md`

**Contenu**:

- 📖 Guide complet d'utilisation des diagrammes
- 🔧 3 options pour visualiser (VS Code, en ligne, GitHub)
- 📊 Tableau des types de diagrammes disponibles
- 🎯 Recommandations par rôle (Backend, Frontend, DevOps, ML, PM)
- 🔄 Scripts de génération automatique
- 📐 Conventions de diagrammes (couleurs, icônes, nommage)
- 🛠️ Guide d'édition et contribution
- 📚 Ressources et outils recommandés

**Public cible**: Toute l'équipe (dev, ops, PM, stakeholders)

---

## 🎨 Types de diagrammes disponibles

| Type | Outil | Localisation | Cas d'usage |
|------|-------|--------------|-------------|
| **Vue d'ensemble** | Mermaid | DESIGN_DIAGRAMS.md §1 | Présentation générale |
| **Architecture C4 - Contexte** | Mermaid | DESIGN_DIAGRAMS.md §2.1 | Stakeholders, systèmes externes |
| **Architecture C4 - Conteneurs** | Mermaid | DESIGN_DIAGRAMS.md §2.2 | Microservices, bases de données |
| **Architecture C4 - Composants** | Mermaid | DESIGN_DIAGRAMS.md §2.3 | Structure interne service Ingestion |
| **Séquence - Flux E2E** | Mermaid | DESIGN_DIAGRAMS.md §3.1 | Traçage complet données |
| **Séquence - Anomalie** | Mermaid | DESIGN_DIAGRAMS.md §3.2 | Détection et fallback |
| **Séquence - Planification** | Mermaid | DESIGN_DIAGRAMS.md §3.3 | Optimisation OR-Tools |
| **Déploiement Kubernetes** | Mermaid | DESIGN_DIAGRAMS.md §4.1 | Production deployment |
| **Quotas Kubernetes** | Tableau | DESIGN_DIAGRAMS.md §4.2 | Dimensionnement ressources |
| **ERD PostgreSQL** | Mermaid | DESIGN_DIAGRAMS.md §5.1 | Schéma base métadonnées |
| **Schéma TimescaleDB** | Mermaid | DESIGN_DIAGRAMS.md §5.2 | Séries temporelles |
| **Topics Kafka** | Mermaid | DESIGN_DIAGRAMS.md §6.1 | Flux de messages |
| **Schéma Avro** | JSON | DESIGN_DIAGRAMS.md §6.2 | Format sensor.raw |
| **Pipeline ML Training** | Mermaid | DESIGN_DIAGRAMS.md §7.1 | Entraînement modèles |
| **Pipeline ML Inference** | Mermaid | DESIGN_DIAGRAMS.md §7.2 | Prédiction RUL online |
| **Event Sourcing** | Mermaid | DESIGN_DIAGRAMS.md §9.1 | Pattern architectural |
| **Circuit Breaker** | Mermaid | DESIGN_DIAGRAMS.md §9.2 | Résilience |
| **Saga Pattern** | Mermaid | DESIGN_DIAGRAMS.md §9.3 | Transactions distribuées |
| **API OpenAPI** | YAML | DESIGN_DIAGRAMS.md §10.1 | Spécification REST API |
| **Architecture Complète** | PlantUML | diagrams/architecture.puml | Vue globale détaillée |

---

## 🚀 Comment utiliser

### Pour les développeurs

**1. Consulter la vue d'ensemble**:

```bash
# Ouvrir dans GitHub ou VS Code avec preview Markdown
open docs/DESIGN_DIAGRAMS.md
```

**2. Trouver votre service**:

- Backend Java → Section 2.3 (Composants)
- Backend Python → Section 3 (Séquences)
- Frontend React → Section 2.2 (Conteneurs)

**3. Comprendre les flux**:

- Flux E2E → Section 3.1
- Anomalies → Section 3.2
- Maintenance → Section 3.3

**4. Consulter le modèle de données**:

- PostgreSQL → Section 5.1
- TimescaleDB → Section 5.2

---

### Pour les DevOps

**1. Architecture Kubernetes**:

```bash
# Voir la section 4 de DESIGN_DIAGRAMS.md
# Quotas par service dans section 4.2
```

**2. Générer le diagramme PlantUML en PNG**:

```bash
cd docs/diagrams
brew install plantuml graphviz
plantuml architecture.puml
open architecture.png
```

**3. Utiliser pour dimensionnement**:

- CPU/Memory requests/limits → Tableau §4.2
- Réplication → Diagramme §4.1
- Persistent Volumes → architecture.puml

---

### Pour les Product Owners

**1. Présenter l'architecture**:

- Ouvrir `docs/DESIGN_DIAGRAMS.md` §1 (Vue d'ensemble)
- Générer PNG du diagramme PlantUML
- Utiliser dans PowerPoint/Google Slides

**2. Expliquer les flux métier**:

- Flux de maintenance → §3.3
- Détection anomalie → §3.2

**3. Planifier les sprints**:

- Identifier dépendances → §2.2 (Conteneurs)
- Estimer complexité → Nombre de connexions

---

### Pour les Data Scientists

**1. Pipeline ML**:

- Training → §7.1
- Inference → §7.2

**2. Features disponibles**:

- Schéma TimescaleDB → §5.2
- Topics Kafka → §6.1

**3. Intégration modèles**:

- MLflow workflow → §7.1
- API FastAPI → §10.1

---

## 📐 Conventions visuelles

### Couleurs standardisées

```
Java/Spring Boot:  #FFA726 (Orange) ☕
Python/FastAPI:    #66BB6A (Vert)   🐍
React/Next.js:     #42A5F5 (Bleu)   ⚛️
Kafka:             #231F20 (Noir)
Bases de données:  #0277BD (Bleu foncé) 📊
Alertes critiques: #F44336 (Rouge)  ⚠️
```

### Icônes

| Icône | Signification |
|-------|---------------|
| ☕ | Service Java/Spring Boot |
| 🐍 | Service Python/FastAPI |
| ⚛️ | Service React/Next.js |
| 📊 | Base de données |
| 🔄 | Event streaming (Kafka) |
| ⚠️ | Alerte ou anomalie |
| 🎯 | Objectif métier |
| 🔧 | Configuration |
| 📈 | Monitoring |

---

## 🔄 Maintenance des diagrammes

### Quand mettre à jour ?

| Événement | Diagrammes à mettre à jour |
|-----------|----------------------------|
| Ajout d'un nouveau service | §1 (Vue), §2.2 (Conteneurs), architecture.puml |
| Nouveau flux de données | §3 (Séquences), §6.1 (Topics Kafka) |
| Modification schéma BDD | §5.1 (ERD) ou §5.2 (TimescaleDB) |
| Changement infra K8s | §4.1 (Déploiement), §4.2 (Quotas) |
| Nouveau pattern | §9 (Patterns) |

### Workflow de mise à jour

```bash
# 1. Créer une branche
git checkout -b docs/update-architecture-diagram

# 2. Éditer les fichiers
# - docs/DESIGN_DIAGRAMS.md (Mermaid)
# - docs/diagrams/architecture.puml (PlantUML)

# 3. Vérifier le rendu
# Option A: GitHub preview
# Option B: VS Code avec extensions
# Option C: https://mermaid.live ou http://plantuml.com

# 4. Générer les PNG/SVG (optionnel)
cd docs/diagrams
plantuml architecture.puml

# 5. Commit
git add docs/
git commit -m "docs: update architecture diagram for new RUL service"

# 6. Push et PR
git push origin docs/update-architecture-diagram
gh pr create --title "Update architecture diagrams"
```

---

## 🎓 Ressources pour aller plus loin

### Apprendre Mermaid

- **Documentation officielle**: <https://mermaid.js.org/intro>
- **Tutoriel interactif**: <https://mermaid.live>
- **Exemples**: <https://github.com/mermaid-js/mermaid/tree/develop/demos>

### Apprendre PlantUML

- **Guide de démarrage**: <https://plantuml.com/fr/starting>
- **Galerie d'exemples**: <https://real-world-plantuml.com>
- **Stdlib C4**: <https://github.com/plantuml-stdlib/C4-PlantUML>

### Apprendre le modèle C4

- **Site officiel**: <https://c4model.com>
- **eBook gratuit**: <https://leanpub.com/visualising-software-architecture>
- **Vidéo**: "Visualising Software Architecture" par Simon Brown

### Outils complémentaires

- **Excalidraw**: Pour diagrammes à main levée (<https://excalidraw.com>)
- **Draw.io**: Pour diagrammes complexes (<https://app.diagrams.net>)
- **Structurizr**: Pour C4 model (<https://structurizr.com>)

---

## ✅ Checklist d'utilisation

### Pour le développeur qui rejoint l'équipe

- [ ] Lire `docs/DESIGN_DIAGRAMS.md` §1 (Vue d'ensemble)
- [ ] Consulter §2.2 (Conteneurs) pour identifier son service
- [ ] Étudier §3 (Séquences) pour comprendre les flux
- [ ] Lire §5 (ERD) pour le modèle de données
- [ ] Installer VS Code extension PlantUML + Mermaid
- [ ] Générer le PNG de architecture.puml

### Pour le PM qui présente le projet

- [ ] Générer PNG haute résolution de architecture.puml
- [ ] Préparer slides avec §1 (Vue d'ensemble)
- [ ] Imprimer §2.1 (Contexte C4) pour stakeholders
- [ ] Capturer §3.1 (Flux E2E) pour démo
- [ ] Préparer tableau §4.2 (Quotas) pour budget

### Pour le DevOps qui déploie

- [ ] Étudier §4.1 (Déploiement K8s)
- [ ] Valider §4.2 (Quotas) avec l'équipe infra
- [ ] Vérifier architecture.puml pour networking
- [ ] Documenter toute différence avec le diagramme
- [ ] Mettre à jour après déploiement

---

## 📊 Métriques de qualité

### Objectifs

- ✅ **Couverture**: 100% des services documentés
- ✅ **Précision**: Diagrammes synchronisés avec le code
- ✅ **Lisibilité**: Pas plus de 30 éléments par diagramme
- ✅ **Accessibilité**: Formats multiples (Mermaid, PlantUML, PNG, SVG)
- ✅ **Maintenance**: Mise à jour à chaque sprint review

### Indicateurs de succès

| Indicateur | Objectif | Actuel |
|------------|----------|--------|
| Services documentés | 7/7 | ✅ 7/7 |
| Flux documentés | >3 | ✅ 3 |
| Diagrammes à jour | 100% | ✅ 100% |
| Formats disponibles | >2 | ✅ 3 (Mermaid, PlantUML, Markdown) |
| Temps onboarding | <2h | 🎯 À mesurer |

---

## 🤝 Contribution

### Améliorer les diagrammes

**Vous avez trouvé une erreur ?**

1. Ouvrir une issue GitHub avec le label `documentation`
2. Préciser le diagramme concerné (section + numéro)
3. Proposer une correction

**Vous voulez ajouter un diagramme ?**

1. Consulter `docs/diagrams/README.md` pour les conventions
2. Créer une branche `docs/add-XXX-diagram`
3. Ajouter le diagramme Mermaid dans `DESIGN_DIAGRAMS.md`
4. Créer une PR avec description du cas d'usage

---

## 📞 Support

### Questions fréquentes

**Q: Comment afficher les diagrammes Mermaid dans VS Code ?**

R: Installer l'extension "Markdown Preview Mermaid Support" et ouvrir DESIGN_DIAGRAMS.md avec Cmd+Shift+V

**Q: Les diagrammes PlantUML ne se génèrent pas**

R: Vérifier que Graphviz est installé: `brew install graphviz`

**Q: Comment exporter en PowerPoint ?**

R: Générer PNG avec `plantuml architecture.puml` puis insérer dans PPT

**Q: Les diagrammes sont-ils versionnés ?**

R: Oui, dans Git. Les PNG/SVG générés sont dans `.gitignore`

### Contact

- **Questions architecture**: Ouvrir issue GitHub `architecture`
- **Questions documentation**: Ouvrir issue GitHub `documentation`
- **Questions urgentes**: Slack channel `#mantis-architecture`

---

## 🎉 Résumé

**Ce qui a été créé**:

✅ **1200+ lignes** de documentation diagrammes
✅ **20+ diagrammes** professionnels (Mermaid + PlantUML)
✅ **3 fichiers** de référence (DESIGN_DIAGRAMS.md, architecture.puml, README.md)
✅ **10 sections** de documentation complète
✅ **Guides d'utilisation** par rôle (dev, ops, PM, ML)

**Bénéfices pour l'équipe**:

🎯 **Vision partagée** de l'architecture MANTIS
📚 **Onboarding facilité** pour nouveaux membres
🔄 **Communication améliorée** avec stakeholders
📐 **Standards de qualité** documentés
🛠️ **Outils et workflows** définis

---

**Créé le**: 2025-01-21
**Version**: 1.0.0
**Statut**: ✅ Complet et prêt à l'emploi
**Prochain review**: Sprint Review suivant

**Auteur**: MANTIS Architecture Team
**Validé par**: Product Owner + Lead Dev

---

> 💡 **Conseil**: Commencez par `docs/DESIGN_DIAGRAMS.md` §1 pour une vue d'ensemble, puis naviguez selon votre rôle. Bonne exploration !
