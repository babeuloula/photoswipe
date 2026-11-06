# Guide pour les néophytes

Ce guide explique, sans prérequis, les outils et le vocabulaire du projet, puis le rôle de chaque dossier. Pour le détail technique, voir [architecture.md](architecture.md).

## 1. Le vocabulaire

### Android
Le système d'exploitation des téléphones Google, Samsung, etc. Une application Android est distribuée sous forme de fichier **APK** (ou AAB pour le Play Store) qu'on installe sur le téléphone.

### Kotlin
Le **langage de programmation** dans lequel l'application est écrite. Google le recommande pour Android. Il est moderne, concis, et évite beaucoup d'erreurs classiques, par exemple les valeurs `null` inattendues. Les fichiers Kotlin se terminent par `.kt`. Le code Kotlin est compilé pour la **JVM** (Java Virtual Machine), le même environnement d'exécution que Java.

### JDK (Java Development Kit)
Les outils qui compilent et exécutent du code pour la JVM. Il faut un JDK installé pour construire le projet. Nous utilisons la version 17.

### Android SDK et niveaux d'API
Le **SDK** (Software Development Kit) Android contient les bibliothèques du système et les outils pour construire une application. Chaque version d'Android a un numéro d'**API** : Android 13 = API 33, Android 17 = API 37.
- `minSdk = 33` : l'application ne s'installe pas sur une version plus ancienne qu'Android 13.
- `targetSdk = 37` : l'application déclare être testée pour Android 17 et en adopte les comportements.
- `compileSdk = 37` : la version du SDK utilisée pour compiler.

### Gradle
L'outil de **construction** (« build ») du projet. Il télécharge les bibliothèques, compile le code, lance les tests et produit l'APK. On le pilote avec des **tâches**, par exemple :
- `./gradlew test` lance les tests ;
- `./gradlew assembleDebug` produit l'APK de développement.

Les fichiers de configuration Gradle sont écrits en Kotlin et se terminent par `.gradle.kts`.

### Gradle Wrapper (`gradlew`)
Un petit script fourni dans le dépôt. Il télécharge automatiquement **la bonne version de Gradle**, ce qui évite à chacun de l'installer. On utilise toujours `./gradlew` (ou `gradlew.bat` sous Windows), jamais un `gradle` installé à la main.

### Android Gradle Plugin (AGP)
L'extension de Gradle, maintenue par Google, qui sait construire une application Android : gestion des ressources, du manifeste, de l'APK, etc.

### Module
Un sous-projet Gradle, avec son propre code et sa propre configuration. Découper en modules impose des frontières : un module ne peut utiliser que les modules dont il dépend explicitement. PhotoSwipe a trois modules : `app`, `data` et `domain` (voir plus bas), plus un module `architecture-tests` qui ne contient que des tests.

### Version catalog (`libs.versions.toml`)
Un fichier unique qui liste toutes les bibliothèques et leurs versions. Les modules y font référence (`libs.junit4`) au lieu de répéter les numéros de version partout.

### Plugin de convention (`build-logic/`)
Un plugin Gradle propre au projet, qui regroupe les réglages communs à plusieurs modules : version d'Android visée, version de Java, etc. Au lieu de recopier ces réglages dans chaque module, celui-ci applique simplement `photoswipe.android.library` (ou `photoswipe.android.application`, ou `photoswipe.jvm.library`). Pour changer un réglage partout, on le modifie à un seul endroit : `build-logic/.../ProjectConfig.kt`.

### Manifeste (`AndroidManifest.xml`)
La carte d'identité de l'application pour Android : son nom, ses écrans (« activités ») et les **permissions** demandées. PhotoSwipe ne demandera jamais l'accès à Internet.

