# 0011 — Portée du résumé avant confirmation

- **Statut** : accepté
- **Date** : 2026-11-04
- **Ticket** : PS-22

## Contexte
Le résumé de fin de session affiche les gardés, les marqués, les favoris et l'espace libéré estimé (SPEC §3.5). Or les marquages et les favoris non confirmés survivent à la session : un refus les laisse en base (§3.5) et l'accueil les signale (« N médias en attente de suppression », §3.4 et §4). La confirmation suivante les enverra donc aussi à la corbeille ou en favori. La SPEC ne dit pas si le résumé compte la session seule ou toutes les demandes en attente.

## Décision
- **Marqués, favoris et espace libéré : toutes les demandes en attente** (`entriesMarkedForDeletion`, `entriesWithPendingFavorite`), pour que le résumé annonce exactement ce que les dialogues système vont demander. L'espace libéré additionne les tailles mémorisées au moment de chaque décision.
- **Gardés : la session seule**, d'après les décisions de la `ReviewSession` dont l'entrée actuelle est `KEPT` sans favori. Un undo ou une décision remplacée ne sont donc pas comptés, et les trois nombres ne se recoupent pas.
- **`GetSessionSummary` est `suspend`** : l'écran de résumé lit une photo instantanée, comme le paquet (ADR 0009).

## Alternatives envisagées
- **Tout compter sur la session** : le résumé annoncerait 5 suppressions alors qu'Android en demanderait 12 si une session précédente a été refusée.
- **Gardés en base, toutes sessions** : nombre sans intérêt (tous les médias gardés de l'année), et sans lien avec ce qui va être confirmé.
- **Gardés incluant les favoris** : avec des favoris d'une session précédente, le nombre de gardés pourrait être plus petit que celui des favoris.

## Conséquences
- `PrepareTrash` (PS-23) lit le même ensemble que le résumé ; un test vérifiera que ses lots contiennent `markedForDeletionCount` médias.
- Le résumé peut s'afficher avec une session vide, depuis la bannière de l'accueil, pour confirmer des marquages restés en attente.
