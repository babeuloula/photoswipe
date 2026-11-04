# Architecture

> Document vivant : il décrit l'état **actuel** du dépôt et ce qui est prévu. Les éléments pas encore implémentés sont marqués *(prévu)*.

## 1. Vue d'ensemble

PhotoSwipe suit une architecture en couches de type **Clean Architecture**. Le cœur métier (`:domain`) ne connaît aucune technologie. Les modules externes (`:data` pour l'accès aux données, `:app` pour l'interface) dépendent de lui, jamais l'inverse. C'est le principe d'**inversion de dépendances** : le domaine définit des interfaces (les « ports ») et `:data` les implémente.

```
                ┌──────────────────────────┐
                │          :app            │  UI Compose, navigation, ViewModels,
                │  (application Android)   │  assemblage Hilt, notifications
                └──────┬────────────┬──────┘
                       │            │ (uniquement pour le câblage DI)
                       ▼            ▼
┌──────────────────────────┐   ┌──────────────────────────┐
│         :domain          │◄──│          :data           │  MediaStore, Room,
│   Kotlin JVM pur :       │   │  (bibliothèque Android)  │  DataStore, WorkManager
│   modèles, use cases,    │   └──────────────────────────┘
│   ports (interfaces)     │
└──────────────────────────┘
```

Règles :
- `:domain` ne dépend d'**aucun** autre module ni d'aucune bibliothèque Android.
- `:data` dépend de `:domain` pour implémenter ses ports.
- `:app` dépend de `:domain` (use cases, modèles). Il ne dépend de `:data` que pour que Hilt puisse brancher les implémentations : le code de l'UI n'importe jamais de classe de `:data` en dehors du package `di`.
- Ces règles sont vérifiées par les tests Konsist du module `:architecture-tests` (ADR [0005](decisions/0005-konsist-architecture-tests.md)) :
  - `:domain` (production et tests) n'importe rien de `android.*`, `androidx.*` (dont Room et Compose) ni `com.google.android.*`, et, du projet, que ses propres packages `fr.babeuloula.photoswipe.domain` ;
  - le code de production de `:app` n'importe `fr.babeuloula.photoswipe.data` que depuis le package `di` ;
  - le package `fr.babeuloula.photoswipe.domain.port` (production) ne déclare que des interfaces, que `:data` implémente (ADR [0007](decisions/0007-domain-ports.md)).
- Chaque règle est elle-même testée sur des fichiers d'exemple (`architecture-tests/src/test/resources/fixtures`, jamais compilés), et chaque test vérifie que le module scanné n'est pas vide. Un test de `:domain` vérifie en plus que le framework Android n'est pas sur son classpath.

## 2. Modules