### Ressources (`res/`)
Tout ce qui n'est pas du code : textes traduits (`strings.xml`), icônes, couleurs, etc. `values/` contient la version par défaut (anglais) et `values-fr/` la traduction française. Android choisit automatiquement selon la langue du téléphone.
- **Pluriels** (`plurals.xml`) : « 1 média », « 3 médias ». Chaque langue a ses règles (en français, 0 est au singulier), donc on ne colle jamais un nombre et un mot nous-mêmes : on demande la bonne forme à Android avec `getQuantityString`.
- **Icônes** (`res/drawable/ic_*.xml`) : des vecteurs Material Symbols Rounded, une seule famille dans toute l'app. Pour en ajouter une, prendre le fichier `symbols/android/<nom>/materialsymbolsrounded/<nom>_24px.xml` (ou `<nom>_fill1_24px.xml` pour une icône pleine, enregistrée en `ic_<nom>_filled.xml`) du dépôt `google/material-design-icons`, retirer son attribut `android:tint` et la lire avec `painterResource(R.drawable.ic_<nom>)`.
- **Polices** (`res/font/`) : Atkinson Hyperlegible Next et IBM Plex Mono, embarquées car l'app n'a pas accès à internet ; elles sont citées dans l'écran des licences.
- **Langue par application** (`res/xml/locales_config.xml`) : la liste des langues que l'on peut choisir pour PhotoSwipe seule, dans les réglages d'Android 13+, sans changer la langue du téléphone.
- **Ajouter une langue** : créer `values-xx/` avec toutes les clés, puis ajouter `xx` dans `locales_config.xml`. Un test (`LocalizationTest`) échoue si une clé manque ou si la langue n'est pas déclarée.

### Activity
Un « écran » au sens Android, et le point d'entrée de l'application. PhotoSwipe n'en aura qu'une (`MainActivity`) ; les différents écrans seront dessinés en Compose à l'intérieur.

### Hilt (injection de dépendances)
Une classe a souvent besoin d'autres objets pour travailler : une horloge, une base de données… Plutôt que de les créer elle-même, elle les reçoit dans son constructeur. **Hilt** se charge de fabriquer ces objets et de les lui fournir. Avantage : dans un test, on peut lui donner une fausse horloge ou une fausse base.
- `@HiltAndroidApp` sur `PhotoSwipeApplication` démarre Hilt au lancement de l'application.
- `@AndroidEntryPoint` sur `MainActivity` permet à Hilt d'y injecter des objets.
- Un **module Hilt** (`@Module`, dans le dossier `di/`) explique à Hilt comment fabriquer un objet qu'il ne sait pas construire seul, par exemple `TimeModule` pour l'horloge.

### KSP
Un outil qui lit les annotations du code (comme `@Inject`) pendant la compilation et génère du code Kotlin. Hilt s'en sert pour écrire automatiquement le code d'assemblage. Il remplace l'ancien outil, **kapt**, plus lent.

### Jetpack Compose
La bibliothèque de Google pour décrire l'interface en Kotlin plutôt qu'en XML. Un écran est une fonction annotée `@Composable` ; quand les données changent, Compose redessine seulement ce qui doit l'être. Une fonction annotée `@Preview` s'affiche directement dans Android Studio, sans lancer l'application.

### Material 3 et thème
**Material 3** est le système de design de Google : composants (boutons, barres, cartes), couleurs, typographie et formes. Le **thème** (`PhotoSwipeTheme`, dans `ui/theme/`) rassemble ces choix à un seul endroit ; les écrans les lisent via `MaterialTheme` au lieu d'écrire des couleurs ou des tailles en dur.
- **Palette de l'application** (`Color.kt`) : PhotoSwipe garde toujours ses couleurs ambre, en clair et en sombre, au lieu d'adopter celles qu'Android tire du fond d'écran (Material You) : c'est sa direction artistique.
- **Clair / sombre** : le thème suit le réglage du téléphone.
- **Edge-to-edge** : l'application se dessine sur tout l'écran, y compris sous la barre d'état et la barre de navigation. Les **insets** sont les marges qu'il faut laisser pour que le contenu ne passe pas sous ces barres ; le `Scaffold` de Material 3 les calcule pour nous.

