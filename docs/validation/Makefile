# Makefile pour compiler le guide de validation MANTIS

# Variables
LATEX = pdflatex
BIBTEX = bibtex
MAIN = guide-validation-mantis
SRC = $(MAIN).tex
PDF = $(MAIN).pdf

# Couleurs
GREEN = \033[0;32m
YELLOW = \033[1;33m
NC = \033[0m

.PHONY: all clean help view

all: $(PDF)
	@echo "$(GREEN)✓ Compilation terminée : $(PDF)$(NC)"

$(PDF): $(SRC)
	@echo "$(YELLOW)Compilation du document LaTeX...$(NC)"
	@$(LATEX) $(SRC)
	@echo "$(YELLOW)Deuxième passe pour les références...$(NC)"
	@$(LATEX) $(SRC)
	@echo "$(YELLOW)Troisième passe finale...$(NC)"
	@$(LATEX) $(SRC)
	@echo "$(GREEN)✓ Compilation réussie!$(NC)"

clean:
	@echo "$(YELLOW)Nettoyage des fichiers temporaires...$(NC)"
	@rm -f *.aux *.log *.toc *.out *.lof *.lot *.fls *.fdb_latexmk *.synctex.gz
	@echo "$(GREEN)✓ Nettoyage terminé$(NC)"

clean-all: clean
	@echo "$(YELLOW)Suppression du PDF...$(NC)"
	@rm -f $(PDF)
	@echo "$(GREEN)✓ Nettoyage complet terminé$(NC)"

view: $(PDF)
	@echo "$(YELLOW)Ouverture du PDF...$(NC)"
	@if command -v open >/dev/null 2>&1; then \
		open $(PDF); \
	elif command -v xdg-open >/dev/null 2>&1; then \
		xdg-open $(PDF); \
	elif command -v evince >/dev/null 2>&1; then \
		evince $(PDF); \
	else \
		echo "$(YELLOW)Ouvrir manuellement: $(PDF)$(NC)"; \
	fi

help:
	@echo "Commandes disponibles:"
	@echo "  make          - Compiler le PDF"
	@echo "  make all      - Compiler le PDF"
	@echo "  make clean    - Nettoyer fichiers temporaires"
	@echo "  make clean-all- Nettoyer tout (y compris PDF)"
	@echo "  make view     - Compiler et ouvrir le PDF"
	@echo "  make help     - Afficher cette aide"

.DEFAULT_GOAL := all
