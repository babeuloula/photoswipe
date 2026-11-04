# 0010 — Pile d'undo de la session

- **Statut** : accepté
- **Date** : 2026-11-04
- **Ticket** : PS-20

## Contexte
La SPEC §3.4 demande un undo en pile : les décisions de la session s'annulent de la plus récente à la plus ancienne, tant que rien n'est confirmé. Les décisions sont écrites en base dès le swipe. Il faut décider où vit la pile, et ce que l'undo remet en base. PS-21 prévoit « favori sur un média déjà gardé, puis undo » : l'undo doit pouvoir rendre une décision antérieure, pas seulement supprimer l'entrée.

## Décision
- **`ReviewSession`, valeur immuable du package `model`**, porte la liste des `SessionDecision` de la session. La pile de swipe la garde dans l'état de son écran ; chaque décision ou undo renvoie une nouvelle session. La pile disparaît avec la session.
- **Chaque use case de décision renvoie une `SessionDecision`** (média, jour cible, entrée remplacée ou `null`). Ils écrivent tous par la même fonction interne `recordDecision`, qui lit l'entrée existante avant d'écrire la nouvelle.
- **`UndoLastDecision`** restaure l'entrée remplacée, ou supprime l'entrée si le média était à trier, puis renvoie la session sans sa dernière décision et le média à remettre sur la pile (`Result.Undone`). Une session vide donne `Result.NothingToUndo`.
- **Suppression confirmée** : si l'entrée visée est déjà `TRASHED`, l'undo renvoie `Result.AlreadyConfirmed` sans rien changer. La confirmation (epic 7) termine la session : l'écran repart ensuite d'une `ReviewSession` vide.

## Alternatives envisagées
- **Retrouver les décisions de la session par `decidedAt`** (toutes celles postérieures au début de session) : la pile survivrait à la mort du process, mais il faudrait une requête de plus dans le port, et l'entrée remplacée serait perdue.
- **Pile mutable injectée (portée de session Hilt)** : un état partagé caché, plus difficile à tester qu'une valeur passée à chaque appel.
- **Undo qui supprime toujours l'entrée** : faux pour un favori posé sur un média déjà gardé, qui redeviendrait à trier.

## Conséquences
- Après la mort du process, les décisions restent en base mais la pile est perdue : l'undo ne porte que sur la session en cours, ce que dit la SPEC.
- Chaque décision coûte une lecture de plus (`entriesFor` sur un seul média), négligeable avec l'index unique `(mediaId, reviewYear)`.
- `GetSessionSummary` (PS-22) pourra compter les décisions à partir de la `ReviewSession`.
