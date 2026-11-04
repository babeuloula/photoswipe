# 0001 — Conventions Gradle partagées via un build inclus `build-logic`

- **Statut** : accepté
- **Date** : 2026-11-04
- **Ticket** : PS-2

## Contexte
PS-2 demande des conventions Gradle partagées. Sans elles, chaque module répète `compileSdk`, `minSdk`, la version de la JVM, etc. En PS-1, `:app` et `:data` dupliquaient déjà cette configuration. D'autres conventions arrivent avec les tickets suivants : Compose, Hilt, ktlint et detekt.

## Décision
On crée un **build inclus `build-logic/`** (module `convention`) qui expose des plugins Gradle écrits en Kotlin :
- `photoswipe.android.application` ;
- `photoswipe.android.library` ;
- `photoswipe.jvm.library`.

Les valeurs partagées (SDK 33/37, JVM 17) sont centralisées dans `ProjectConfig`. Les modules appliquent le plugin de convention via le version catalog et ne déclarent plus que ce qui leur est propre : namespace, dépendances, `applicationId`.

Points de détail :
- La version Java n'est pas fixée dans `compileOptions` : AGP la déduit du toolchain (`jvmToolchain(17)`), on évite donc deux sources de vérité.
- Les tests des conventions (Gradle `ProjectBuilder`) sont reliés à la tâche racine `test`. `./gradlew test` les lance donc aussi, en local comme en CI.

## Alternatives envisagées
- **`buildSrc`** : plus simple à mettre en place, mais toute modification invalide le cache de tout le build. Gradle et Google (projet *Now in Android*) recommandent un build inclus.
- **Blocs `subprojects {}` / `allprojects {}` dans le build racine** : couplage implicite entre modules, incompatible avec l'isolation de projet, et déconseillé par Gradle.
- **Pas de convention (duplication)** : acceptable avec deux modules Android, mais la duplication grandit avec Compose, Hilt et la qualité.

## Conséquences
- Un nouveau module s'écrit en quelques lignes.
- Pour changer un réglage commun, on modifie `ProjectConfig` ou un plugin de convention, et les tests de `build-logic` vérifient le résultat.
- Coût : un dossier de plus à comprendre (expliqué dans `docs/beginners-guide.md`).
- Les prochaines conventions (Compose, Hilt, qualité) seront ajoutées comme nouveaux plugins dans `build-logic`, chacune dans son ticket.
