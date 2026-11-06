# 0021 — KSP hors du cache de build et non incrémental

- **Statut** : accepté
- **Date** : 2026-11-06
- **Ticket** : correctif hors ticket (PR #71), documenté avec PS-71

## Contexte
Après le merge de PS-68, `main` est devenu rouge sans changement de code en cause : `:app:transformReleaseClassesWithAsm` échouait sur `ReminderRescheduleReceiver`. Le log montrait `:app:kspReleaseKotlin FROM-CACHE`, puis `compileReleaseJavaWithJavac` et `hiltJavaCompileRelease` en `NO-SOURCE` : l'entrée de cache KSP revenait sans les sources Java générées par Hilt (`Hilt_*`), et la transformation Hilt ne trouvait plus leurs classes. `MediaSourceBindingTest` notait déjà un symptôme proche (composant Hilt de la release perdu au cache). En local, KSP incrémental perdait de même des fichiers générés après un changement de branche (module Hilt de `src/debug` absent, `MissingBinding`).

## Décision
- `applyKsp()` (`build-logic`, `Ksp.kt`), utilisé par les conventions Hilt et Room, applique KSP et sort ses tâches `ksp…Kotlin` du cache de build (`outputs.doNotCacheIf`, API publique de Gradle).
- `gradle.properties` : `ksp.incremental=false`.
- Vérification : dans le log de la CI, `:app:kspReleaseKotlin` s'exécute et `compileReleaseJavaWithJavac` compile.

## Alternatives envisagées
- **Vider le cache GitHub Actions** : débloque une fois, le problème revient.
- **Garder le cache et l'incrémental en espérant une version corrigée de KSP** : le check obligatoire de `main` bloque tous les merges en attendant.
- Coût accepté : KSP refait tout son traitement à chaque compilation qui en a besoin (quelques secondes par module).
