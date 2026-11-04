# Tests

> Document vivant : il décrit les tests **actuels** du dépôt et ce qui est prévu par la SPEC §11. Les éléments pas encore implémentés sont marqués *(prévu)*.

## 1. Principes

- **Chaque ticket est livré avec ses tests.** Rien n'est terminé sans tests verts, en local comme en CI.
- **Sur la JVM d'abord** : les tests unitaires tournent sans émulateur, y compris ceux qui ont besoin d'Android (Robolectric). Seuls les vrais chemins de données (MediaStore réel, migrations Room) passent par des tests instrumentés (`src/androidTest`, émulateur ou appareil).
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

# Tests instrumentés : un émulateur (ou un appareil) doit être démarré et visible par `adb devices`
~/Library/Android/sdk/emulator/emulator -avd Medium_Phone_API_37.0 &   # sur la machine de Jérémy
./gradlew connectedDebugAndroidTest              # tous les modules
./gradlew :data:connectedDebugAndroidTest        # un seul module
```

Rapports : `<module>/build/reports/tests/<tâche>/index.html` (HTML lisible) et `<module>/build/test-results/` (XML JUnit, repris par la CI). Tests instrumentés : `<module>/build/reports/androidTests/connected/` et `<module>/build/outputs/androidTest-results/connected/`.

## 3. Les tests par module

| Module | Framework | Ce qui est testé |
|---|---|---|
| `:domain` | JUnit Jupiter (JUnit 6) | Mise en place du module (pas de framework Android sur le classpath), invariants des modèles, `SameDayMatcher` (même jour, fuseau, 29 février), `ReviewWindowProvider` (milieu d'année, changement d'année, 29 février, fuseau de l'horloge, avec `Clock.fixed`), plages de dates de `SameDayMatcher` (changement d'heure, 29 février, accord avec `matches` heure par heure sur deux ans), fixtures partagées (builders, `FixedClock`, contrat des ports respecté par `FakeMediaSource` et `FakeReviewRepository`, avec `runTest`), use cases (`GetReviewDays` : jours vides, partiellement triés, année suivante, fenêtre à cheval sur deux années, réaction aux décisions ; `GetDeckForDay` : ordre multi-années, médias déjà décidés exclus, 29 février, stratégie injectée, taille du paquet égale au compteur de `GetReviewDays` ; `KeepMedia` : entrée enregistrée, année du jour cible, remplacement d'une décision, cycle annuel sur deux années ; `MarkForDeletion` : entrée et taille enregistrées, média en attente sans passage à la corbeille, remplacement d'une décision, retrait du paquet ; `UndoLastDecision` : session vide, undo simple, undo multiple dans l'ordre inverse, restauration de la décision remplacée, suppression confirmée refusée ; `FavoriteMedia` : gardé avec favori en attente, favori sur un média déjà gardé, favori puis undo ; `GetSessionSummary` : session vide, compteurs, somme des tailles, demandes en attente d'une session précédente, décision annulée ou remplacée, suppression confirmée, média en attente pour deux années compté une fois ; `PrepareTrash` et `RecordTrashResult` : aucun marqué, lots multiples, média marqué pour deux années demandé une fois, média illisible écarté, confirmation, refus, confirmation partielle, accord avec le résumé ; `CleanOrphanEntries` : aucun orphelin, plusieurs orphelins, média dans la corbeille, idempotence, aucune purge en accès partiel ou sans accès), `FakeMediaActionRequestFactory` (découpage en lots), `ReviewSession` (pile, invariants de `SessionDecision`), `RecentYearsFirstOrdering` (année décroissante, heure croissante, fuseau, départage par identifiant). Les flux se testent sans bibliothèque de plus : on collecte dans une liste depuis `backgroundScope` avec `UnconfinedTestDispatcher(testScheduler)`, puis on vérifie les émissions |
| `:data` | JUnit 4 + Robolectric (API 34) | Mise en place du module, accès aux fixtures de `:domain`, DAO Room sur une base en mémoire (insertion, unicité du couple média / année, lecture par année, suppression, flux observé, marquages, corbeille, purge), `RoomReviewRepository` sur la même base (écriture, remplacement, suppression, flux après écriture, listes plus longues qu'une requête) et ses mappers, `DataStoreSettingsRepository` sur un vrai DataStore dans un dossier temporaire (valeurs par défaut, écriture puis relecture, persistance dans le fichier, flux, heure invalide), `MediaStoreMediaSource` face à `FakeMediaStoreProvider`, un faux MediaStore adossé à une vraie table SQLite en mémoire (plages ouvertes à droite, plages du domaine, mapping des photos et des vidéos avec durée, aucune fonction SQL de date, repli sur `DATE_ADDED` dans la sélection et au mapping, corbeille exclue, identifiants nombreux) et la construction des sélections. En instrumenté : MediaStore réel (photos datées par EXIF, plages du domaine, mapping) et migration Room depuis le schéma exporté |
| `:app` | JUnit 4 + Robolectric (API 34) | Démarrage de `MainActivity` (Hilt, Compose, bord à bord), `TimeModule`, thème Material 3, i18n, accès aux fixtures de `:domain` |
| `:architecture-tests` | JUnit Jupiter + Konsist | Sens des dépendances entre modules (ADR [0005](decisions/0005-konsist-architecture-tests.md)), ports du domaine limités aux interfaces (ADR [0007](decisions/0007-domain-ports.md)) |
| `build-logic` | JUnit Jupiter + `ProjectBuilder` | Chaque plugin de convention applique les bonnes valeurs (SDK, JVM, Hilt, Compose, qualité) |

Les modules Android restent sur JUnit 4 en attendant une décision ADR sur JUnit 5/6 côté Android.

### Robolectric (`:app`, `:data`)

- Il simule Android sur la JVM. Il est réglé sur l'**API 34** (`src/test/resources/robolectric.properties` de chaque module) : les API 35 et plus demandent Java 21 (ADR [0002](decisions/0002-robolectric-api-level.md)).
- La convention `photoswipe.android.robolectric` l'ajoute à un module et le configure.
- Il tourne **hors ligne** : son jar Android (`android-all-instrumented`) est résolu par Gradle, donc mis en cache, au lieu d'être téléchargé à chaque exécution. Monter la version de Robolectric impose de mettre à jour `robolectric-androidAll` dans `gradle/libs.versions.toml`, sinon les tests échouent avec « Path is not a file ».
- Base Room : `Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), PhotoSwipeDatabase::class.java)`, fermée dans `@After`.
- Tests qui ont besoin de Hilt : `@HiltAndroidTest`, `HiltAndroidRule` et `@Config(application = HiltTestApplication::class)`.
- Tests Compose : `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule<MainActivity>()`. Pas de `createComposeRule()` : son activité hôte n'est pas visible des tests Robolectric. Pour lire des valeurs du thème, composer dans `Robolectric.buildActivity(ComponentActivity::class.java)`.
- Langue d'un test : `@Config(qualifiers = "fr")`.

### Tests instrumentés (`:data`, `src/androidTest`)

- JUnit 4 avec `AndroidJUnit4` et `AndroidJUnitRunner` (réglé par la convention Android commune). Les noms entre backticks avec espaces fonctionnent comme ailleurs : le format DEX les accepte depuis l'API 30, et le `minSdk` est 33.
- `MediaStoreMediaSourceInstrumentedTest` : le test insère ses propres photos JPEG, datées par leur EXIF (`DateTimeOriginal` et décalage horaire) comme de vraies photos, puis les supprime. MediaStore recalcule `DATE_TAKEN` depuis le fichier : un `DATE_TAKEN` posé à la main serait écrasé. L'application lit sans permission les médias qu'elle a ajoutés, ceux de l'appareil restent invisibles.
- `PhotoSwipeDatabaseMigrationTest` : `MigrationTestHelper` crée la base depuis le schéma exporté (`data/schemas/`, ajouté aux assets des tests par le plugin Room) et vérifie qu'il correspond à l'entité. Chaque nouvelle version de la base y ajoute sa migration.

### Localisation (`:app`, package `i18n`)

- `LocalizationTest` : chaque clé existe en anglais et en français, sans clé orpheline, et chaque dossier `values-xx` traduit est déclaré dans `res/xml/locales_config.xml`. Il complète `MissingTranslation` d'Android Lint.
- `PluralsTest` : les formes du pluriel dans chaque langue.
- `TranslatableResourcesTest` : le lecteur de ressources, sur `src/test/resources/fixtures/i18n`.

### Architecture (`:architecture-tests`)

Konsist lit les sources de `:app`, `:data` et `:domain` sans les compiler. Chaque règle est testée sur des fixtures (`src/test/resources/fixtures`, jamais compilées), et chaque test vérifie que le module scanné n'est pas vide. Une nouvelle règle ou un nouveau module s'ajoute ici.

## 4. Fixtures

- Fixtures de fichiers : `src/test/resources/fixtures/` de chaque module qui en a besoin (architecture, i18n).
- Fixtures en mémoire de `:domain` : `domain/src/testFixtures/kotlin`, package `fr.babeuloula.photoswipe.domain.fixtures` (plugin Gradle `java-test-fixtures`). Un module les utilise avec `testImplementation(testFixtures(project(":domain")))` ; `:data` et `:app` le font déjà.
  - **Builders** `aMediaItem()` et `aReviewEntry()` : un objet valide aux valeurs par défaut fixes (média 1, photo, prise le 15 juillet 2020 ; entrée gardée en 2026). Le test ne précise que ce qui compte pour lui. Les identifiants sont explicites, sans compteur global.
  - **`FakeMediaSource`** : galerie en mémoire (`add`, `remove` pour simuler une suppression, une mise à la corbeille ou un accès partiel). Plages ouvertes à droite, chaque média renvoyé une seule fois.
  - **`FakeReviewRepository`** : une entrée par couple (média, année de revue), comme l'index unique Room ; `entries` expose ce qui a été enregistré.
  - **`FakeMediaActionRequestFactory`** : découpe les médias en lots de `batchSize` (500 par défaut, plus petit dans un test pour avoir plusieurs lots) ; chaque demande factice expose sa nature (corbeille ou favori) et ses médias.
  - **`FixedClock`** : horloge figée que le test déplace (`advanceBy`, `setTo`), par exemple pour franchir minuit ; `FixedClock.at(LocalDateTime)` la crée dans le fuseau de Paris par défaut.
  - Les fakes sont eux-mêmes testés (`domain/src/test/.../fixtures`) : ils doivent respecter le contrat des ports, sinon les tests qui s'appuient dessus mentiraient.
  - Les fonctions `suspend` s'appellent dans `runTest { }` (kotlinx-coroutines-test).
  - Qualité : ktlint et detekt analysent aussi `src/testFixtures` ; `MagicNumber` et `LongParameterList` y sont désactivées comme dans les tests.
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

`.github/workflows/ci.yml` a deux jobs, à chaque push sur `main` et sur chaque PR :

- **Quality, unit tests and debug build** (check obligatoire du ruleset, ADR 0004) : `./gradlew test quality assembleDebug --continue` en une seule invocation parallèle ;
- **Instrumented tests (emulator)** : `./gradlew connectedDebugAndroidTest` sur un émulateur API 34 (`reactivecircus/android-emulator-runner`, accélération KVM).

 Le résumé du run liste les tests passés et échoués ; en cas d'échec, les rapports sont publiés en artefact. Un nouveau type de test s'ajoute à la CI dans la PR qui l'introduit.