| Module | Plugin de convention | Package | Rôle |
|---|---|---|---|
| `:app` | `photoswipe.android.application`, `photoswipe.android.hilt`, `photoswipe.android.compose`, `photoswipe.android.robolectric` | `fr.babeuloula.photoswipe` | Point d'entrée (`PhotoSwipeApplication`, `MainActivity`), assemblage Hilt (package `di`), UI Compose et thème Material 3 (package `ui`), écrans et ViewModels *(prévu)* |
| `:data` | `photoswipe.android.library`, `photoswipe.android.hilt`, `photoswipe.android.room`, `photoswipe.android.robolectric` | `fr.babeuloula.photoswipe.data` | Base Room du package `database` : `PhotoSwipeDatabase` (version 1, schéma exporté dans `data/schemas/`), entité `ReviewEntryEntity` (table `review_entry`, clé primaire `(media_id, review_year)`) et `ReviewEntryDao` (ADR [0013](decisions/0013-room-database.md)). Implémentations Android des ports : `RoomReviewRepository` (package `review`, port `ReviewRepository`, mappers entité ↔ domaine, listes d'identifiants découpées par lots de 500)  ; `DataStoreSettingsRepository` (package `settings`, port `SettingsRepository` sur DataStore Preferences : rappel désactivé à 18:00 par défaut, heure stockée en minutes depuis minuit)  ; `MediaStoreMediaSource` (package `media`, port `MediaSource` : photos et vidéos, chaque collection interrogée à part pour garder ses URI ; plages `[début, fin[` comparées à `DATE_TAKEN` en millisecondes, ou à `DATE_ADDED` en secondes si la date de prise de vue manque, sans fonction SQL de date ; corbeille système exclue (`MATCH_EXCLUDE`) ; identifiants par lots de 500 ; sur un `CoroutineDispatcher` injecté) |
| `:domain` | `photoswipe.jvm.library` | `fr.babeuloula.photoswipe.domain` | Modèles du package `model` (`MediaItem`, `ReviewDecision`, `ReviewDay`, `SessionSummary`, `ReviewSession`, `SessionDecision`, `MediaAccess`…, ADR [0006](decisions/0006-domain-model-types.md)). Services purs du package `service` (`SameDayMatcher`, SPEC §3.1, y compris le 29 février, et ses plages de dates pour `MediaSource` ; `ReviewWindowProvider`, fenêtre de 7 jours lue sur la `Clock` injectée ; `DeckOrdering`, stratégie d'ordre du paquet, dont `RecentYearsFirstOrdering` est l'implémentation par défaut, SPEC §3.2). Ports du package `port`, uniquement des interfaces aux appels `suspend` : `MediaSource`, `ReviewRepository`, `SettingsRepository`, `MediaActionRequestFactory` (demandes `MediaActionRequest` opaques) et `ReminderScheduler` (ADR [0007](decisions/0007-domain-ports.md)) ; seules les lectures observées renvoient un `Flow` : `ReviewRepository.observeEntries` (ADR [0008](decisions/0008-reactive-review-days.md)) et `SettingsRepository.observeReminderSettings`. Use cases du package `usecase`, un par intention avec `operator fun invoke` : `GetReviewDays` (nombre de médias à trier par jour de la fenêtre, en `Flow`) ; `GetDeckForDay` (paquet ordonné des médias à trier d'un jour, en `suspend`, ADR [0009](decisions/0009-deck-snapshot-and-ordering.md)) ; `KeepMedia`, `MarkForDeletion` et `FavoriteMedia` (décision `KEPT`, `MARKED_FOR_DELETION`, ou `KEPT` avec favori en attente, pour le couple média, année du jour cible, sans aucune suppression ni favori réels ; chacun renvoie la `SessionDecision` à empiler) ; `UndoLastDecision` (annule la dernière décision d'une `ReviewSession` immuable, ADR [0010](decisions/0010-session-undo-stack.md)) ; `GetSessionSummary` (gardés de la session, marqués, favoris et espace libéré de toutes les demandes en attente, ADR [0011](decisions/0011-session-summary-scope.md)) ; `PrepareTrash` (demandes de corbeille par lots pour tous les médias marqués encore lisibles) et `RecordTrashResult` (lot confirmé passé à `TRASHED`, rien sur refus) ; `CleanOrphanEntries` (purge des entrées des médias disparus, seulement avec un accès complet `MediaAccess.FULL`, ADR [0012](decisions/0012-orphan-cleanup-media-access.md)) ; `ScheduleDailyReminder` *(prévu)*. Fixtures de test partagées dans `src/testFixtures` (plugin `java-test-fixtures`, package `fixtures`) : builders `aMediaItem()` et `aReviewEntry()`, `FakeMediaSource`, `FakeReviewRepository`, `FakeMediaActionRequestFactory` (lots de taille réglable) et `FixedClock`, consommés par les tests de `:data` et `:app` via `testImplementation(testFixtures(project(":domain")))` |
| `:architecture-tests` | `photoswipe.jvm.library` | `fr.babeuloula.photoswipe.architecture` | Tests d'architecture Konsist uniquement (aucun code de production). Dépend d'aucun module : Konsist lit les sources de `:app`, `:data` et `:domain`, déclarées en entrées de sa tâche `test` pour que Gradle la relance quand elles changent |

### Conventions Gradle (`build-logic/`)

La configuration commune est portée par des plugins de convention, dans le build inclus `build-logic/` (voir l'ADR [0001](decisions/0001-build-logic-convention-plugins.md)).

| Plugin | Applique | Configure |
|---|---|---|
| `photoswipe.android.application` | `com.android.application`, `photoswipe.quality` | `compileSdk` 37, `minSdk` 33, `targetSdk` 37, toolchain JVM 17, Android Lint bloquant |
| `photoswipe.android.library` | `com.android.library`, `photoswipe.quality` | `compileSdk` 37, `minSdk` 33, toolchain JVM 17, Android Lint bloquant |
| `photoswipe.jvm.library` | `org.jetbrains.kotlin.jvm`, `photoswipe.quality` | toolchain JVM 17, tests sur la plateforme JUnit |
| `photoswipe.quality` | `org.jlleitschuh.gradle.ktlint`, `io.gitlab.arturbosch.detekt` | ktlint et detekt bloquants (detekt lit aussi `src/testFixtures`, absent de ses sources par défaut), tâche `quality` par module (ktlint + detekt + Android Lint sur les modules Android). Appliqué d'office par les trois conventions ci-dessus (ADR [0003](decisions/0003-static-analysis-tooling.md)) |
| `photoswipe.android.hilt` | `com.google.devtools.ksp`, `com.google.dagger.hilt.android` | dépendances `hilt-android` et `hilt-compiler` (processeur via KSP, jamais kapt). À appliquer après une convention Android |
| `photoswipe.android.room` | `com.google.devtools.ksp`, `androidx.room` | schéma exporté dans `schemas/` du module, dépendances `room-runtime` et `room-compiler` (processeur via KSP). À appliquer après une convention Android |
| `photoswipe.android.robolectric` | — | `unitTests.isIncludeAndroidResources`, dépendance `robolectric` en test, jar Android résolu par Gradle (configuration `robolectricRuntime`, tâche `syncRobolectricJars`) et tests lancés hors ligne. À appliquer après une convention Android ; chaque module garde son `robolectric.properties` (`sdk=34`) |
| `photoswipe.android.compose` | `org.jetbrains.kotlin.plugin.compose` | `buildFeatures.compose`, BOM Compose (`implementation` et `testImplementation`), `ui`, `ui-tooling-preview`, et `ui-tooling` en `debugImplementation` seulement. À appliquer après une convention Android |

- Les valeurs sont centralisées dans `ProjectConfig`.
- Android Lint : `abortOnError` et `MissingTranslation` en erreur (SPEC §0.2), réglés dans la configuration Android commune (`KotlinAndroid.kt`).
- Les tests (`ProjectBuilder`) vérifient que chaque plugin applique les bonnes valeurs ; `./gradlew test` les lance.
- Les versions sont toutes dans `gradle/libs.versions.toml`, y compris celles des plugins AGP, Kotlin, Compose, KSP et Hilt, que `build-logic` utilise en `compileOnly`.

### Injection de dépendances (Hilt)

- `PhotoSwipeApplication` porte `@HiltAndroidApp` : c'est la racine du graphe. `MainActivity` porte `@AndroidEntryPoint` (Hilt exige une `ComponentActivity`).
- Les modules Hilt de `:app` vivent dans le package `fr.babeuloula.photoswipe.di`. `TimeModule` fournit l'horloge (`java.time.Clock`) en singleton.
- Les modules Hilt de `:data` vivent dans `fr.babeuloula.photoswipe.data.di` : `PortsModule` lie chaque port à son implémentation (`ReviewRepository` → `RoomReviewRepository`, `SettingsRepository` → `DataStoreSettingsRepository`, `MediaSource` → `MediaStoreMediaSource`, en singletons) ; `StorageModule` fournit la base Room, le DataStore des réglages (singletons, une instance par fichier) et le `ContentResolver` ; `DispatchersModule` fournit `Dispatchers.IO` sous le qualificatif `@IoDispatcher`. Hilt les agrège à la compilation de `:app`. `MediaActionRequestFactory` et `ReminderScheduler` seront liés avec leurs implémentations (epics 7 et 8).
- Les tests Hilt tournent sur la JVM avec Robolectric (`@HiltAndroidTest` + `HiltTestApplication`), sur l'API 34 (ADR [0002](decisions/0002-robolectric-api-level.md)).

### Thème et interface (Compose, Material 3)

- `MainActivity` appelle `enableEdgeToEdge()` puis affiche `PhotoSwipeTheme { PhotoSwipeApp() }`. L'application est dessinée sous les barres système, rendues transparentes.
- `ui/theme/PhotoSwipeTheme` fournit à `MaterialTheme` les couleurs, la typographie (`Type.kt`) et les formes (`Shape.kt`) : aucune valeur de style n'est écrite en dur dans les écrans (SPEC §8).
- Couleurs : dynamiques (Material You) par défaut. Avec `minSdk` 33, elles existent sur tous les appareils visés (API 31+). La palette de repli (`Color.kt`, générée par Material Theme Builder) sert quand on les désactive : aperçus, tests et futur réglage éventuel.
- Clair ou sombre : suit le système (`isSystemInDarkTheme()`). Le thème XML de la fenêtre (`Theme.PhotoSwipe`, parent `DayNight` de la plateforme) ne sert qu'avant le premier affichage de Compose.
- Insets : le `Scaffold` transmet les marges des barres système (`innerPadding`), que chaque écran applique.
- `PhotoSwipeApp` affiche pour l'instant un écran d'accueil provisoire, remplacé par la pile de swipe (epic 5).

### Internationalisation (SPEC §9)

- Ressources par défaut en anglais (`values/`), traduction française (`values-fr/`). Chaque type de ressource a son fichier : `strings.xml`, `plurals.xml`.
- Les pluriels passent par `getQuantityString` (`plurals/media_count` sert d'exemple), jamais par concaténation. Le français déclare `one`, `many` et `other`.
- `res/xml/locales_config.xml`, référencé par `android:localeConfig` dans le manifeste, liste les langues du réglage de langue par application d'Android 13+.
- Deux filets : `MissingTranslation` en erreur dans Android Lint, et `LocalizationTest` (`:app`, package `i18n`) qui vérifie que chaque clé existe en anglais et en français, sans clé orpheline, et que chaque dossier `values-xx` traduit figure dans `locales_config.xml`. Son lecteur de ressources (`TranslatableResources`) est lui-même testé sur des fixtures (`src/test/resources/fixtures/i18n`).

## 3. Flux de données *(prévu, SPEC §5.4)*

L'UI suit **MVVM** avec un **flux unidirectionnel de données (UDF)** :

```
Écran Compose ──UiEvent──► ViewModel ──appel──► Use case (:domain) ──port──► Implémentation (:data)
      ▲                        │
      └────── StateFlow<UiState> + UiEffect (navigation, dialogue système)
```

- Chaque écran a un `UiState` immuable, des `UiEvent` entrants et des `UiEffect` sortants.
- Un ViewModel ne connaît que des use cases.
- Le temps (`Clock`) et la concurrence (`CoroutineDispatcher`) sont injectés, jamais appelés en statique, pour rester testables.

## 4. Configuration Android

| Paramètre | Valeur | Raison |
|---|---|---|
| `applicationId` | `fr.babeuloula.photoswipe` | SPEC §7 |
| `minSdk` | 33 (Android 13) | SPEC §2 : permissions médias granulaires, choix de langue par application |
| `compileSdk` / `targetSdk` | 37 (Android 17) | Dernière version stable, et le Play Store impose une cible récente |
| JVM cible | 17 (toolchain) | Minimum requis par AGP 9.5. AGP déduit `compileOptions` du toolchain |
| Permissions | aucune pour l'instant | `INTERNET` est interdite (SPEC §0.6). Les permissions médias et notifications viendront avec leurs epics |

## 5. Versions

Relevées le 2026-11-03 sur les sources officielles (Google Maven, Maven Central, services.gradle.org, releases GitHub). Elles sont centralisées dans `gradle/libs.versions.toml`.

| Outil | Version | Note |
|---|---|---|
| Android Gradle Plugin (AGP) | 9.5.0 | Kotlin intégré à AGP 9 : les modules Android n'appliquent pas `kotlin-android` |
| Gradle | 9.8.2 | AGP 9.5 demande au moins Gradle 9.6.0 |
| Kotlin | 2.4.21 | Déclaré à la racine pour aligner le Kotlin intégré d'AGP |
| JDK | 17 | |
| JUnit Jupiter (BOM) | 6.1.3 | Tests de `:domain` |
| JUnit 4 | 4.13.2 | Tests unitaires des modules Android |
| KSP | 2.3.13 | Versionné indépendamment de Kotlin depuis KSP 2.3 |
| Hilt (Dagger) | 2.60.1 | Bibliothèque, processeur et plugin Gradle |
| AndroidX Activity (`activity-compose`) | 1.13.0 | |
| AndroidX Lifecycle (`lifecycle-viewmodel-compose`, `lifecycle-runtime-compose`) | 2.11.0 | Relevée le 2026-11-04 sur Google Maven |
| AndroidX Hilt (`hilt-lifecycle-viewmodel-compose`) | 1.4.0 | Relevée le 2026-11-04 sur Google Maven |
| Compose BOM | 2026.09.00 | Compose UI 1.12.1, Material 3 1.4.0 |
| Kotlin Compose compiler plugin | 2.4.21 | Même version que Kotlin |
| Robolectric | 4.17 | Tests sur l'API 34 (ADR 0002) |
| Room (runtime, compiler, testing, plugin Gradle) | 2.8.5 | Relevée le 2026-11-04 sur Google Maven, dernière stable |
| DataStore Preferences | 1.3.0 | Relevée le 2026-11-04 sur Google Maven, dernière stable |
| AndroidX Test (`runner`, `ext:junit`) | 1.7.0, 1.3.0 | Relevées le 2026-11-04 sur Google Maven |
| `reactivecircus/android-emulator-runner` (CI) | v2 (2.38.0) | Relevée le 2026-11-04 sur GitHub |
| kotlinx-coroutines (core, test) | 1.11.0 | Relevée le 2026-11-04 sur Maven Central ; core et test partagent la même version |
| Konsist | 0.18.1 | Dernière stable (la 1.0 n'est qu'en bêta), compilée avec Kotlin 2.4.21 |
| ktlint-gradle (plugin) | 14.2.0 | |
| ktlint | 1.8.0 | Moteur utilisé par le plugin, style `ktlint_official` |
| detekt | 1.23.8 | Dernière stable ; la 2.0 n'est qu'en alpha (ADR 0003) |
| GitHub Actions | `checkout@v7`, `setup-java@v6`, `gradle/actions/setup-gradle@v6`, `upload-artifact@v7`, `mikepenz/action-junit-report@v6` | CI |

## 6. Bibliothèques et justification

SPEC §0.4 : aucune bibliothèque sans justification écrite ici.

| Bibliothèque | Module | Justification |
|---|---|---|
| JUnit Jupiter 6 | `:domain` (tests) | Standard actuel des tests JVM. La SPEC demande JUnit 5 ; JUnit 6 en est la version suivante, avec la même API |
| JUnit 4 | `:app`, `:data` (tests) | Fonctionne nativement sur Android sans plugin supplémentaire |
| Room | `:data` | Base SQLite locale des décisions imposée par la SPEC §6 ; requêtes vérifiées à la compilation, `Flow` observable, schéma exporté pour tester les migrations |
| DataStore Preferences | `:data` | Préférences clé-valeur imposées par la SPEC §5.3 (réglages du rappel) : écritures atomiques et asynchrones, lecture en `Flow`, à la place de `SharedPreferences` |
| AndroidX Test (`runner`, `ext:junit`) | `:data` (tests instrumentés) | `AndroidJUnitRunner` et `AndroidJUnit4` pour lancer JUnit 4 sur un appareil ou un émulateur |
| Room Testing | `:data` (tests instrumentés) | `MigrationTestHelper` : vérifie les migrations contre les schémas exportés (SPEC §6) |
| Lifecycle ViewModel Compose | `:app` | `ViewModel` des écrans (état et effets survivent à la recréation de l'activité, SPEC §5.4) |
| Lifecycle Runtime Compose | `:app` | `collectAsStateWithLifecycle` (l'état n'est lu qu'au premier plan) et `LifecycleResumeEffect` (relire l'accès aux médias au retour des réglages) |
| Hilt Lifecycle ViewModel Compose | `:app` | `hiltViewModel()` : ViewModels `@HiltViewModel` injectés par constructeur dans Compose |
| Hilt + KSP | `:app` | Injection de dépendances imposée par la SPEC §5.5 ; KSP plutôt que kapt, plus rapide et maintenu |
| AndroidX Activity Compose | `:app` | `ComponentActivity` (classe de base exigée par `@AndroidEntryPoint`), `setContent` et `enableEdgeToEdge` |
| Jetpack Compose (BOM, UI, tooling) | `:app` | UI imposée par la SPEC §7 ; la BOM aligne les versions des bibliothèques Compose |
| Compose Material 3 | `:app` | Material 3 imposé par la SPEC §8 : thème, couleurs dynamiques, composants standards |
| Compose UI Test (JUnit 4) | `:app` (tests) | Tests de rendu Compose (`createAndroidComposeRule`) sur Robolectric |
| Hilt Android Testing | `:app` (tests) | `@HiltAndroidTest` et `HiltAndroidRule` pour vérifier l'injection dans les tests |
| Robolectric | `:app`, `:data` (tests) | Exécute le framework Android sur la JVM : tests Hilt sans émulateur, en local comme en CI |
| Robolectric `android-all-instrumented` | `:app`, `:data` (tests) | Jar Android de l'API 34 résolu par Gradle (configuration `robolectricRuntime`) : Robolectric tourne hors ligne, sans téléchargement à chaque test ni erreur 429 de Maven Central en CI. Sa version suit celle de Robolectric |
| kotlinx-coroutines-core | `:domain` (`api`) | `Flow` des ports et des use cases réactifs (`GetReviewDays`, ADR 0008) ; Kotlin pur, sans Android |
| kotlinx-coroutines-test | `:domain` (tests) | `runTest` pour appeler depuis les tests les fonctions `suspend` des ports et de leurs fakes (SPEC §11) |
| Konsist | `:architecture-tests` (tests) | Tests d'architecture imposés par la SPEC §5.1 et §11 : lit le code Kotlin (imports, packages) de tous les modules, sans les compiler ni dépendre d'eux (ADR 0005) |
| ktlint (plugin `org.jlleitschuh.gradle.ktlint`) | tous (build) | Format du code imposé par la SPEC §12 ; le plugin crée les tâches `ktlintCheck` et `ktlintFormat` par source set |
| detekt | tous (build) | Analyse statique imposée par la SPEC §12 (complexité, code mort, mauvaises pratiques) |
| `mikepenz/action-junit-report` | CI | Affiche le récapitulatif des tests JUnit dans le résumé du run GitHub Actions, sans fouiller les logs |

Prévues dans la SPEC, ajoutées avec leur ticket : Navigation Compose, Room, DataStore, Coil 3, Media3 ExoPlayer, WorkManager, MockK, Turbine.

## 7. CI

`.github/workflows/ci.yml` se lance à chaque push sur `main` et sur chaque PR. Il installe le JDK 17 (Temurin) et Gradle, valide le wrapper, puis exécute en une seule invocation Gradle `test` (modules, règles d'architecture Konsist et conventions `build-logic`), `quality` (ktlint, detekt et Android Lint) et `assembleDebug`, avec `--continue` pour remonter toutes les erreurs d'un coup. Le récapitulatif des tests JUnit s'affiche dans le résumé du run. En cas d'échec, les rapports sont publiés en artefact. Chaque nouveau check du projet y est ajouté dans la PR qui l'introduit.

- **Parallélisme** : `org.gradle.parallel=true` (`gradle.properties`) fait tourner les modules indépendants en parallèle. Un seul job plutôt que plusieurs : chaque job repartirait d'un runner neuf et recompilerait `:app`.
- **Robolectric hors ligne** : son jar Android passe par le cache Gradle au lieu d'être téléchargé à chaque run.
- **Cache Gradle** : géré par `setup-gradle` (dépendances, build cache, wrapper). Seuls les runs de `main` écrivent le cache ; les PR le lisent sans le modifier.
- **Blocage** : un ruleset GitHub sur `main` exige que le job « Quality, unit tests and debug build » soit vert avant tout merge (ADR [0004](decisions/0004-required-ci-check-on-main.md)). Renommer ce job impose de mettre à jour le ruleset.
