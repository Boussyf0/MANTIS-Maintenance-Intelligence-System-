# Guide de Validation MANTIS - Documentation LaTeX

Ce répertoire contient le guide complet du système de validation MANTIS au format LaTeX.

## 📄 Fichiers

- **guide-validation-mantis.tex** - Document LaTeX principal
- **Makefile** - Compilation automatique
- **README.md** - Ce fichier

## 🔧 Prérequis

### Installation LaTeX

#### macOS
```bash
# Installer MacTeX (complet, ~4 GB)
brew install --cask mactex

# Ou BasicTeX (minimal, ~100 MB)
brew install --cask basictex
sudo tlmgr update --self
sudo tlmgr install collection-fontsrecommended
sudo tlmgr install collection-latexextra
```

#### Linux (Ubuntu/Debian)
```bash
sudo apt update
sudo apt install texlive-full
# Ou version minimale
sudo apt install texlive-latex-base texlive-latex-extra
```

#### Windows
- Télécharger MiKTeX : https://miktex.org/download
- Ou TeX Live : https://tug.org/texlive/

## 📝 Compilation

### Méthode 1 : Makefile (recommandé)

```bash
# Compiler le PDF
make

# Compiler et ouvrir
make view

# Nettoyer les fichiers temporaires
make clean

# Nettoyer tout (y compris le PDF)
make clean-all
```

### Méthode 2 : Manuelle

```bash
# Compiler (3 passes pour les références)
pdflatex guide-validation-mantis.tex
pdflatex guide-validation-mantis.tex
pdflatex guide-validation-mantis.tex

# Le PDF est généré : guide-validation-mantis.pdf
```

### Méthode 3 : Overleaf (en ligne)

1. Aller sur https://www.overleaf.com
2. Créer un nouveau projet
3. Uploader `guide-validation-mantis.tex`
4. Compiler en ligne

## 📖 Contenu du guide

Le guide est organisé par profil d'équipe :

### 1. Introduction
- Vue d'ensemble du système
- Architecture de validation
- Format Conventional Commits

### 2. Guide par Profil

#### 🔧 Développeur Backend (Java/Spring Boot)
- Installation et configuration
- Workflow quotidien
- Validations automatiques
- Commandes Maven
- Dépannage

#### 🐍 Data Scientist / Développeur ML (Python)
- Configuration environnement virtuel
- Workflow de développement
- Formatage et linting
- Tests pytest
- Bonnes pratiques MLflow

#### ⚛️ Développeur Frontend (React/Next.js)
- Installation dépendances
- Workflow de développement
- Tests et linting
- Build et déploiement

#### ⚙️ DevOps / Administrateur Système
- Configuration infrastructure
- CI/CD GitHub Actions
- Monitoring et logging
- Gestion des secrets
- Backup et restauration

#### 📊 Chef de Projet
- Vue d'ensemble du système
- Métriques de qualité
- Suivi des Pull Requests
- Rapports disponibles

### 3. Commandes Essentielles
- Makefile
- Scripts de validation

### 4. Dépannage
- Hooks Git
- Messages de commit
- Tests Java/Python
- Fichiers sensibles

### 5. FAQ
- Questions générales
- Questions techniques

### 6. Annexes
- Structure des fichiers
- Références
- Contacts

## 🎨 Personnalisation

### Ajouter un logo

Placez votre logo dans le même répertoire et nommez-le `logo.png`, ou modifiez la ligne dans le `.tex` :

```latex
\fancyhead[R]{\includegraphics[height=1cm]{logo.png}}
```

### Modifier les couleurs

Dans le préambule du `.tex` :

```latex
\definecolor{maincolor}{RGB}{0,102,204}
```

### Ajouter une section

```latex
\section{Nouvelle Section}
\subsection{Sous-section}
Contenu...
```

## 📊 Structure du document

```
guide-validation-mantis.tex
├── Préambule (packages, configuration)
├── Page de titre
├── Table des matières
├── Introduction
├── Format de Commit
├── Guide par Profil
│   ├── Développeur Backend
│   ├── Data Scientist
│   ├── Développeur Frontend
│   ├── DevOps
│   └── Chef de Projet
├── Commandes Essentielles
├── Dépannage
├── FAQ
└── Annexes
```

## 🔍 Vérification du PDF

Après compilation, vérifier :
- ✅ Table des matières complète
- ✅ Tous les liens hypertexte fonctionnent
- ✅ Syntaxe colorée dans les blocs de code
- ✅ Diagrammes TikZ affichés correctement
- ✅ Numérotation des pages

## 🐛 Dépannage de compilation

### Erreur : Package not found

```bash
# Installer les packages manquants (macOS/Linux)
sudo tlmgr install <package-name>

# Ou installer une collection complète
sudo tlmgr install collection-latexextra
```

### Erreur : Font warnings

```bash
# Installer les polices
sudo tlmgr install collection-fontsrecommended
```

### Compilation bloquée

```bash
# Nettoyer et recompiler
make clean
make
```

### Diagrammes TikZ ne s'affichent pas

Vérifier que le package `tikz` est installé :
```bash
sudo tlmgr install pgf
```

## 📤 Distribution

### Version imprimée
- Format : A4
- Marges : 2.5cm
- Recto-verso recommandé

### Version numérique
Le PDF généré inclut :
- Liens hypertexte cliquables
- Bookmarks pour navigation
- Métadonnées (titre, auteur)

## 🔄 Mise à jour du guide

Pour mettre à jour le guide :

1. Modifier `guide-validation-mantis.tex`
2. Recompiler :
   ```bash
   make clean
   make
   ```
3. Vérifier le PDF généré
4. Commiter les changements :
   ```bash
   git add guide-validation-mantis.tex
   git commit -m "docs(validation): mettre à jour guide LaTeX"
   ```

## 📋 Checklist avant distribution

- [ ] PDF compile sans erreurs
- [ ] Tous les liens fonctionnent
- [ ] Table des matières à jour
- [ ] Pas de "overfull hbox" warnings critiques
- [ ] Numérotation correcte
- [ ] Exemples de code à jour
- [ ] Captures d'écran actuelles (si ajoutées)
- [ ] Version et date à jour

## 💡 Conseils

### Pour un document plus léger

Commenter les packages non utilisés dans le préambule :
```latex
% \usepackage{packagename}
```

### Pour ajouter des images

```latex
\begin{figure}[h]
\centering
\includegraphics[width=0.8\textwidth]{image.png}
\caption{Description de l'image}
\label{fig:mon-image}
\end{figure}
```

### Pour des références croisées

```latex
Voir section~\ref{sec:ma-section}
Voir figure~\ref{fig:mon-image}
```

## 📚 Ressources LaTeX

- [Overleaf Documentation](https://www.overleaf.com/learn)
- [TikZ Examples](https://texample.net/tikz/)
- [LaTeX Wikibook](https://en.wikibooks.org/wiki/LaTeX)
- [CTAN](https://ctan.org/) - Package repository

## 🎯 Prochaines améliorations

- [ ] Ajouter captures d'écran des interfaces
- [ ] Diagrammes de flux plus détaillés
- [ ] Exemples de code plus complets
- [ ] Version anglaise du guide
- [ ] Version courte (quick start)

---

**Maintenu par** : MANTIS Team - EMSI
**Version** : 1.0
**Dernière mise à jour** : 2025-01-22