### Outils de qualité : ktlint, detekt, Android Lint
Trois « relecteurs automatiques », qui font échouer le build au moindre problème :
- **ktlint** vérifie la **mise en forme** (espaces, indentation, ordre des imports). Les règles sont dans `.editorconfig`. `./gradlew ktlintFormat` corrige tout seul la plupart des problèmes.
- **detekt** cherche les **mauvaises pratiques** : fonction trop longue ou trop complexe, code inutilisé, « nombre magique » sans nom… Ses réglages sont dans `config/detekt/detekt.yml`.
- **Android Lint** connaît Android : ressource manquante, API trop récente pour le téléphone, **traduction manquante**. Une chaîne présente en anglais mais pas en français fait échouer le build.

Une seule commande lance les trois : `./gradlew quality`.

### Outils qui arrivent dans les prochains tickets
- **Room** et **DataStore** : le stockage local, pour les décisions de tri et les préférences.

### Tests unitaires
Du code qui vérifie automatiquement que le code fonctionne. Ils se trouvent dans `src/test/` et se lancent avec `./gradlew test`, sans téléphone ni émulateur.

Dans `app` et `data`, certains tests ont besoin d'Android (par exemple pour démarrer `MainActivity` ou ouvrir la base de données). **Robolectric** simule Android directement sur l'ordinateur, ce qui évite l'émulateur. Il est réglé sur Android 14 (API 34) dans `src/test/resources/robolectric.properties` de chaque module, car les versions plus récentes demanderaient Java 21.

Le module `domain` fournit aussi des **fixtures de test** dans `src/testFixtures/` : du code qui n'est jamais livré dans l'application, mais que les tests de tous les modules peuvent utiliser. On y trouve des *builders* (`aMediaItem()` crée un média valide, on ne précise que ce qui compte pour le test), des *fakes* (de fausses implémentations des ports, en mémoire, à la place de MediaStore ou de Room) et `FixedClock`, une horloge figée que le test avance à la main.

Le détail (outils par module, commandes pour lancer un seul test, fixtures) est dans [`docs/testing.md`](testing.md).

### CI (intégration continue)
À chaque envoi de code sur GitHub, des serveurs de GitHub (**GitHub Actions**) construisent le projet et lancent les tests et le lint. Si quelque chose casse, la PR le signale en rouge, et GitHub refuse de la fusionner dans `main` tant que la CI n'est pas verte. Le résumé du run liste les tests passés et échoués.

### PR (Pull Request)
Une proposition de modification sur GitHub, relue avant d'être fusionnée dans la branche principale `main`.

## 2. À quoi servent les dossiers

