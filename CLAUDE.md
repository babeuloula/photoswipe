# CLAUDE.md — PhotoSwipe

> **À maintenir à jour.** Toute PR qui change une commande, une convention, la structure des modules ou le processus de travail met à jour ce fichier dans la même PR.

Application Android (Kotlin + Jetpack Compose) qui fait revoir chaque jour les photos et vidéos prises le même jour lors des années précédentes. Tri par swipe : gauche = marquer pour suppression, droite = garder, haut = menu favori / partage.

## Sources de vérité
- `SPEC.md` : la spécification complète. **Elle fait foi**, en particulier le §0 (règles non négociables) et le §16 (points ouverts, valeurs par défaut appliquées sauf avis contraire).
- `docs/architecture.md` : les modules, le sens des dépendances, les versions et la justification de chaque bibliothèque.
- `docs/beginners-guide.md` : le guide pour les néophytes (Kotlin, Gradle, Android, rôle de chaque dossier).
- `docs/testing.md` : la stratégie de tests, les outils par module et les commandes.
- `README.md` : présentation courte et liens vers la documentation.
- `docs/decisions/` : les ADR. Toute décision structurante absente de la spec y est écrite au lieu d'être improvisée en silence.

## Règles permanentes (résumé du SPEC §0)
- Le code est en anglais. Les commentaires et la KDoc sont en français.
- Aucune chaîne en dur dans l'UI : `values/strings.xml` en anglais, `values-fr/` en français. `MissingTranslation` est une erreur.
- `:domain` est du Kotlin JVM pur : ni Android, ni Room, ni Compose. Les dépendances vont toujours **vers** `:domain`.
- Principes SOLID et KISS, injection par constructeur (Hilt + KSP), aucun singleton statique.
- Aucune permission `INTERNET`. L'application est 100 % locale.
- Chaque ticket est livré avec ses tests. Rien n'est terminé sans tests verts, en local comme en CI.
- Aucune nouvelle bibliothèque sans une justification d'une ligne dans `docs/architecture.md`.
- Versions : vérifier les dernières versions stables sur les sources officielles (Google Maven, Maven Central, services.gradle.org), pas de mémoire.

