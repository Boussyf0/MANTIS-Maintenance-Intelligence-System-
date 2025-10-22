# 🎯 Système de Validation MANTIS - Résumé de Configuration

## ✅ Ce qui a été configuré

### 1. Git Hooks (`.githooks/`)

#### **pre-commit**
Validation avant chaque commit :
- ✅ Détection de fichiers sensibles (`.env`, `credentials.json`, `*.key`)
- ✅ Compilation et tests Java des services modifiés (Maven)
- ✅ Vérification formatage Python (flake8, black)
- ✅ Détection de conflits de merge non résolus

#### **commit-msg**
Validation du format du message de commit :
- ✅ Format Conventional Commits : `<type>(<scope>): <description>`
- ✅ Longueur 10-72 caractères
- ✅ Description en minuscule, sans point final
- ✅ Types valides : feat, fix, docs, style, refactor, perf, test, build, ci, chore

#### **pre-push**
Tests complets avant chaque push :
- ✅ Vérification de la branche (confirmation pour main/master)
- ✅ Working directory propre
- ✅ Tests unitaires Java (Maven)
- ✅ Tests Python (pytest)
- ✅ Connexion au repository distant

### 2. GitHub Actions CI/CD (`.github/workflows/ci.yml`)

Pipeline automatique à chaque push/PR :
- ✅ **validate-commit-messages** : Vérifie tous les messages de commit
- ✅ **code-quality** : Scan de fichiers sensibles et qualité du code
- ✅ **test-java-services** : Compilation Maven + tests + coverage (JaCoCo)
- ✅ **test-python-services** : Lint (flake8) + format (black) + tests (pytest)
- ✅ **integration-tests** : Tests d'intégration avec Testcontainers
- ✅ **docker-build** : Construction et validation des images Docker
- ✅ **security-scan** : Scan de vulnérabilités avec Trivy
- ✅ **deployment-ready** : Vérification finale pour déploiement

### 3. Scripts de Validation (`scripts/`)

#### **install-hooks.sh**
Installation automatique des hooks Git :
```bash
./scripts/install-hooks.sh
# ou
make install-hooks
```

#### **validate-project.sh**
Validation complète de la structure du projet :
```bash
./scripts/validate-project.sh
# ou
make validate
```

Vérifie :
- Structure des répertoires
- Fichiers de configuration
- Services Java (compilation, tests)
- Services Python
- Infrastructure Docker
- Git Hooks
- GitHub Actions

#### **test-validation.sh**
Démonstration interactive du système :
```bash
./scripts/test-validation.sh
# ou
make test-validation
```

### 4. Template de Commit (`.gitmessage`)

Template automatique avec exemples et documentation :
```bash
# Configuration
git config commit.template .gitmessage

# Utilisation
git commit  # Ouvre l'éditeur avec le template
```

### 5. Makefile (mis à jour)

Nouvelles commandes :
```bash
make validate         # Valider le projet
make install-hooks    # Installer les hooks
make test-validation  # Démonstration
```

### 6. Documentation

- **README_VALIDATION.md** : Guide complet du système de validation
- **CONTRIBUTING.md** : Mis à jour avec les instructions de validation
- **CLAUDE.md** : Documentation pour Claude Code
- **VALIDATION_SETUP_SUMMARY.md** : Ce fichier

## 🚀 Démarrage Rapide

### Installation initiale

```bash
# 1. Installer les hooks Git
./scripts/install-hooks.sh

# 2. Configurer le template de commit
git config commit.template .gitmessage

# 3. Valider la configuration
./scripts/validate-project.sh

# 4. (Optionnel) Démonstration
./scripts/test-validation.sh
```

### Workflow de développement

```bash
# 1. Créer une branche
git checkout -b feature/ma-fonctionnalite

# 2. Développer
# ... modifications ...

# 3. Commit (le hook pre-commit validera automatiquement)
git commit
# Ou avec message direct
git commit -m "feat(ingestion): ajouter support Modbus TCP"

# 4. Push (le hook pre-push exécutera les tests)
git push origin feature/ma-fonctionnalite

# 5. Créer une Pull Request
# GitHub Actions CI/CD s'exécutera automatiquement
```

## 📋 Format de Commit Obligatoire

```
<type>(<scope>): <description>

<body optionnel>

<footer optionnel>
```

### Types valides

| Type | Description | Exemple |
|------|-------------|---------|
| `feat` | Nouvelle fonctionnalité | `feat(ingestion): ajouter support Modbus TCP` |
| `fix` | Correction de bug | `fix(rul): corriger prédiction RUL < 24h` |
| `docs` | Documentation | `docs(readme): mettre à jour installation` |
| `style` | Formatage | `style(preprocessing): formater selon PEP8` |
| `refactor` | Refactoring | `refactor(features): optimiser calcul FFT` |
| `perf` | Performance | `perf(anomaly): réduire latence de 30%` |
| `test` | Tests | `test(rul): ajouter tests LSTM` |
| `build` | Build system | `build(docker): optimiser image` |
| `ci` | CI/CD | `ci(actions): ajouter cache Maven` |
| `chore` | Maintenance | `chore(deps): mettre à jour PyTorch` |

