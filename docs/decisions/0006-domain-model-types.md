# 0006 — Types des modèles du domaine

- **Statut** : accepté
- **Date** : 2026-11-04
- **Ticket** : PS-10

## Contexte
La SPEC (§5.2) liste les modèles du domaine (`MediaId`, `MediaItem`, `MediaType`, `ReviewDay`, `ReviewDecision`, `SessionSummary`) sans fixer leurs types ni, pour `ReviewDay` et `SessionSummary`, leurs champs. `:domain` doit rester du Kotlin JVM pur et n'ajouter aucune bibliothèque sans justification.

## Décision
- **Temps** : `java.time` du JDK. La date de prise de vue est un `Instant` (sans fuseau), la durée d'une vidéo un `java.time.Duration` nullable, un jour cible une `LocalDate`. La conversion d'un instant en jour local se fera au moment du calcul, avec le fuseau de l'appareil (SPEC §3.1), injecté par une `Clock`.
- **`MediaId`** : classe de valeur (`@JvmInline value class`) autour du `Long` MediaStore.
- **`ReviewDay`** : `date` (jour cible) et `remainingCount` (médias à trier, pour PS-16). L'année de revue (SPEC §3.3) est dérivée : `reviewYear = date.year`.
- **`SessionSummary`** : `keptCount`, `markedForDeletionCount`, `favoriteCount` et `estimatedFreedBytes` (SPEC §3.5).
- **Invariants** vérifiés à la construction (`require`) : tailles, durées et compteurs non négatifs, pas de durée pour une photo. Une vidéo peut avoir une durée inconnue (`null`), car MediaStore ne la fournit pas toujours.
- Le favori n'est pas une valeur de `ReviewDecision` : c'est une demande en attente, stockée à part (`favorite_pending`, SPEC §6).

## Alternatives envisagées
- **Epoch en `Long` (millisecondes)** : c'est le format de MediaStore et de Room, mais on peut le confondre avec n'importe quel autre `Long` et il ne dit rien de son unité. La conversion reste dans `:data`.
- **kotlinx-datetime** : bibliothèque en plus, sans gain ici puisque `java.time` est disponible sur la JVM comme sur Android (depuis l'API 26, et le `minSdk` du projet est 33).
- **`kotlin.time.Duration`** : rien à lui reprocher, mais mélanger `kotlin.time` et `java.time` dans les mêmes modèles serait incohérent.

## Conséquences
- Les modèles n'acceptent pas d'état incohérent : une erreur de mapping dans `:data` lève une `IllegalArgumentException` au lieu de se propager.
- `:data` convertit les millisecondes MediaStore et Room en `Instant` et `Duration`.
