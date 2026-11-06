# 0026 — Retrier un jour déjà trié, en gardant la décision d'avant

- **Statut** : accepté
- **Date** : 2026-11-06
- **Ticket** : PS-108

## Contexte
Un jour tout trié était inactif sur l'accueil : impossible de revenir sur ses choix (une photo gardée par erreur, un marquage à rattraper). Le ticket prévoyait d'effacer les décisions du jour puis de rouvrir sa pile. Jérémy a ensuite demandé que, en retriant, chaque carte montre ce qu'il avait décidé la dernière fois (gardé, marqué, favori). Une décision effacée ne peut plus être montrée ; un état en mémoire ou dans la route se perdrait en quittant la pile ou à la mort du process (SPEC §3.4).

## Décision
- **Rouvrir au lieu d'effacer** : `ReopenDay` marque `reopened = true` les entrées du jour (année de revue du jour) dont le média est encore dans la galerie, sauf `TRASHED`. La règle « une entrée retire le média de la pile » devient « une entrée **non rouverte** » (`ReviewEntry.isSorted`) : `GetDeckForDay`, `GetReviewDays` et `GetDaySession` la suivent, donc le nombre de l'accueil remonte.
- **Un marquage rouvert n'est plus en attente** : `entriesMarkedForDeletion` et `markTrashed` l'ignorent (bannière, résumé, corbeille).
- **La nouvelle décision remplace l'ancienne** par le chemin habituel (`DecisionRecorder`) : l'entrée rouverte devient l'entrée remplacée du journal, et l'undo la restaure, rouverte, avec sa pastille.
- **Journal du jour oublié** à la réouverture : ses décisions ne sont plus annulables (leurs médias sont de nouveau sur la pile) et ne comptent plus dans « Trié aujourd'hui ».
- **Pastille sur la carte** : `GetPreviousDecisions` lit les entrées rouvertes du paquet ; coche pour un gardé, corbeille pour un marqué (« Marqué pour suppression la dernière fois », pas « Supprimé » : un média supprimé ne revient pas) ; le favori garde son cœur (PS-86).
- **Pas de dialogue de confirmation** avant de retrier : rien n'est perdu tant qu'on ne trie pas de nouveau, à l'exception des marquages qui quittent l'attente de suppression (ils reviennent à trier, avec leur pastille).
- **Base en version 3** : colonne `reopened` (`review_entry`, et `made_reopened` / `previous_reopened` du journal), par une migration manuelle (`MIGRATION_2_3`) : l'entrée remplacée du journal est facultative et Room la lit absente quand toutes ses colonnes sont nulles, ce qu'une valeur par défaut sur `previous_reopened` aurait cassé.

## Alternatives envisagées
- **Effacer les décisions du jour** (ticket d'origine) : plus simple, mais rend la pastille impossible.
- **Mode « retrier » passé par la route** (`DeckRoute(epochDay, resort = true)`), sans migration : le nombre de l'accueil reste à 0 et l'état se perd en quittant la pile.