## Commandes
Prérequis : JDK 17 et le SDK Android (`local.properties` contient `sdk.dir`, ce fichier n'est pas commité).

```bash
# En local sur la machine de Jérémy : le JBR d'Android Studio est en version 25, on force le JDK 17
export JAVA_HOME=/opt/homebrew/opt/openjdk@17

./gradlew test            # tests unitaires de tous les modules + tests des conventions build-logic
./gradlew quality         # ktlint + detekt + Android Lint sur tous les modules (bloquant)
./gradlew ktlintFormat    # corrige automatiquement la mise en forme
./gradlew assembleDebug   # APK debug dans app/build/outputs/apk/debug/
./gradlew test quality assembleDebug   # ce que lance le job obligatoire de la CI
./gradlew connectedDebugAndroidTest   # tests instrumentés (src/androidTest), émulateur démarré ; second job de la CI
```

## Structure
- `:app` : UI, navigation, assemblage DI (modules Hilt dans le package `di`), point d'entrée Android (`fr.babeuloula.photoswipe`). L'application est verrouillée en portrait (`screenOrientation="portrait"` dans le manifeste, décision de Jérémy). Chaque écran vit dans son package `ui/<écran>` : `XxxScreen` sans état (aperçus, tests UI), relié à son `XxxViewModel` (`@HiltViewModel`, `hiltViewModel()`), qui expose un `XxxUiState` en `StateFlow`, reçoit des `XxxEvent` et émet des `XxxEffect` ponctuels (canal lu une fois). L'accès aux médias passe par `MediaAccessGate`, qui entoure le `NavHost` et n'affiche la navigation qu'avec la permission. Navigation : routes `@Serializable` dans `ui/navigation/Routes.kt` (types simples seulement, par exemple `DeckRoute(epochDay)`), graphe dans `PhotoSwipeNavHost` ; un écran réel remplace sa coquille (`ShellScreen`) en gardant sa route (ADR 0014). Le thème Material 3 vit dans `ui/theme` (`PhotoSwipeTheme`) : les écrans lisent couleurs, typo et formes via `MaterialTheme`, jamais en dur.
- `:data` : implémentations Android des ports du domaine : MediaStore, Room, DataStore, WorkManager (`fr.babeuloula.photoswipe.data`). Ses modules Hilt vivent dans `data/di` (`PortsModule`, `StorageModule`, `DispatchersModule` avec `@IoDispatcher`) : un port se lie là, sauf `MediaSource`, lié dans `:app` par variante (`src/release` : MediaStore ; `src/debug` : `SwitchableMediaSource` et données de démo, ADR 0015). La base Room vit dans `database` (`PhotoSwipeDatabase`, `ReviewEntryEntity`, `ReviewEntryDao`) ; son schéma est exporté dans `data/schemas/` et commité : changer l'entité impose une nouvelle version et une migration (ADR 0013).
- `:domain` : modèles (`model`), services purs (`service`), ports (`port`, uniquement des interfaces aux appels `suspend` ; `Flow` seulement pour une lecture observée, comme `ReviewRepository.observeEntries` ou `SettingsRepository.observeReminderSettings`, ADR 0007 et 0008), use cases (`usecase`, un par intention avec `operator fun invoke`), en Kotlin JVM pur (`fr.babeuloula.photoswipe.domain`). Fixtures de test partagées dans `src/testFixtures` (package `fixtures` : `aMediaItem()`, `aReviewEntry()`, `FakeMediaSource`, `FakeReviewRepository`, `FakeMediaActionRequestFactory`, `FixedClock`), consommées par `:data` et `:app` via `testImplementation(testFixtures(project(":domain")))`.
- `:architecture-tests` : tests d'architecture Konsist seulement (ADR 0005). `:domain` n'importe ni `android.*`, ni `androidx.*`, ni `com.google.android.*`, ni rien du projet hors `…domain` ; `…domain.port` ne déclare que des interfaces ; `:app` n'importe `…data` que depuis son package `di`. Chaque règle est testée sur des fixtures (`src/test/resources/fixtures`). Un nouveau module ou une nouvelle règle de dépendance s'ajoute ici, et ses sources dans les entrées de la tâche `test` du module.
- `build-logic/` (build inclus) : plugins de convention `photoswipe.android.application`, `photoswipe.android.library`, `photoswipe.jvm.library`, `photoswipe.android.hilt` (KSP + Hilt), `photoswipe.android.compose` (compilateur Compose + BOM), `photoswipe.android.room` (KSP + Room, schémas exportés dans `schemas/`) et `photoswipe.android.robolectric` (Robolectric hors ligne), ces quatre derniers à appliquer après une convention Android, et `photoswipe.quality` (ktlint + detekt + tâche `quality`), appliqué d'office par les trois conventions de module (ADR 0003). Les valeurs partagées (SDK, JVM) sont dans `ProjectConfig`. Un module applique sa convention et ne déclare que ce qui lui est propre (namespace, dépendances). Toute nouvelle configuration commune (Compose, Hilt, qualité) devient un plugin de convention (ADR 0001).
- i18n : `values/` (anglais) et `values-fr/`, un fichier par type (`strings.xml`, `plurals.xml`). Une nouvelle langue `values-xx` s'ajoute aussi dans `res/xml/locales_config.xml` ; `LocalizationTest` vérifie la parité des clés et la déclaration des langues.
- Qualité : style ktlint `ktlint_official` (`.editorconfig`), règles detekt ajustées et justifiées dans `config/detekt/detekt.yml`, Android Lint avec `MissingTranslation` en erreur.
- Versions : `gradle/libs.versions.toml`, sans aucune version en dur ailleurs. CI : `.github/workflows/ci.yml` ; son job « Quality, unit tests and debug build » est un check obligatoire du ruleset de `main` (ADR 0004), le renommer impose de mettre à jour le ruleset ; le job « Instrumented tests (emulator) » lance les tests instrumentés.

## Tests
- `:domain` et `:architecture-tests` : JUnit Jupiter (JUnit 6). Les fonctions `suspend` se testent dans `runTest` (kotlinx-coroutines-test). Un `Flow` se teste avec `first()`, ou en collectant dans une liste depuis `backgroundScope` avec `UnconfinedTestDispatcher(testScheduler)`. Turbine, prévu par la SPEC §11, n'est pas encore ajouté (ADR 0008).
- Données de test : les builders, fakes et `FixedClock` de `:domain` (`testFixtures`) plutôt que des objets construits à la main ; un nouveau fake respecte le contrat de son port et est lui-même testé.
- Modules Android (`:app`, `:data`) : JUnit 4, en attendant une décision ADR sur JUnit 5/6 côté Android.
- Tests qui ont besoin d'Android ou de Hilt : Robolectric (convention `photoswipe.android.robolectric`, `robolectric.properties` à `sdk=34` dans chaque module) + `@HiltAndroidTest` (`HiltTestApplication`), sur l'API 34 (ADR 0002). Room se teste sur une base en mémoire (`Room.inMemoryDatabaseBuilder`). Robolectric tourne hors ligne : monter sa version impose de mettre à jour `robolectric-androidAll` dans le catalogue (sinon « Path is not a file »).
- Variantes : `src/debug` (outils développeur, données de démo, jamais en release) et `src/release`. Leurs tests vont dans `src/testDebug` et `src/testRelease` ; les tests unitaires release sont activés mais filtrés sur `src/testRelease` (`app/build.gradle.kts`).
- Tests Compose : `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule<MainActivity>()` (les API v1 sont dépréciées). Pas de `createComposeRule()` : son activité hôte (`ui-test-manifest`) n'est pas visible des tests unitaires Robolectric ; pour lire des valeurs de thème, composer dans `Robolectric.buildActivity(ComponentActivity::class.java)`.
- Tests instrumentés (`src/androidTest`, `:data`) : `AndroidJUnit4`, mêmes noms de tests qu'ailleurs (les espaces sont acceptés par le DEX depuis l'API 30, sous notre `minSdk` 33). MediaStore réel : insérer des JPEG datés par EXIF (MediaStore recalcule `DATE_TAKEN`) et les supprimer ; migrations Room avec `MigrationTestHelper`.
- Les noms des tests sont en anglais et décrivent le comportement, par exemple `` `moves leap day media to February 28 on non-leap years` ``.