```
photoswipe/
├── .github/workflows/ci.yml   Configuration de la CI GitHub Actions
├── app/                       Module « application » : ce qui s'installe sur le téléphone
│   ├── build.gradle.kts       Sa configuration Gradle (SDK, identifiant, dépendances)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml   Carte d'identité de l'application
│       │   ├── kotlin/               Code Kotlin : PhotoSwipeApplication, MainActivity, di/ (modules Hilt), permission/ (lecture des autorisations), locale/ (langue de l'application), reminder/ (notification du rappel, replanification après un changement d'heure), ui/ (écrans Compose : ui/navigation/ pour le passage d'un écran à l'autre, ui/permission/ pour l'accès aux photos, ui/home/ pour l'accueil, ui/deck/ pour la pile de swipe et la carte de chaque média, ui/summary/ pour le résumé avant confirmation, ui/settings/ pour les réglages, ui/licenses/ pour les licences) et ui/theme/ (thème Material 3)
│       │   └── res/                  Ressources : textes (values/ en anglais, values-fr/ en français), thème de la fenêtre et écran de démarrage (themes.xml), couleurs hors Compose (values/colors.xml, values-night/ pour le sombre), icônes (drawable/), icône de l'application (mipmap-anydpi/), polices (font/), textes des licences (raw/)
│       ├── debug/                    Code présent seulement dans la version de développement : outils développeur (données de démo, médias d'exemple dans la vraie galerie, remise à zéro des décisions), aussi ouverts depuis les réglages
│       ├── release/                  Code présent seulement dans la version publiée (lecture de la vraie galerie)
│       ├── test/                     Tests unitaires du module (Robolectric pour ce qui touche à Android)
│       └── testDebug/, testRelease/  Tests propres à chaque version
├── .editorconfig              Règles de mise en forme partagées par l'éditeur et ktlint
├── architecture-tests/        Tests d'architecture (Konsist) : vérifient que chaque module n'utilise que ce qu'il a le droit d'utiliser
│   ├── build.gradle.kts
│   └── src/test/
│       ├── kotlin/                   Les règles et leurs tests
│       └── resources/fixtures/       Faux fichiers Kotlin (jamais compilés) qui servent d'exemples pour tester les règles
├── build-logic/               Plugins de convention : réglages Gradle communs à tous les modules
│   └── convention/src/
│       ├── main/kotlin/              Les plugins (Android, JVM, Hilt, Compose, Room, Robolectric, qualité) et ProjectConfig (SDK, version de Java)
│       └── test/kotlin/              Tests qui vérifient que les plugins appliquent les bons réglages
├── config/detekt/detekt.yml   Réglages de detekt (analyse statique)
├── data/                      Module « données » : accès aux photos du téléphone, base de données…
│   ├── build.gradle.kts
│   ├── schemas/                      Description de la base Room, générée à la compilation et versionnée (sert à tester les migrations)
│   ├── src/main/kotlin/…/database/   La base Room : la table des décisions (ReviewEntryEntity) et ses requêtes (ReviewEntryDao), le journal des décisions pour « Annuler » (DecisionRecordEntity, DecisionJournalDao)
│   ├── src/main/kotlin/…/reminder/   WorkManagerReminderScheduler : planifie chaque jour la tâche du rappel (WorkManager)
│   ├── src/main/kotlin/…/review/     RoomReviewRepository : enregistre les décisions du domaine dans la base ; RoomDecisionJournal : tient le journal des décisions
│   ├── src/main/kotlin/…/settings/   DataStoreSettingsRepository : enregistre les réglages (rappel quotidien)
│   ├── src/main/kotlin/…/media/      MediaStoreMediaSource : lit les photos et vidéos de la galerie du téléphone (MediaStore)
│   ├── src/main/kotlin/…/di/         Modules Hilt : disent quelle classe de data fournir pour chaque interface du domaine
│   ├── src/debug/                    Version de développement seulement : le seeder qui ajoute de vraies photos et vidéos datées à la galerie (seed/), et sa vidéo d'exemple (res/raw)
│   ├── src/androidTest/              Tests instrumentés : ils tournent sur un émulateur ou un téléphone (vraie galerie, vraie base)
│   └── src/test/
├── domain/                    Module « domaine » : les règles métier, en Kotlin pur
│   ├── build.gradle.kts
│   ├── src/main/kotlin/…/model/  Les modèles : MediaItem, ReviewDecision, ReviewDay, ReviewEntry, SessionSummary…
│   ├── src/main/kotlin/…/port/  Les ports : des interfaces (MediaSource, ReviewRepository…) que le module data implémente
│   ├── src/main/kotlin/…/service/  Les services purs : SameDayMatcher (médias « du même jour »), ReviewWindowProvider (les 7 jours de rattrapage), DeckOrdering (l'ordre des cartes)…
│   ├── src/main/kotlin/…/usecase/  Les cas d'usage, un par intention de l'utilisateur : GetReviewDays (combien de médias à trier par jour), GetDeckForDay (les cartes d'un jour, dans l'ordre), KeepMedia (garder un média), MarkForDeletion (le marquer pour suppression), FavoriteMedia (le mettre en favori), UndoLastDecision (annuler la dernière décision), GetDaySession (retrouver les décisions d'un jour à annuler), GetSessionSummary (le résumé avant confirmation), PrepareTrash et RecordTrashResult (envoyer à la corbeille après confirmation), RequestFavorite et RecordFavoriteResult (ajouter tout de suite un favori à la galerie), CleanOrphanEntries (oublier les médias disparus)…
│   ├── src/testFixtures/kotlin/…/fixtures/  Outils de test partagés avec data et app : builders aMediaItem(), aReviewEntry(), faux ports (FakeMediaSource, FakeReviewRepository, FakeMediaActionRequestFactory), FixedClock
│   └── src/test/
├── docs/                      Documentation (ce guide, l'architecture, les décisions)
├── scripts/                   Outils hors Gradle : seed-emulator.sh pousse sur l'émulateur des photos datées (pour tester les dialogues de corbeille d'Android), avec son générateur Python et ses tests
├── gradle/
│   ├── libs.versions.toml     Version catalog : toutes les bibliothèques et versions
│   └── wrapper/               Fichiers du Gradle Wrapper (ne pas modifier à la main)
├── build.gradle.kts           Configuration Gradle du projet racine (déclaration des plugins)
├── settings.gradle.kts        Liste des modules, des dépôts de bibliothèques et inclusion de build-logic
├── gradle.properties          Réglages de Gradle (mémoire, cache…)
├── gradlew, gradlew.bat       Scripts du Gradle Wrapper (macOS/Linux et Windows)
├── local.properties           Chemin du SDK Android sur ta machine (non versionné)
├── README.md                  Présentation courte du projet et liens vers la doc
├── CLAUDE.md                  Règles et commandes pour Claude Code et les contributeurs
└── SPEC.md                    La spécification du projet
```

