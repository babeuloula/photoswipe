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

L'APK seul se construit avec `./gradlew assembleDebug` (dans `app/build/outputs/apk/debug/`). Il s'installe par-dessus la version précédente, sans désinstaller ni perdre les décisions : les builds debug sont signés par la clé partagée `app/debug.keystore` et leur `versionCode` est le nombre de commits Git (PS-110). Depuis Android Studio, ouvrir le dossier du projet puis lancer la configuration `app`.

## Liens profonds et raccourcis

PhotoSwipe s'ouvre sur un écran précis par un lien `photoswipe://`, par exemple depuis une routine d'automatisation (Tasker, Raccourcis) ou `adb` :

| Lien | Écran ouvert |
|---|---|
| `photoswipe://today` | La pile du jour ; l'accueil s'il n'y reste rien à trier |
| `photoswipe://summary` | Le résumé du tri d'aujourd'hui |
| `photoswipe://day/2026-07-15` | La vue d'ensemble de cette date (format `AAAA-MM-JJ`) ; l'accueil si elle n'a aucun souvenir |

Un lien invalide ouvre l'accueil. Comme un lancement ordinaire, le lien passe d'abord par le tutoriel (premier lancement) et par l'accès aux photos. « Retour » ramène à l'accueil. Un lien ne fait qu'ouvrir un écran : il ne change rien et ne renvoie aucune donnée.

```bash
adb shell am start -a android.intent.action.VIEW -d "photoswipe://today"
```

L'appui long sur l'icône propose les raccourcis « Trier aujourd'hui » et « Résumé ». Décision : [ADR 0038](docs/decisions/0038-deep-links-and-shortcuts.md).

## Lancer les tests

```bash
./gradlew test                         # tous les tests unitaires, sans émulateur
./gradlew quality                      # ktlint, detekt (KDoc comprise) et Android Lint
./gradlew test quality assembleDebug   # ce que lance le job obligatoire de la CI
./gradlew connectedDebugAndroidTest    # tests instrumentés, émulateur démarré (second job de la CI)
python3 -m unittest discover -s scripts && shellcheck scripts/*.sh   # tests du script de l'émulateur
```

Stratégie, tests par module et commandes ciblées : [docs/testing.md](docs/testing.md).

## Remplir l'émulateur de souvenirs

Trois façons de tester l'application sans ses propres photos, toutes réservées au développement :

- **Données de démo** (version debug) : « Outils dev PhotoSwipe » (icône à part dans le lanceur, ou Réglages › Outils développeur) › « Données de démo ». La galerie est remplacée par des souvenirs générés, sans fichiers : idéal pour l'accueil et les compteurs.
- **Seeder** (version debug) : mêmes outils › « Médias d'exemple dans la galerie » › « Ajouter ». De vraies photos et vidéos datées sont insérées dans MediaStore (dossier « PhotoSwipe seed »), une par année sur 5 ans pour aujourd'hui et les 6 jours précédents ; « Retirer » les supprime. Elles appartiennent à l'application : Android peut ne pas afficher ses dialogues de corbeille et de favori pour elles.
- **Script adb** : `scripts/seed-emulator.sh` pousse des photos JPEG datées par EXIF dans `/sdcard/Pictures/PhotoSwipeAdb` et lance un scan ; `--remove` les retire. Ces photos n'appartiennent pas à l'application : les dialogues système s'affichent. Prérequis : `adb` dans le `PATH`, `python3` et `ffmpeg` (`brew install ffmpeg`).

Détails : [docs/testing.md](docs/testing.md#4-fixtures).

## Documentation

- [Spécification](SPEC.md) : fonctionnalités, règles métier et choix techniques (fait foi)
- [Guide pour les néophytes](docs/beginners-guide.md) : Kotlin, Gradle, Android et le rôle de chaque dossier
- [Architecture](docs/architecture.md) : modules, dépendances, versions et bibliothèques
- [Tests](docs/testing.md) : stratégie, outils et commandes
- [Couverture du tutoriel](docs/tutorial-coverage.md) : fonctionnalités livrées et pages du tutoriel
- [Décisions (ADR)](docs/decisions/) : décisions structurantes et leur justification, à partir du [modèle](docs/decisions/0000-template.md)
- [CLAUDE.md](CLAUDE.md) : règles, commandes et processus de contribution

## Licence

PhotoSwipe est distribué sous [licence Apache 2.0](LICENSE). Les bibliothèques embarquées, toutes sous licence Apache 2.0, sont listées dans l'application (Réglages › À propos › Licences).

## Nouveautés

Les nouveautés de chaque version sont dans les [notes de version](https://github.com/babeuloula/photoswipe/releases).

## Confidentialité

PhotoSwipe ne collecte aucune donnée et n'a pas accès à Internet : voir la [politique de confidentialité](PRIVACY.md).
