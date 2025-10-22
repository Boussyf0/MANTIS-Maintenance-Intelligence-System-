# Système de Validation MANTIS

Ce document explique comment le système de validation automatique de MANTIS garantit la qualité du code à chaque push.

## Vue d'ensemble

MANTIS utilise un système de validation multi-niveaux :

1. **Git Hooks locaux** - Validation avant commit/push
2. **GitHub Actions CI/CD** - Validation sur le serveur
3. **Scripts de validation** - Validation manuelle du projet

## 🔧 Installation

### 1. Installer les Git Hooks

```bash
# À la racine du projet
./scripts/install-hooks.sh
```

Cela configure :
- `pre-commit` : Validation avant chaque commit
- `commit-msg` : Vérification du format du message
- `pre-push` : Tests avant chaque push

### 2. Configurer le template de commit

```bash
git config commit.template .gitmessage
```

## 📋 Git Hooks - Détails

### Pre-commit Hook

**Déclenché** : Avant chaque `git commit`

**Vérifie** :
- ✅ Fichiers sensibles (`.env`, `credentials.json`, `*.key`, etc.)
- ✅ Compilation et tests des services Java modifiés (Maven)
- ✅ Formatage Python (flake8, black)
- ✅ Conflits de merge non résolus (`<<<<<<< HEAD`)

**Exemple de sortie** :
```
🔍 MANTIS Pre-commit Hook - Validation en cours...

[1/6] Vérification des fichiers staged...
✓ Fichiers staged trouvés

[2/6] Vérification des fichiers sensibles...
✓ Pas de fichiers sensibles détectés

[3/6] Vérification du code Java...
  Compilation et tests pour services/ingestion-iiot...
✓ Code Java valide

[4/6] Vérification du code Python...
✓ Code Python valide

[5/6] Préparation du message de commit...
✓ Message de commit sera vérifié

[6/6] Vérification des conflits de merge...
✓ Pas de conflits de merge

═══════════════════════════════════════
✓ Toutes les vérifications sont passées!
═══════════════════════════════════════
```

### Commit-msg Hook

**Déclenché** : Après avoir écrit le message de commit

**Vérifie** :
- ✅ Format Conventional Commits : `<type>(<scope>): <description>`
- ✅ Longueur du titre : 10-72 caractères
- ✅ Description en minuscule, sans point final
- ✅ Ligne vide entre titre et corps

**Format obligatoire** :
```
<type>(<scope>): <description>

<body optionnel>

<footer optionnel>
```

**Types valides** :
- `feat` : Nouvelle fonctionnalité
- `fix` : Correction de bug
- `docs` : Documentation
- `style` : Formatage
- `refactor` : Refactoring
- `perf` : Performance
- `test` : Tests
- `build` : Build system
- `ci` : CI/CD
- `chore` : Maintenance

**Exemples valides** :
```bash
feat(ingestion): ajouter support pour Modbus TCP
fix(rul): corriger prédiction pour RUL < 24h
docs(readme): mettre à jour les instructions d'installation
refactor(preprocessing): optimiser le pipeline de nettoyage
```

**Exemples invalides** :
```bash
❌ Added new feature              # Pas de type
❌ feat: Added feature.           # Point final interdit
❌ FEAT(ingestion): add support   # Type en majuscule
❌ feat add support                # Manque les deux-points
```

### Pre-push Hook

**Déclenché** : Avant chaque `git push`

**Vérifie** :
- ✅ Branche actuelle (demande confirmation pour main/master)
- ✅ Working directory propre (pas de modifications non commitées)
- ✅ Tests Java (Maven)
- ✅ Tests Python (pytest)
- ✅ Connexion au repository distant

**Exemple** :
```
╔═══════════════════════════════════════╗
║   MANTIS Pre-push Hook - Validation  ║
╚═══════════════════════════════════════╝

[1/5] Vérification de la branche...
✓ Branche: feature/nouveau-service

[2/5] Vérification de l'état du working directory...
✓ Working directory propre

[3/5] Exécution des tests Java...
  Testing services/ingestion-iiot...
✓ Tous les tests Java passent

[4/5] Exécution des tests Python...
✓ Tous les tests Python passent

[5/5] Vérification de la connexion au repository distant...
✓ Connexion OK

═══════════════════════════════════════
✓ Toutes les vérifications sont passées!
✓ Push autorisé vers feature/nouveau-service
═══════════════════════════════════════
```

## 🤖 GitHub Actions CI/CD

**Déclenché** : À chaque push ou pull request

**Workflows** :

### 1. Validate Commit Messages
- Vérifie tous les messages de commit de la PR
- Format Conventional Commits obligatoire

