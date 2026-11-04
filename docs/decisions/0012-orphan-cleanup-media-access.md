# 0012 — Nettoyage des orphelins selon l'accès à la galerie

- **Statut** : accepté
- **Date** : 2026-11-04
- **Ticket** : PS-24

## Contexte
La SPEC §6 demande de supprimer, au démarrage et après chaque confirmation, les entrées des médias qui n'existent plus. `MediaSource.mediaByIds` ne renvoie que les médias présents **et lisibles** : en accès partiel (Android 14+), un média hors de la sélection est absent sans avoir disparu, et sans accès la galerie paraît vide. L'ADR 0007 prévoyait que la purge ne tourne pas dans ces cas. Le domaine ne connaît pas les permissions, qui sont une notion Android.

## Décision
- **Enum `MediaAccess` (`FULL`, `PARTIAL`, `NONE`) dans `model`**. `:app` le déduit des permissions accordées et le passe à `CleanOrphanEntries`.
- **`CleanOrphanEntries(access)` ne purge qu'avec `FULL`** : il compare `knownMediaIds` aux médias renvoyés par `mediaByIds`, supprime toutes les entrées (toutes années) des absents et renvoie leur nombre. Avec `PARTIAL` ou `NONE`, il ne fait rien et renvoie 0.

## Alternatives envisagées
- **Booléen `fullAccess`** : moins lisible à l'appel, et l'accueil (epic 4) aura besoin de distinguer les trois états pour sa bannière.
- **Port `MediaAccessProvider`** lu par le use case : un port de plus pour une valeur que l'appelant connaît déjà au moment d'appeler.
- **Restreindre la purge à la sélection en accès partiel** : impossible de distinguer un média supprimé d'un média désélectionné avec l'API MediaStore.

## Conséquences
- En accès partiel, les entrées des médias supprimés restent en base jusqu'au premier démarrage avec un accès complet. Elles ne gênent pas : elles ne concernent aucun média proposé.
- `mediaByIds` peut recevoir des milliers d'identifiants : `MediaStoreMediaSource` (epic 3) découpe la requête pour rester sous la limite de paramètres SQLite.
