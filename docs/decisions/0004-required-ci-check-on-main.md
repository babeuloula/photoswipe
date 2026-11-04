# 0004 — CI obligatoire avant tout merge sur main

- **Statut** : accepté
- **Date** : 2026-11-04
- **Ticket** : PS-6

## Contexte
Le ticket PS-6 demande que la CI soit bloquante quand le lint ou les tests échouent. Le job GitHub Actions échoue bien dans ce cas, mais sans protection de `main`, une PR rouge reste fusionnable : l'échec est visible, pas bloquant.

## Décision
Un ruleset GitHub « main: CI required » protège `main` : le check `Quality, unit tests and debug build` (le job de `.github/workflows/ci.yml`) doit être vert avant le merge, et la branche ne peut être ni supprimée ni réécrite par un force push. Aucune revue n'est exigée et les administrateurs peuvent contourner la règle en cas d'urgence.

## Alternatives envisagées
- Pas de protection : l'échec reste purement informatif, et un oubli suffit à casser `main`.
- Protection de branche « classique » : fonctionne, mais GitHub recommande désormais les rulesets, plus lisibles et versionnables par l'API.
- Revue obligatoire : inutile pour un projet à un seul développeur ; la revue reste un processus (CLAUDE.md), pas une contrainte technique.

## Conséquences
- Une PR ne peut être fusionnée que si sa CI est verte.
- Le ruleset référence le nom du job : renommer le job sans mettre à jour le ruleset bloque toutes les PR.
- Le ruleset vit dans les réglages du dépôt, pas dans le code : on l'inspecte avec `gh api repos/babeuloula/photoswipe/rulesets`.