Les dossiers `build/` et `.gradle/`, créés automatiquement à la construction, ne sont pas versionnés. On peut les supprimer sans risque.

### Pourquoi trois modules ?
Une analogie avec un restaurant :
- **`domain`, la recette** : elle décrit *quoi* faire (« garder une photo », « trouver les photos du même jour les années précédentes ») sans savoir dans quelle cuisine elle est préparée. C'est du Kotlin pur, testable très rapidement, et il ne dépend de rien.
- **`data`, la cuisine et le garde-manger** : il sait *comment* aller chercher les ingrédients, c'est-à-dire lire les photos du téléphone et enregistrer les décisions en base.
- **`app`, la salle** : ce que voit l'utilisateur (les écrans, les gestes de swipe). C'est aussi là que tout est assemblé.

Comme la recette ne dépend pas de la cuisine, on pourrait changer la manière de stocker les données sans toucher aux règles métier.

Ces frontières sont vérifiées automatiquement par des **tests d'architecture** écrits avec **Konsist**, une bibliothèque qui lit le code Kotlin comme du texte structuré (fichiers, packages, imports). Si quelqu'un importe par exemple une classe Android dans `domain`, ou une classe de `data` dans un écran de `app`, `./gradlew test` échoue et la CI devient rouge.

Le domaine décrit ce dont il a besoin sous forme d'**interfaces**, appelées « ports » (package `port`) : par exemple `MediaSource`, « donne-moi les médias pris entre telle et telle date ». Le module `data` fournit les classes qui font vraiment le travail avec MediaStore ou Room, et Hilt les branche. Les fonctions de ces interfaces sont marquées `suspend` : elles peuvent attendre (lecture disque, base de données) sans bloquer l'écran. Certaines renvoient un `Flow` : un flux de valeurs qui se met à jour tout seul, par exemple le nombre de médias à trier, qui baisse à chaque décision.

## 3. Lancer le projet

1. Installer [Android Studio](https://developer.android.com/studio). Il fournit le SDK Android et un JDK.
2. Ouvrir le dossier du projet dans Android Studio. Il synchronise Gradle tout seul.
3. En ligne de commande, avec un JDK 17 :

```bash
./gradlew test            # lancer les tests
./gradlew quality         # vérifier le format et la qualité (ktlint, detekt, Android Lint)
./gradlew ktlintFormat    # corriger automatiquement la mise en forme
./gradlew assembleDebug   # construire l'APK de développement
```

L'APK produit se trouve dans `app/build/outputs/apk/debug/app-debug.apk`.
