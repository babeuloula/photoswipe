# Tests

> Document vivant : il décrit les tests **actuels** du dépôt et ce qui est prévu par la SPEC §11. Les éléments pas encore implémentés sont marqués *(prévu)*.

## 1. Principes

- **Chaque ticket est livré avec ses tests.** Rien n'est terminé sans tests verts, en local comme en CI.
- **Sans émulateur** : tous les tests actuels tournent sur la JVM, y compris ceux qui ont besoin d'Android (Robolectric).
- **Noms en anglais, qui décrivent le comportement** : `` `moves leap day media to February 28 on non-leap years` ``. Commentaires et KDoc en français.
- **Un test qui ne peut pas échouer ne sert à rien.** Quand un test s'appuie sur un outil maison (lecteur de ressources, règle Konsist), cet outil est lui-même testé sur des fixtures qui contiennent volontairement des erreurs.
- Le temps (`Clock`) et la concurrence sont injectés, jamais appelés en statique, pour rester testables.

## 2. Commandes

Prérequis : JDK 17 (`export JAVA_HOME=/opt/homebrew/opt/openjdk@17` sur la machine de Jérémy).

```bash
./gradlew test                                   # tous les tests : modules, Konsist, conventions build-logic
./gradlew test quality assembleDebug             # ce que lance la CI
./gradlew :domain:test                           # un seul module JVM
./gradlew :app:testDebugUnitTest                 # tests de :app (variante debug seulement)
./gradlew :app:testDebugUnitTest --tests '*LocalizationTest'          # une classe
./gradlew :app:testDebugUnitTest --tests '*PluralsTest.uses*English'  # une méthode (motif)
./gradlew :app:testDebugUnitTest --rerun         # relance même si Gradle croit le résultat à jour
```

Rapports : `<module>/build/reports/tests/<tâche>/index.html` (HTML lisible) et `<module>/build/test-results/` (XML JUnit, repris par la CI).

## 3. Les tests par module

| Module | Framework | Ce qui est testé |
|---|---|---|
| `:domain` | JUnit Jupiter (JUnit 6) | Mise en place du module (pas de framework Android sur le classpath). Modèles, `SameDayMatcher`, use cases *(prévu, epic 2)* |
| `:data` | JUnit 4 | Mise en place du module. DAO Room en mémoire, migrations, DataStore *(prévu, epic 3)* |
| `:app` | JUnit 4 + Robolectric (API 34) | Démarrage de `MainActivity` (Hilt, Compose, bord à bord), `TimeModule`, thème Material 3, i18n |
| `:architecture-tests` | JUnit Jupiter + Konsist | Sens des dépendances entre modules (ADR [0005](decisions/0005-konsist-architecture-tests.md)) |
| `build-logic` | JUnit Jupiter + `ProjectBuilder` | Chaque plugin de convention applique les bonnes valeurs (SDK, JVM, Hilt, Compose, qualité) |

Les modules Android restent sur JUnit 4 en attendant une décision ADR sur JUnit 5/6 côté Android.

### Robolectric (`:app`)

- Il simule Android sur la JVM. Il est réglé sur l'**API 34** (`app/src/test/resources/robolectric.properties`) : les API 35 et plus demandent Java 21 (ADR [0002](decisions/0002-robolectric-api-level.md)).
- Il tourne **hors ligne** : son jar Android (`android-all-instrumented`) est résolu par Gradle, donc mis en cache, au lieu d'être téléchargé à chaque exécution. Monter la version de Robolectric impose de mettre à jour `robolectric-androidAll` dans `gradle/libs.versions.toml`, sinon les tests échouent avec « Path is not a file ».
- Tests qui ont besoin de Hilt : `@HiltAndroidTest`, `HiltAndroidRule` et `@Config(application = HiltTestApplication::class)`.
- Tests Compose : `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule<MainActivity>()`. Pas de `createComposeRule()` : son activité hôte n'est pas visible des tests Robolectric. Pour lire des valeurs du thème, composer dans `Robolectric.buildActivity(ComponentActivity::class.java)`.
- Langue d'un test : `@Config(qualifiers = "fr")`.

### Localisation (`:app`, package `i18n`)

- `LocalizationTest` : chaque clé existe en anglais et en français, sans clé orpheline, et chaque dossier `values-xx` traduit est déclaré dans `res/xml/locales_config.xml`. Il complète `MissingTranslation` d'Android Lint.
- `PluralsTest` : les formes du pluriel dans chaque langue.
- `TranslatableResourcesTest` : le lecteur de ressources, sur `src/test/resources/fixtures/i18n`.

### Architecture (`:architecture-tests`)

Konsist lit les sources de `:app`, `:data` et `:domain` sans les compiler. Chaque règle est testée sur des fixtures (`src/test/resources/fixtures`, jamais compilées), et chaque test vérifie que le module scanné n'est pas vide. Une nouvelle règle ou un nouveau module s'ajoute ici.

## 4. Fixtures

- Fixtures de fichiers : `src/test/resources/fixtures/` de chaque module qui en a besoin (architecture, i18n).
- Fixtures en mémoire de `:domain` (`testFixtures`) : builders `aMediaItem()`, `aReviewEntry()`, `FakeMediaSource`, `FakeReviewRepository`, `FixedClock` *(prévu, epic 2)*.
- Seeder MediaStore en debug et script `scripts/seed-emulator.sh` pour tester sur émulateur *(prévu, epic 10)*.

## 5. Prévu par la SPEC (§11)

| Type | Portée | Outils |
|---|---|---|
| Unitaires domaine | `SameDayMatcher` (29 février, changement d'année, fenêtre de 7 jours), cycle de revue, use cases, annulation | JUnit, MockK, Turbine, kotlinx-coroutines-test |
| ViewModels | `UiState`, `UiEvent`, `UiEffect` de chaque écran | Turbine, kotlinx-coroutines-test |
| Intégration données | DAO Room en mémoire, migrations ; requêtes MediaStore (tests instrumentés) | AndroidX Test, Room testing |
| UI / fonctionnels | Pile de swipe, boutons alternatifs, résumé, états vides, permissions | Compose UI Test + Hilt test |

Objectifs : couverture du domaine **≥ 90 %**, tous les ViewModels testés, chaque cas limite de la SPEC §14 couvert par au moins un test.

## 6. CI

`.github/workflows/ci.yml` lance `./gradlew test quality assembleDebug --continue` en une seule invocation parallèle, à chaque push sur `main` et sur chaque PR. Le résumé du run liste les tests passés et échoués ; en cas d'échec, les rapports sont publiés en artefact. Un nouveau type de test (par exemple les tests instrumentés) s'ajoute à la CI dans la PR qui l'introduit.
