# PhotoSwipe

[![CI](https://github.com/babeuloula/photoswipe/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/babeuloula/photoswipe/actions/workflows/ci.yml)

Application Android qui fait revoir chaque jour les photos et vidéos prises le même jour lors des années précédentes. On les trie d'un geste : à gauche pour marquer à supprimer, à droite pour garder, vers le haut pour mettre en favori ou partager. L'application est 100 % locale, sans accès à Internet.

## Prérequis

- **JDK 17**. Sur macOS avec Homebrew : `brew install openjdk@17`, puis `export JAVA_HOME=/opt/homebrew/opt/openjdk@17`. Le JDK embarqué d'Android Studio est trop récent.
- **SDK Android** (installé avec [Android Studio](https://developer.android.com/studio)). Le fichier `local.properties`, non commité, indique son emplacement : `sdk.dir=/Users/<vous>/Library/Android/sdk`.
- Un téléphone ou un émulateur sous **Android 13** (API 33) ou plus récent.

Gradle est fourni par le wrapper (`./gradlew`) : rien d'autre à installer.

## Lancer l'application

```bash
~/Library/Android/sdk/emulator/emulator -avd <nom_de_l_avd> &   # ou brancher un téléphone en débogage USB
./gradlew installDebug                                          # compile et installe la version debug
```

L'APK seul se construit avec `./gradlew assembleDebug` (dans `app/build/outputs/apk/debug/`). Depuis Android Studio, ouvrir le dossier du projet puis lancer la configuration `app`.

## Lancer les tests

```bash
./gradlew test                         # tous les tests unitaires, sans émulateur
./gradlew quality                      # ktlint, detekt et Android Lint
./gradlew test quality assembleDebug   # ce que lance la CI
```

Stratégie, tests par module et commandes ciblées : [docs/testing.md](docs/testing.md).

## Utiliser le seeder *(prévu, epic 10)*

Un seeder de debug et un script `scripts/seed-emulator.sh` rempliront l'émulateur de photos et vidéos datées, pour tester l'application sans ses propres souvenirs.

## Documentation

- [Spécification](SPEC.md) : fonctionnalités, règles métier et choix techniques (fait foi)
- [Guide pour les néophytes](docs/beginners-guide.md) : Kotlin, Gradle, Android et le rôle de chaque dossier
- [Architecture](docs/architecture.md) : modules, dépendances, versions et bibliothèques
- [Tests](docs/testing.md) : stratégie, outils et commandes
- [Décisions (ADR)](docs/decisions/) : décisions structurantes et leur justification, à partir du [modèle](docs/decisions/0000-template.md)
- [CLAUDE.md](CLAUDE.md) : règles, commandes et processus de contribution