### 2. Code Quality Checks
- Détection de fichiers sensibles
- Scan de TODO/FIXME

### 3. Test Java Services
- Compilation Maven
- Tests unitaires (JUnit 5)
- Génération de rapports (Surefire)
- Couverture de code (JaCoCo)

### 4. Test Python Services
- Lint (flake8)
- Formatage (black)
- Tests (pytest)
- Couverture (pytest-cov)

### 5. Integration Tests
- Tests d'intégration avec Testcontainers
- PostgreSQL, Kafka

### 6. Docker Build
- Construction des images Docker
- Validation des Dockerfiles

### 7. Security Scan
- Scan de vulnérabilités avec Trivy
- Upload vers GitHub Security

### 8. Deployment Ready
- Vérifie que tous les checks sont passés
- Marque comme prêt pour le déploiement

## 🔍 Scripts de Validation

### validate-project.sh

Valide la structure complète du projet :

```bash
./scripts/validate-project.sh
```

**Vérifie** :
1. Structure des répertoires
2. Fichiers de configuration
3. Services Java (compilation, tests)
4. Services Python
5. Infrastructure Docker
6. Scripts
7. Git Hooks
8. GitHub Actions

**Exemple de sortie** :
```
╔═══════════════════════════════════════╗
║   MANTIS - Validation du Projet      ║
╚═══════════════════════════════════════╝

[1/8] Vérification de la structure des répertoires...
✓ services
✓ infrastructure/docker
✓ data
✓ scripts
✓ tests
✓ docs

[2/8] Vérification des fichiers de configuration...
✓ README.md
✓ ARCHITECTURE.md
✓ CONTRIBUTING.md
✓ Makefile
✓ .gitignore
✓ requirements.txt

... [output continué] ...

═══════════════════════════════════════
         RÉSUMÉ DE LA VALIDATION
═══════════════════════════════════════
✓ Projet entièrement valide!
✓ 0 erreurs, 0 avertissements
```

## 🚫 Désactiver temporairement les hooks

**⚠️ Déconseillé**, mais parfois nécessaire :

```bash
# Ignorer le hook pre-commit
git commit --no-verify

# Ignorer le hook pre-push
git push --no-verify
```

**Cas d'usage légitimes** :
- Commit WIP (Work In Progress) sur une branche de développement
- Urgence de production (avec validation manuelle après)
- Tests de hooks eux-mêmes

## 📊 Métriques de qualité

### Coverage minimale
- Java : 80% (JaCoCo)
- Python : 80% (pytest-cov)

### Formats de code
- Java : Google Java Format
- Python : Black (line-length 120)

### Linting
- Java : SpotBugs, PMD
- Python : flake8, pylint

## 🔧 Dépannage

### Hook "permission denied"

```bash
chmod +x .githooks/pre-commit
chmod +x .githooks/commit-msg
chmod +x .githooks/pre-push
```

### Hooks non exécutés

Vérifier que le chemin est configuré :

```bash
git config core.hooksPath
# Devrait afficher: .githooks
```

Si vide, réinstaller :

```bash
./scripts/install-hooks.sh
```

### Tests Java échouent

```bash
cd services/ingestion-iiot
mvn clean test -X  # Mode debug
```

### Tests Python échouent

```bash
cd services/mon-service
pytest -v --tb=short  # Traceback court
```

### Commit message rejeté

Utiliser le template :

```bash
git commit  # Sans -m, ouvre l'éditeur avec le template
```

## 📚 Ressources

- [Conventional Commits](https://www.conventionalcommits.org/)
- [Git Hooks Documentation](https://git-scm.com/book/en/v2/Customizing-Git-Git-Hooks)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [CONTRIBUTING.md](CONTRIBUTING.md) - Guide de contribution complet

## ❓ FAQ

**Q: Puis-je modifier les hooks ?**
R: Oui, les hooks sont dans `.githooks/`. Modifiez-les et testez avant de committer.

**Q: Les hooks ralentissent mes commits**
R: Les hooks exécutent uniquement les tests des services modifiés. Pour les désactiver temporairement : `git commit --no-verify`

**Q: Comment tester un hook avant de committer ?**
R: Exécutez directement : `.githooks/pre-commit` ou `.githooks/commit-msg <fichier>`

**Q: Les hooks fonctionnent-ils sur Windows ?**
R: Oui, mais nécessite Git Bash ou WSL2.

**Q: GitHub Actions échoue mais pas les hooks locaux**
R: Assurez-vous d'avoir les mêmes versions de Java/Python/Maven localement. Vérifiez `.github/workflows/ci.yml`.

---

**Maintenu par** : MANTIS Team - EMSI
**Dernière mise à jour** : 2025-01-22
