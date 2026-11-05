# 0019 — Journal des décisions pour l'undo

- **Statut** : accepté
- **Date** : 2026-11-05
- **Ticket** : PS-57

## Contexte
L'ADR 0010 gardait la pile d'undo dans l'état de l'écran : elle disparaissait en quittant la pile ou à la mort du process. Jérémy a décidé (PS-46) qu'« Annuler » doit remonter les décisions déjà prises d'un jour, même après être sorti de la pile ou après une fermeture de l'app (SPEC §3.4). Il faut donc persister l'historique, avec l'entrée que chaque décision a remplacée.

## Décision
- **Table `decision_journal`** dans la base Room (version 2, migration automatique 1 → 2) : une ligne par décision, avec son jour (`day`, jours depuis l'epoch), l'entrée écrite (`made_*`) et l'entrée remplacée (`previous_*`, nulle si le média était à trier). L'identifiant auto-incrémenté ordonne les décisions d'un jour.
- **Port `DecisionJournal`** dans `:domain` : `record`, `undo`, `recordsFor(day)`, `forget(mediaIds)`, `forgetDaysBefore(day)`. Écrire une décision et la journaliser, ou l'annuler et l'effacer, se fait dans **une seule transaction** (`DecisionJournalDao`) : le journal et les décisions restent d'accord après un arrêt brutal.
- **`SessionDecision` porte sa ligne de journal** (`DecisionRecord`) et le média. Le côté de sortie de la carte (gauche, droite, haut) se lit sur l'entrée écrite au swipe : il n'est plus gardé à part par l'écran, et survit donc lui aussi.
- **`GetDaySession`** reprend la session d'un jour à l'ouverture de la pile : décisions du journal dont le média est encore lisible et encore décidé pour l'année de revue. Il oublie au passage les jours sortis de la fenêtre de revue.
- **Fin de l'undo** : une suppression confirmée (`RecordTrashResult`) et la purge des orphelins (`CleanOrphanEntries`) retirent les médias concernés du journal.

## Alternatives envisagées
- **Retrouver la session par `decidedAt`** dans `review_entry` : l'entrée remplacée serait perdue, et l'undo d'un favori posé sur un média gardé le rendrait à trier (déjà écarté par l'ADR 0010).
- **`SavedStateHandle`** : survit à la mort du process, mais pas à la sortie de la pile ni à une fermeture volontaire de l'app.
- **Deux écritures séparées** (décision puis journal) : un arrêt entre les deux laisserait une décision sans undo ou un undo sans décision.

## Conséquences
- L'ADR 0010 reste valable pour la forme de la pile (`ReviewSession` immuable, `UndoLastDecision`) ; seule sa durée de vie change.
- Chaque décision coûte une insertion de plus, dans la même transaction.
- Le journal reste petit : 7 jours au plus, et il se vide à chaque confirmation.
