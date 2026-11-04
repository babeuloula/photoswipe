# 0002 — Robolectric sur l'API 34 pour les tests JVM de `:app`

- **Statut** : accepté
- **Date** : 2026-11-04
- **Ticket** : PS-3

## Contexte
PS-3 demande de vérifier qu'un module Hilt « compile et s'injecte ». Une injection réelle a besoin du framework Android. On veut la tester sur la JVM, sans émulateur, pour que `./gradlew test` la couvre en local comme en CI. Robolectric le permet. Mais Robolectric 4.17 exige Java 21 pour simuler les API 35 et plus, alors que le projet est sur JDK 17 (`ProjectConfig.JVM_VERSION`, minimum d'AGP 9.5).

## Décision
Les tests Robolectric tournent sur l'**API 34** (Android 14), fixée dans `app/src/test/resources/robolectric.properties`. Cette API est comprise entre `minSdk` (33) et `targetSdk` (37).

## Alternatives envisagées
- **Lancer les tests avec un JDK 21** (toolchain dédiée aux tâches de test) : on pourrait tester sur l'API 37, mais il faudrait un deuxième JDK sur chaque machine et en CI. C'est une complexité en plus pour un gain faible tant que les tests ne dépendent pas d'un comportement propre à l'API 35+.
- **Tests instrumentés sur émulateur** : fidèles, mais lents et absents de la CI pour l'instant (PS-6).

## Conséquences
- Les tests Hilt et, plus tard, les tests Compose sur JVM ne voient pas les comportements spécifiques aux API 35 à 37. Les tests instrumentés couvriront ces cas.
- Si un test doit viser une API récente, on rouvre cette décision : passage du projet à JDK 21, ou toolchain de test en 21.
