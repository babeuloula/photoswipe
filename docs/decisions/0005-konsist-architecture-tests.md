# 0005 — Tests d'architecture avec Konsist, dans un module dédié

- **Statut** : accepté
- **Date** : 2026-11-04
- **Ticket** : PS-7

## Contexte
La SPEC (§5.1, §11) demande un test d'architecture, « Konsist ou ArchUnit », qui vérifie que `:domain` ne dépend d'aucune bibliothèque Android et que `:app` ne dépend de `:data` que pour le câblage DI. Restent à choisir l'outil, l'endroit où vivent ces tests et la forme exacte des règles.

## Décision
- **Konsist 0.18.1** (dernière stable ; la 1.0 n'est qu'en bêta). Il lit les sources Kotlin (fichiers, packages, imports) sans les compiler, et la 0.18.1 est compilée avec Kotlin 2.4.21, la version du projet.
- Un **module JVM dédié `:architecture-tests`** (convention `photoswipe.jvm.library`), qui ne dépend d'aucun module du projet et ne contient que des tests. Ses sources scannées (`app/src`, `data/src`, `domain/src`) sont déclarées en entrées de sa tâche `test` : sinon Gradle, qui ne voit pas que Konsist les lit, jugerait le test à jour après une modification de `:app` ou `:domain`.
- Règles, exprimées sur les imports :
  - `:domain`, production et tests : aucun import de `android`, `androidx` (Room, Compose et tout Jetpack), `com.google.android`, ni d'un package du projet hors `fr.babeuloula.photoswipe.domain` (donc ni `:data`, ni `:app`). `javax.inject` reste permis (JSR-330, pur Java).
  - `:app`, production seulement : les imports de `fr.babeuloula.photoswipe.data` ne sont permis que dans le package `di` et ses sous-packages. Les tests de `:app` peuvent légitimement remplacer une implémentation de `:data`.
- Chaque règle est testée sur des **fixtures** (`src/test/resources/fixtures`, fichiers Kotlin jamais compilés) qui la violent et la respectent. Chaque test sur le vrai code vérifie aussi que le module scanné n'est pas vide, pour qu'un renommage ne rende pas la règle toujours vraie.

## Alternatives envisagées
- **ArchUnit** : il analyse le bytecode, donc il ne voit que ce qui a été compilé, et demande de compiler `:app` et `:data` (modules Android) pour les tests d'un autre module. Il ignore aussi les notions propres à Kotlin. Konsist est fait pour Kotlin et plus simple à brancher.
- **Tests Konsist dans `:app`** : `:app` dépend déjà de tout, mais ses tests Robolectric sont lents et mélanger règles globales et tests de l'application brouille les responsabilités. Konsist recommande lui-même un module dédié.
- **Vérifier les dépendances Gradle** (par exemple, interdire `project(":data")` dans `:app`) : impossible, car Hilt a besoin de cette dépendance. La frontière à tenir est dans le code (quels packages importent `:data`), pas dans Gradle.

## Conséquences
- `./gradlew test`, donc la CI, échoue dès qu'un import interdit apparaît, avec l'emplacement exact (`fichier:ligne:colonne`).
- Les règles portent sur les imports : un nom pleinement qualifié écrit dans le code sans import (`fr.babeuloula.photoswipe.data.Foo()`) leur échappe. Cas rare, à repérer en revue.
- Un nouveau module, ou une nouvelle frontière (par exemple les ViewModels qui ne connaissent que des use cases, SPEC §5.4), s'ajoute dans ce module, avec ses fixtures, et ses sources dans les entrées de la tâche `test`.
