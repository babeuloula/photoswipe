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
- Publier une version : `git tag v1.0.0 && git push origin v1.0.0`, puis construire l'AAB depuis ce commit.

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

## Captures d'écran

Prises sur l'émulateur `Medium_Phone_API_37.0` (1080 × 2400) avec des photos Unsplash datées par leur EXIF d'un 6 novembre de plusieurs années, barre d'état en mode démo (09:41, batterie pleine). Les visuels mis en scène (1080 × 1920) ajoutent un titre, le téléphone et les crédits des photos visibles.