## Processus de travail
1. On travaille un ticket (PS-N) à la fois, dans l'ordre de l'epic. Chaque ticket a sa branche `ps-N-description` et sa PR vers `main`.
2. Avant de commiter, `./gradlew test quality assembleDebug` doit passer.
3. Les commits suivent Conventional Commits et citent le ticket, par exemple `feat(core): add multi-module skeleton (PS-1)`.
4. Chaque PR est assignée à `babeuloula` et porte :
   - un label `epic: N-nom` ;
   - un ou plusieurs labels `area: build|ci|quality|app|data|domain|i18n|testing` ;
   - un label de type : `enhancement`, `bug`, `documentation` ou `accessibility`.

   Lancer `gh label list` avant de créer un nouveau label.
5. **La CI est maintenue au fil du projet.** Tout nouveau check (ktlint, detekt, Konsist, tests instrumentés, nouveau module, etc.) est ajouté à `.github/workflows/ci.yml` dans la même PR. Une PR n'est prête que si son run CI est vert.
6. **La documentation est maintenue au fil du projet.** On met à jour `CLAUDE.md`, `docs/architecture.md`, `docs/beginners-guide.md` et `docs/testing.md` dès qu'un module, un dossier, un outil ou une commande change.
7. **Démo sur émulateur** : si le ticket a un effet visible, on lance l'émulateur (`~/Library/Android/sdk/emulator/emulator -avd Medium_Phone_API_37.0`), on installe l'app (`./gradlew installDebug`) et on donne à Jérémy la liste de ce qu'il faut tester et comment. Si le ticket n'a rien de visible, on le dit explicitement.
8. **Suivi Notion** : les tickets vivent dans le kanban Notion « Tickets de développement » (page PhotoSwipe). Le `Ticket ID` Notion (PS-N) fait foi pour la numérotation. Le statut avance au fil du travail :
   - « En cours » quand on commence le ticket ;
   - « En revue » à l'ouverture de la PR, avec les critères d'acceptation cochés et le lien de la PR ;
   - « Terminé » une fois la PR mergée.
