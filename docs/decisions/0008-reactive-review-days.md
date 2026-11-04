# 0008 — Flux réactif des jours de revue

- **Statut** : accepté
- **Date** : 2026-11-04
- **Ticket** : PS-16

## Contexte
L'accueil affiche, pour chaque jour de la fenêtre, le nombre de médias encore à trier (SPEC §4). PS-16 demande un use case `GetReviewDays` qui expose ces nombres sous forme de **flux réactif** (`Flow`). L'ADR 0007 avait écarté `Flow` des ports « tant qu'aucun ticket n'en a besoin » : PS-16 est ce ticket.

Pour compter, il faut aussi lire les médias de chaque jour. `MediaSource.mediaTakenIn` reçoit des plages de dates que le domaine calcule (SPEC §5.3), sans que la SPEC fixe la première année à chercher.

## Décision
- **kotlinx-coroutines-core dans `:domain`**, en `api` : `Flow` apparaît dans les signatures publiques des ports et des use cases, que `:data` et `:app` utilisent. C'est une bibliothèque Kotlin pure, sans Android ; la règle Konsist du domaine (liste d'interdits) l'accepte déjà.
- **`ReviewRepository.observeEntries(reviewYears)`** : un seul flux, observé **par année de revue**, qui émet les entrées dès la collecte puis après chaque écriture. On filtre ensuite par média dans le domaine. Une observation par liste de médias mettrait des milliers d'identifiants dans un `IN (…)` SQLite, au risque de dépasser la limite de paramètres ; la fenêtre, elle, ne couvre au plus que deux années (fin décembre et début janvier). Le flux peut réémettre une liste identique (Room réémet à chaque écriture dans la table) : `GetReviewDays` applique `distinctUntilChanged`.
- **`MediaSource` reste en `suspend`** : la galerie est lue une fois à la collecte. Ni l'ajout d'un média dans la galerie, ni le passage à minuit ne relancent le calcul : l'accueil recollecte le flux en revenant au premier plan. Observer MediaStore (`ContentObserver`) sera ajouté si un ticket en a besoin.
- **Plages de dates calculées par `SameDayMatcher.rangesFor`**, à côté de `matches`, pour que la règle du §3.1 n'ait qu'un propriétaire. Une plage par année, de minuit à minuit dans le fuseau de l'appareil (`atStartOfDay`, qui suit le changement d'heure : un jour peut durer 23 ou 25 heures). Pour un 28 février d'année non bissextile, la plage d'une année bissextile s'étend au 29 février. Un test vérifie, heure par heure sur deux ans, que les plages couvrent exactement les médias que `matches` retient.
- **Première année cherchée : 1970** (`SameDayMatcher.EARLIEST_YEAR`), début de l'heure epoch de MediaStore. Cela fait environ 56 plages par jour. `GetReviewDays` fait un appel `mediaTakenIn` par jour : environ 112 paramètres par requête, loin de toute limite SQLite.
- **Médias « traités »** : toute entrée de l'année de revue du jour, quelle que soit la décision (`KEPT`, `MARKED_FOR_DELETION`, `TRASHED`), retire le média du compte.
- **Package `usecase`** : `fr.babeuloula.photoswipe.domain.usecase`, un use case par classe avec `operator fun invoke` (SPEC §5.2). Pas de `flowOn` : le choix du dispatcher reviendra à l'appelant, ou à un `CoroutineDispatcher` injecté si un use case en a besoin (SPEC §5.2).

## Alternatives envisagées
- **Use case `suspend` qui renvoie une liste** : plus simple, mais le ticket demande un flux, et l'accueil devrait recharger à la main après chaque décision.
- **Observer par liste de médias** (`observeEntriesFor(year, mediaIds)`) : limite de paramètres SQLite et un flux par jour à combiner.
- **Signal d'invalidation (`Flow<Unit>`)** : oblige à relire les entrées à chaque signal ; Room fournit directement une requête observable.
- **Première année plus récente (2000, appareils photo numériques)** : moins de plages, mais une photo scannée et datée par son EXIF serait perdue sans raison valable. Avant 1970, une date de prise de vue serait négative en epoch, cas trop rare pour compliquer la requête.

## Conséquences
- `RoomReviewRepository` (epic 3) implémente `observeEntries` avec une requête `@Query` qui renvoie un `Flow`.
- Les flux se testent pour l'instant sans Turbine (prévu par la SPEC §11) : collecter dans une liste depuis `backgroundScope` suffit à ces tests. Turbine sera ajouté, avec sa justification, quand un test en aura besoin (ViewModels, epic 4).
- `FakeReviewRepository` garde ses entrées dans un `MutableStateFlow` pour émettre après chaque écriture.
- Les médias datés d'avant 1970 ne sont jamais proposés : limite connue.
- Un média marqué pour suppression une année et jamais confirmé n'a pas d'entrée l'année suivante : il y est de nouveau compté à trier, conformément au §3.3 (une décision vaut pour un couple média, année de revue). Le bandeau « N médias en attente de suppression » le signale par ailleurs.
