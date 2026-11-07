# Publication sur le Play Store

Préparation de la publication (PS-72, SPEC §13).

Les textes à recopier dans la Play Console vivent dans Notion, page [Publication Play Store](https://app.notion.com/p/410f693cd865816bb181c61e401762d7) (sous PhotoSwipe) :

- **Politique de confidentialité** : la page à publier sur le site de Jérémy, dont l'URL publique se renseigne dans la Play Console et dans la fiche ;
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
4. version GitHub du tag créée (notes générées depuis les PR) avec `photoswipe-vX.Y.Z.aab` (pour le Play Store) et `photoswipe-vX.Y.Z.apk` (installation directe) ; si elle existe déjà, ses fichiers sont remplacés ;
5. clé et `keystore.properties` effacés, même en cas d'échec.

Secrets du dépôt (Settings → Secrets and variables → Actions) :

| Secret | Valeur |
|---|---|
| `PHOTOSWIPE_UPLOAD_KEYSTORE_BASE64` | `base64 -i ~/.android/photoswipe-upload.jks` |
| `PHOTOSWIPE_UPLOAD_KEYSTORE_PASSWORD` | `storePassword` de `keystore.properties` |
| `PHOTOSWIPE_UPLOAD_KEY_PASSWORD` | `keyPassword` de `keystore.properties` |

Sans ces secrets, le workflow s'arrête avant le build : aucune release non signée n'est publiée.

## Autorisations à déclarer dans la Play Console
- `SCHEDULE_EXACT_ALARM` (PS-125, ADR 0031) : le rappel quotidien et sa reprogrammation arrivent à l'heure choisie par l'utilisateur. Déclarer cet usage (« rappel choisi par l'utilisateur ») dans le formulaire des autorisations ; l'autorisation est demandée à l'utilisateur dans « Alarmes et rappels », pas `USE_EXACT_ALARM`.

## Captures d'écran

Prises sur l'émulateur `Medium_Phone_API_37.0` (1080 × 2400) avec des photos Unsplash datées par leur EXIF d'un 6 novembre de plusieurs années, barre d'état en mode démo (09:41, batterie pleine). Les visuels mis en scène (1080 × 1920) ajoutent un titre, le téléphone et les crédits des photos visibles.
