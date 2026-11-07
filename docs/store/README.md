# Publication sur le Play Store

Préparation de la publication (PS-72, SPEC §13).

Les textes à recopier dans la Play Console vivent dans Notion, page [Publication Play Store](https://app.notion.com/p/410f693cd865816bb181c61e401762d7) (sous PhotoSwipe) :

- **Politique de confidentialité** : la page à publier sur le site de Jérémy, dont l'URL publique se renseigne dans la Play Console et dans la fiche ; sa version de référence, en français et en anglais, est [`PRIVACY.md`](../../PRIVACY.md) à la racine du dépôt (PS-127), à tenir à jour à chaque nouvelle autorisation ou donnée enregistrée ;
- **Déclaration d'usage des autorisations photos et vidéos** ;
- **Sécurité des données** ;
- **Fiche du Play Store** (FR et EN).

Restent dans le dépôt :

| Fichier | Contenu |
|---|---|
| [screenshots/](screenshots/) | Captures de la fiche, mises en scène (`fr/`, `en/`) et brutes (`raw/`) |
| [credits.md](credits.md) | Photos Unsplash des captures et leurs auteurs |

## Version

- `versionName` vient du dernier tag Git `vX.Y.Z` (`AppVersion` de `build-logic`) : `1.2.0` sur le commit du tag, `1.2.0-dev.5` cinq commits plus loin, `0.0.0-dev` sans tag.
- `versionCode` est le nombre de commits (PS-110) : toujours croissant, donc accepté par le Play Store à chaque nouvelle version.
- Publier une version : `git tag v1.0.0 && git push origin v1.0.0`. Le workflow `Release` (voir plus bas) construit et publie le reste.

## Build release signé (AAB)

1. La clé d'upload vit **hors du dépôt** : `~/.android/photoswipe-upload.jks` (PKCS12, alias `upload`, RSA 4096, 10 000 jours). Elle a été créée le 2026-11-06.
2. `keystore.properties`, à la racine du dépôt et **ignoré par Git**, donne son chemin et ses mots de passe :
   ```properties
   storeFile=/Users/<vous>/.android/photoswipe-upload.jks
   storePassword=…
   keyAlias=upload
   keyPassword=…
   ```
3. `./gradlew :app:bundleRelease` produit `app/build/outputs/bundle/release/app-release.aab`, signé par cette clé. Sans `keystore.properties`, l'AAB n'est pas signé.
4. Sauvegarder la clé et son mot de passe dans un gestionnaire de mots de passe : avec la signature d'application par Google Play, une clé d'upload perdue se remplace, mais il faut passer par le support Google.

## Release par tag (CI)

Pousser un tag `vX.Y.Z` lance le workflow `.github/workflows/release.yml` (PS-115) :

1. checkout de tout l'historique et des tags (`versionCode` = nombre de commits, `versionName` = version du tag) ;
2. clé d'upload reconstituée depuis les secrets dans le dossier temporaire du runner, et `keystore.properties` qui la désigne ;
3. `./gradlew test quality bundleRelease assembleRelease`, puis vérification des signatures (`jarsigner`, `apksigner`) ;
4. version GitHub du tag créée (notes générées depuis les PR) avec `photoswipe-vX.Y.Z.aab` (pour le Play Store), `photoswipe-vX.Y.Z.apk` (installation directe) et `version.properties` (`versionCode` et `versionName` lus dans l'APK par `aapt2`, pour F-Droid) ; si elle existe déjà, ses fichiers sont remplacés ;
5. clé et `keystore.properties` effacés, même en cas d'échec.

Secrets du dépôt (Settings → Secrets and variables → Actions) :

| Secret | Valeur |
|---|---|
| `PHOTOSWIPE_UPLOAD_KEYSTORE_BASE64` | `base64 -i ~/.android/photoswipe-upload.jks` |
| `PHOTOSWIPE_UPLOAD_KEYSTORE_PASSWORD` | `storePassword` de `keystore.properties` |
| `PHOTOSWIPE_UPLOAD_KEY_PASSWORD` | `keyPassword` de `keystore.properties` |

Sans ces secrets, le workflow s'arrête avant le build : aucune release non signée n'est publiée.

Sans la CI (crédits GitHub Actions épuisés), joindre ces trois fichiers à la main : `./gradlew :app:bundleRelease :app:assembleRelease` sur le commit du tag, `version.properties` écrit comme dans le workflow, puis `gh release upload vX.Y.Z …`.

## F-Droid (PS-128)

F-Droid compile l'application depuis les sources et la signe avec sa propre clé : on ne passe pas d'une installation Play Store à une installation F-Droid sans désinstaller.

- **Fiche** : `fastlane/metadata/android/en-US/` et `fr-FR/` (titre, descriptions courte et complète, icône 512 × 512, captures de `screenshots/`), lus par F-Droid dans le dépôt. Les textes reprennent la fiche du Play Store : la modifier ici aussi. L'icône est le rendu de l'icône adaptative (`ic_launcher_foreground` sur `launcher_background`), recadré sur ses 72 dp visibles.
- **Recette** : `metadata/fr.babeuloula.photoswipe.yml` dans [fdroiddata](https://gitlab.com/fdroid/fdroiddata), avec `subdir: app`, `gradle: [yes]` (build `release` non signé, faute de `keystore.properties`) et le JDK 17 installé par `sudo` (toolchain JVM 17 du projet).
- **Mises à jour automatiques** : le `versionCode` (nombre de commits) ne se lit pas dans le code, donc `UpdateCheckMode: HTTP` lit `https://github.com/babeuloula/photoswipe/releases/latest/download/version.properties`, et `AutoUpdateMode: Version v%v` compile le tag. Chaque release GitHub doit donc porter `version.properties`. F-Droid clone tout l'historique : il retrouve le même `versionCode` au même tag.
- **Bloc des dépendances** : `dependenciesInfo` désactivé dans `app/build.gradle.kts`, F-Droid refusant ce bloc chiffré pour Google dans la signature de l'APK.
- **Notes de version** (facultatif) : `fastlane/metadata/android/<langue>/changelogs/<versionCode>.txt`, 500 caractères au plus.

## Autorisations à déclarer dans la Play Console
- `SCHEDULE_EXACT_ALARM` (PS-125, ADR 0031) : le rappel quotidien et sa reprogrammation arrivent à l'heure choisie par l'utilisateur. Déclarer cet usage (« rappel choisi par l'utilisateur ») dans le formulaire des autorisations ; l'autorisation est demandée à l'utilisateur dans « Alarmes et rappels », pas `USE_EXACT_ALARM`.

## Captures d'écran

Prises sur l'émulateur `Medium_Phone_API_37.0` (1080 × 2400) avec des photos Unsplash datées par leur EXIF d'un 6 novembre de plusieurs années, barre d'état en mode démo (09:41, batterie pleine). Les visuels mis en scène (1080 × 1920) ajoutent un titre, le téléphone et les crédits des photos visibles.