### Scopes suggérés

`ingestion`, `preprocessing`, `features`, `anomaly`, `rul`, `orchestrator`, `dashboard`, `infrastructure`, `database`, `docs`, `tests`

## ⚠️ Points d'Attention

### Fichiers sensibles interdits

Le système détecte et bloque automatiquement :
- `*.env` (sauf `.env.example`)
- `*credentials*.json`
- `*.key`, `*.pem`
- Patterns : `password`, `secret`, `api_key`

### Coverage minimale

- Java : 80% (JaCoCo)
- Python : 80% (pytest-cov)

### Tous les tests doivent passer

- Tests unitaires
- Tests d'intégration
- Lint (flake8, pylint)
- Format (black, isort)

## 🔧 Commandes Utiles

### Validation

```bash
# Valider la structure complète
make validate

# Valider les hooks Git
git config core.hooksPath  # Devrait afficher: .githooks

# Tester un hook manuellement
.githooks/pre-commit
.githooks/commit-msg .git/COMMIT_EDITMSG
```

### Tests

```bash
# Tests Java
cd services/ingestion-iiot
mvn clean test

# Tests Python (quand disponibles)
cd services/preprocessing
pytest tests/

# Tests d'intégration
make test-integration
```

### Désactiver temporairement (déconseillé)

```bash
# Ignorer les hooks (use with caution!)
git commit --no-verify
git push --no-verify
```

## 📊 Vérification du Statut

### Hooks installés ?

```bash
ls -la .githooks/
git config core.hooksPath
```

### GitHub Actions configurées ?

```bash
ls -la .github/workflows/
cat .github/workflows/ci.yml
```

### Validation complète

```bash
./scripts/validate-project.sh
```

## 🎓 Ressources

### Documentation

- [README_VALIDATION.md](README_VALIDATION.md) - Guide complet
- [CONTRIBUTING.md](CONTRIBUTING.md) - Guide de contribution
- [CLAUDE.md](CLAUDE.md) - Pour Claude Code

### Standards

- [Conventional Commits](https://www.conventionalcommits.org/)
- [Git Hooks](https://git-scm.com/book/en/v2/Customizing-Git-Git-Hooks)
- [GitHub Actions](https://docs.github.com/en/actions)

## ✅ Checklist de Vérification

Avant de pusher, assurez-vous que :

- [ ] Les hooks Git sont installés (`./scripts/install-hooks.sh`)
- [ ] Le template de commit est configuré (`git config commit.template .gitmessage`)
- [ ] Le projet est valide (`./scripts/validate-project.sh`)
- [ ] Tous les tests passent localement
- [ ] Le format de commit est correct
- [ ] Pas de fichiers sensibles
- [ ] La documentation est à jour

## 🐛 Dépannage

### Hooks ne s'exécutent pas

```bash
# Vérifier la configuration
git config core.hooksPath

# Réinstaller
./scripts/install-hooks.sh

# Vérifier les permissions
chmod +x .githooks/*
```

### Commit message rejeté

```bash
# Utiliser le template
git commit  # Sans -m, ouvre l'éditeur

# Exemple valide
git commit -m "feat(ingestion): ajouter support Modbus"
```

### Tests échouent

```bash
# Java
cd services/ingestion-iiot
mvn clean test -X  # Mode debug

# Python
pytest -v --tb=short
```

## 📈 Métriques de Qualité

Le système garantit :
- ✅ 100% des commits suivent le format Conventional Commits
- ✅ 0 fichiers sensibles dans le repository
- ✅ Couverture de code ≥ 80%
- ✅ Tous les tests passent avant le push
- ✅ Build Docker réussi
- ✅ Aucune vulnérabilité de sécurité critique

## 🎉 Avantages

### Pour le développeur

- 🚀 Feedback immédiat sur la qualité du code
- 🛡️ Protection contre les erreurs communes
- 📝 Format de commit standardisé
- ⚡ Tests automatiques avant le push

### Pour l'équipe

- 📊 Historique Git propre et lisible
- 🔍 Traçabilité des changements
- 🤝 Standards de code uniformes
- 🔒 Sécurité renforcée

### Pour le projet

- ✅ Qualité du code garantie
- 🔄 CI/CD fiable
- 📈 Maintenabilité accrue
- 🎯 Productivité améliorée

---

**Système configuré et opérationnel ! 🎯**

Pour toute question :
- 📧 Email : O.ouedrhiri@emsi.ma, H.Tabbaa@emsi.ma, lachgar.m@gmail.com
- 📚 Documentation : [docs/](docs/)
- 💬 GitHub Discussions

**Dernière mise à jour** : 2025-01-22
**Maintenu par** : MANTIS Team - EMSI
