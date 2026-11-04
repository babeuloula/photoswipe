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
Un sous-projet Gradle, avec son propre code et sa propre configuration. Découper en modules impose des frontières : un module ne peut utiliser que les modules dont il dépend explicitement. PhotoSwipe a trois modules : `app`, `data` et `domain` (voir plus bas).

### Version catalog (`libs.versions.toml`)
Un fichier unique qui liste toutes les bibliothèques et leurs versions. Les modules y font référence (`libs.junit4`) au lieu de répéter les numéros de version partout.

### Plugin de convention (`build-logic/`)
Un plugin Gradle propre au projet, qui regroupe les réglages communs à plusieurs modules : version d'Android visée, version de Java, etc. Au lieu de recopier ces réglages dans chaque module, celui-ci applique simplement `photoswipe.android.library` (ou `photoswipe.android.application`, ou `photoswipe.jvm.library`). Pour changer un réglage partout, on le modifie à un seul endroit : `build-logic/.../ProjectConfig.kt`.

### Manifeste (`AndroidManifest.xml`)
La carte d'identité de l'application pour Android : son nom, ses écrans (« activités ») et les **permissions** demandées. PhotoSwipe ne demandera jamais l'accès à Internet.

### Ressources (`res/`)
Tout ce qui n'est pas du code : textes traduits (`strings.xml`), icônes, couleurs, etc. `values/` contient la version par défaut (anglais) et `values-fr/` la traduction française. Android choisit automatiquement selon la langue du téléphone.

### Activity
Un « écran » au sens Android, et le point d'entrée de l'application. PhotoSwipe n'en aura qu'une (`MainActivity`) ; les différents écrans seront dessinés en Compose à l'intérieur.

### Outils qui arrivent dans les prochains tickets
- **Jetpack Compose** : la bibliothèque de Google pour décrire l'interface en Kotlin plutôt qu'en XML.
- **Material 3** : le système de design de Google (composants, couleurs, typographie).
- **Hilt** : l'outil d'**injection de dépendances**. Il fournit automatiquement à chaque classe les objets dont elle a besoin, ce qui facilite les tests.
- **Room** et **DataStore** : le stockage local, pour les décisions de tri et les préférences.
- **ktlint**, **detekt** et **Android Lint** : des outils qui vérifient la mise en forme et la qualité du code.

### Tests unitaires
Du code qui vérifie automatiquement que le code fonctionne. Ils se trouvent dans `src/test/` et se lancent avec `./gradlew test`, sans téléphone ni émulateur.

### CI (intégration continue)
À chaque envoi de code sur GitHub, des serveurs de GitHub (**GitHub Actions**) construisent le projet et lancent les tests et le lint. Si quelque chose casse, la PR le signale en rouge.

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
│       │   ├── kotlin/               Code Kotlin (MainActivity, et plus tard les écrans)
│       │   └── res/                  Ressources : textes, icônes, couleurs
│       └── test/                     Tests unitaires du module
├── build-logic/               Plugins de convention : réglages Gradle communs à tous les modules
│   └── convention/src/
│       ├── main/kotlin/              Les plugins et ProjectConfig (SDK, version de Java)
│       └── test/kotlin/              Tests qui vérifient que les plugins appliquent les bons réglages
├── data/                      Module « données » : accès aux photos du téléphone, base de données…
│   ├── build.gradle.kts
│   └── src/test/
├── domain/                    Module « domaine » : les règles métier, en Kotlin pur
│   ├── build.gradle.kts
│   └── src/test/
├── docs/                      Documentation (ce guide, l'architecture, les décisions)
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

## 3. Lancer le projet

1. Installer [Android Studio](https://developer.android.com/studio). Il fournit le SDK Android et un JDK.
2. Ouvrir le dossier du projet dans Android Studio. Il synchronise Gradle tout seul.
3. En ligne de commande, avec un JDK 17 :

```bash
./gradlew test            # lancer les tests
./gradlew assembleDebug   # construire l'APK de développement
```

L'APK produit se trouve dans `app/build/outputs/apk/debug/app-debug.apk`.
