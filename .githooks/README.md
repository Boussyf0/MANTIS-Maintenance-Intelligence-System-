# Git Hooks MANTIS

Ce répertoire contient les hooks Git personnalisés pour garantir la qualité du code dans MANTIS.

## 📁 Hooks disponibles

### pre-commit
**Exécuté** : Avant chaque `git commit`

**Fonction** : Valide le code avant de créer le commit

**Vérifications** :
- ✅ Détecte les fichiers sensibles (secrets, credentials)
- ✅ Compile et teste les services Java modifiés
- ✅ Vérifie le formatage Python (flake8, black)
- ✅ Détecte les conflits de merge non résolus

**Bypass** (déconseillé) :
```bash
git commit --no-verify
```

### commit-msg
**Exécuté** : Après avoir écrit le message de commit

**Fonction** : Vérifie le format du message de commit

**Format requis** :
```
<type>(<scope>): <description>
```

**Types valides** :
`feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`

**Exemples valides** :
- `feat(ingestion): ajouter support pour Modbus TCP`
- `fix(rul): corriger prédiction pour RUL < 24h`
- `docs(readme): mettre à jour les instructions`

**Exemples invalides** :
- ❌ `Added new feature` (pas de type)
- ❌ `feat: Added feature.` (point final)
- ❌ `FEAT(ingestion): add` (type en majuscule)

### pre-push
**Exécuté** : Avant chaque `git push`

**Fonction** : Exécute tous les tests avant le push

**Vérifications** :
- ✅ Branche actuelle (demande confirmation pour main/master)
- ✅ Working directory propre
- ✅ Tests Java (Maven)
- ✅ Tests Python (pytest)
- ✅ Connexion au repository distant

**Bypass** (déconseillé) :
```bash
git push --no-verify
```

## 🔧 Installation

### Automatique (recommandé)
```bash
./scripts/install-hooks.sh
```

### Manuelle
```bash
# Configurer le chemin
git config core.hooksPath .githooks

# Rendre exécutable
chmod +x .githooks/*
```

## 📊 Vérification

### Vérifier que les hooks sont installés
```bash
git config core.hooksPath
# Devrait afficher: .githooks
```

### Lister les hooks
```bash
ls -la .githooks/
```

### Tester un hook manuellement
```bash
# Pre-commit
.githooks/pre-commit

# Commit-msg (nécessite un fichier de message)
echo "feat(test): tester le hook" > /tmp/test_msg
.githooks/commit-msg /tmp/test_msg
```

## 🎯 Workflow

### Commit normal (hooks activés)
```bash
# 1. Stage des fichiers
git add .

# 2. Commit (pre-commit s'exécute automatiquement)
git commit
# ou
git commit -m "feat(scope): description"

# 3. Push (pre-push s'exécute automatiquement)
git push
```

### Bypass temporaire (use with caution!)
```bash
# Ignorer pre-commit et commit-msg
git commit --no-verify -m "wip: work in progress"

# Ignorer pre-push
git push --no-verify
```

## ⚙️ Configuration

Les hooks utilisent ces outils :
- **Java** : Maven (pour compilation et tests)
- **Python** : flake8, black, pytest
- **Git** : Pour vérifications de statut

### Dépendances requises

#### Java
```bash
mvn --version
# Apache Maven 3.9+
# Java 17+
```

#### Python
```bash
python3 --version  # Python 3.11+
pip install flake8 black pytest
```

## 🐛 Dépannage

### Hook "permission denied"
```bash
chmod +x .githooks/pre-commit
chmod +x .githooks/commit-msg
chmod +x .githooks/pre-push
```

### Hooks ne s'exécutent pas
```bash
# Vérifier la configuration
git config core.hooksPath

# Si vide, configurer
git config core.hooksPath .githooks
```

### Maven non trouvé
```bash
# Vérifier l'installation
which mvn

# Installer Maven si nécessaire
# macOS : brew install maven
# Linux : sudo apt install maven
```

### Python tools non trouvés
```bash
# Installer les outils
pip install flake8 black pytest

# Vérifier
flake8 --version
black --version
pytest --version
```

## 📝 Modification des hooks

Les hooks sont des scripts bash dans `.githooks/`. Pour modifier :

1. Éditer le fichier correspondant
2. Tester localement
3. Commiter les modifications
4. Les autres développeurs devront réexécuter `./scripts/install-hooks.sh`

## 🔒 Sécurité

### Protection contre les secrets

Le hook `pre-commit` détecte automatiquement :
- Fichiers `.env` (sauf `.env.example`)
- Fichiers `credentials*.json`
- Fichiers `*.key`, `*.pem`
- Patterns : `password`, `secret`, `api_key`, `token`

### Exemple de blocage
```bash
$ git add .env
$ git commit -m "feat: add config"

🔍 MANTIS Pre-commit Hook - Validation en cours...

[2/6] Vérification des fichiers sensibles...
✗ Fichier sensible détecté : .env
  ATTENTION: Ne commitez jamais de secrets ou credentials!

═══════════════════════════════════════
✗ 1 erreur(s) détectée(s)
═══════════════════════════════════════

Commit annulé. Corrigez les erreurs et réessayez.
```

## 📚 Documentation complémentaire

- [README_VALIDATION.md](../README_VALIDATION.md) - Guide complet du système
- [CONTRIBUTING.md](../CONTRIBUTING.md) - Guide de contribution
- [.gitmessage](../.gitmessage) - Template de commit message

## 💡 Bonnes pratiques

### ✅ À faire
- Utiliser le template de commit (`.gitmessage`)
- Tester localement avant de commiter
- Lire les messages d'erreur des hooks
- Corriger les problèmes plutôt que de bypass

### ❌ À éviter
- Utiliser `--no-verify` systématiquement
- Commiter des fichiers sensibles
- Ignorer les messages d'erreur
- Push sans tester localement

## 🎓 Ressources

- [Git Hooks Documentation](https://git-scm.com/book/en/v2/Customizing-Git-Git-Hooks)
- [Conventional Commits](https://www.conventionalcommits.org/)
- [Bash Scripting Guide](https://www.gnu.org/software/bash/manual/)

---

**Maintenu par** : MANTIS Team - EMSI
**Version** : 1.0.0
**Dernière mise à jour** : 2025-10-22
