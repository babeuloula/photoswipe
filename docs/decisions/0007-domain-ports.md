# 0007 — Ports du domaine

- **Statut** : accepté
- **Date** : 2026-11-04
- **Ticket** : PS-14

## Contexte
La SPEC (§5.2) nomme cinq ports (`MediaSource`, `ReviewRepository`, `SettingsRepository`, `MediaActionRequestFactory`, `ReminderScheduler`) et dit à quoi chacun sert. Elle ne fixe ni leurs signatures, ni leur package, ni leur style d'appel (synchrone, `suspend`, `Flow`). `:domain` reste du Kotlin JVM pur et n'ajoute aucune bibliothèque sans justification.

## Décision
- **Package** : `fr.babeuloula.photoswipe.domain.port`. Il ne contient **que des interfaces** ; une règle Konsist le vérifie (`domain ports declare only interfaces`). Les types de données qu'ils échangent vont dans `model`.
- **Appels `suspend`, sans `Flow`** : Room et DataStore font des entrées-sorties, donc les lectures et écritures sont des fonctions `suspend`. `suspend` est un mot-clé du langage et n'ajoute aucune dépendance au domaine. `Flow` demanderait kotlinx-coroutines-core dans `:domain` alors qu'aucun besoin réactif n'est encore écrit ; il sera ajouté, avec sa justification, par le premier ticket qui en a besoin. `MediaActionRequestFactory` et `ReminderScheduler` restent synchrones : `createTrashRequest` et la mise en file WorkManager ne bloquent pas.
- **Plages de dates** : `MediaSource.mediaTakenIn` reçoit des `OpenEndRange<Instant>` (`début..<fin`), type de la bibliothèque standard Kotlin, stable. Le domaine calcule les plages (un jour par année, dans le fuseau de l'appareil), `:data` les traduit en millisecondes epoch sans fonction SQL de date (SPEC §5.3).
- **Orphelins** : `MediaSource.mediaByIds` renvoie les médias encore présents et accessibles ; `ReviewRepository.knownMediaIds` et `deleteAllFor` permettent de purger les autres (SPEC §6). La même lecture par identifiants fournit les `MediaItem` (et donc leur URI) des demandes de corbeille et de favori.
- **Nouveaux modèles** :
  - `ReviewEntry` : une ligne de décision (SPEC §6), avec `favoritePending`. Un favori est toujours gardé (SPEC §2) : `favoritePending` n'est accepté qu'avec `KEPT`.
  - `ReminderSettings` : rappel activé et heure. Par défaut **désactivé, à 18:00** : la permission `POST_NOTIFICATIONS` n'est demandée qu'au moment du réglage (SPEC §3.6), l'activer d'office n'aurait donc aucun effet.
- **Demande de confirmation opaque** : `MediaActionRequest` est une interface du domaine qui n'expose que les identifiants des médias couverts (pour enregistrer le résultat de chaque demande). `:data` l'implémente autour de l'`IntentSender` ; `:app` le récupère pour lancer le dialogue.
- **Lots** : le découpage en lots (environ 500 URI par demande) est une contrainte Android ; il reste dans `:data`, et la fabrique renvoie une liste de demandes.

## Alternatives envisagées
- **`Flow` dès maintenant** (décisions et réglages observables) : pratique pour l'accueil, mais c'est une bibliothèque en plus sans besoin écrit dans un ticket. Reporté.
- **Type maison pour les plages** (`InstantRange`) : rien de plus que `OpenEndRange<Instant>`, qui existe déjà.
- **Identifiants seuls pour la fabrique de demandes** : `:data` devrait relire MediaStore pour retrouver l'URI et la collection (image ou vidéo) de chaque média. Passer les `MediaItem` évite cette lecture cachée.
- **Ports dans le package racine du domaine** : on ne pourrait plus vérifier simplement qu'ils ne contiennent que des interfaces.

## Conséquences
- Les fakes de PS-15 implémentent ces interfaces en mémoire.
- Avec l'accès partiel d'Android 14+, `mediaByIds` ne renvoie que les médias sélectionnés : la purge des orphelins ne doit pas tourner (ou doit se restreindre) tant que l'accès est partiel, sous peine d'effacer de vraies décisions.
- L'écran a besoin de l'`IntentSender` caché derrière `MediaActionRequest`, mais `:app` n'importe `:data` que depuis `di` : le déballage passera par un adaptateur fourni par le package `di` (ticket de confirmation, epic 7).
- Ajouter un `Flow` à un port imposera d'ajouter kotlinx-coroutines-core à `:domain` (justification dans `docs/architecture.md`).
- Le rappel désactivé par défaut est un choix à valider avec Jérémy (complète le point 5 de la SPEC §16).