9. Après avoir ouvert la PR, on s'arrête et on attend la revue avant le ticket suivant.
10. Ne rien faire qui dépasse le périmètre du ticket et de l'epic en cours.

## État d'avancement
- Epic 1 « Squelette & outillage » : PS-1 mergé (PR #1), PS-2 mergé (PR #2), PS-3 mergé (PR #3), PS-4 mergé (PR #4), PS-5 mergé (PR #5), PS-6 mergé (PR #6), PS-7 mergé (PR #7), PS-8 mergé (PR #8), PS-9 mergé (PR #9). Epic 1 terminé.
- Epic 2 « Domaine » : PS-10 mergé (PR #10, modèles du domaine dans `domain/model`, ADR 0006), PS-11 mergé (PR #11, `SameDayMatcher` dans `domain/service`, fuseau passé à chaque appel), PS-12 mergé (PR #12, 29 février affiché le 28 février des années non bissextiles), PS-13 mergé (PR #13, `ReviewWindowProvider`, fenêtre de 7 jours sur une `Clock` injectée), PS-14 mergé (PR #14, ports du domaine dans `domain/port`, modèles `ReviewEntry` et `ReminderSettings`, ADR 0007), PS-15 mergé (PR #15, fixtures de test dans `domain/src/testFixtures`), PS-16 mergé (PR #16, use case `GetReviewDays` en `Flow`, `ReviewRepository.observeEntries`, ADR 0008), PS-17 mergé (PR #17, use case `GetDeckForDay` en `suspend`, stratégie `DeckOrdering`, ADR 0009), PS-18 mergé (PR #18, use case `KeepMedia`), PS-19 mergé (PR #19, use case `MarkForDeletion`), PS-20 mergé (PR #20, use case `UndoLastDecision`, `ReviewSession` immuable, ADR 0010), PS-21 mergé (PR #21, use case `FavoriteMedia`), PS-22 mergé (PR #22, use case `GetSessionSummary`, ADR 0011), PS-23 mergé (PR #23, use cases `PrepareTrash` et `RecordTrashResult`), PS-24 mergé (PR #24, use case `CleanOrphanEntries`, enum `MediaAccess`, ADR 0012). Epic 2 terminé.
- Epic 3 « Données » : PS-25 mergé (PR #25, base Room `PhotoSwipeDatabase`, conventions `photoswipe.android.room` et `photoswipe.android.robolectric`, ADR 0013), PS-26 mergé (PR #26, `RoomReviewRepository` dans `data/review`), PS-27 mergé (PR #27, `DataStoreSettingsRepository` dans `data/settings`, rappel désactivé par défaut comme le veut l'ADR 0007), PS-28 mergé (PR #28, `MediaStoreMediaSource` dans `data/media`, photos), PS-29 mergé (PR #29, vidéos, repli sur `DATE_ADDED`, corbeille exclue), PS-30 mergé (PR #30), PS-31 mergé (PR #31, câblage Hilt de `:data` dans `data/di`). Epic 3 terminé.
- Epic 4 « Permissions & accueil » : PS-32 mergé (PR #32, écran d'explication, app en portrait), PS-33 mergé (PR #33, parcours d'accès aux médias `MediaAccessGate` / `MediaAccessViewModel`, refus avec lien vers les réglages, révocation ; `MediaAccessReader` dans `permission`), PS-34 mergé (PR #34, accès partiel d'Android 14+ : `READ_MEDIA_VISUAL_USER_SELECTED`, `LimitedAccessBanner`), PS-35 mergé (PR #35, navigation typée `ui/navigation` et coquilles d'écrans, retour prédictif, ADR 0014), PS-36 en revue (données de démo en debug, `MediaSource` lié par variante, ADR 0015).
