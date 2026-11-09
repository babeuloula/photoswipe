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
| [feature-graphic/](feature-graphic/) | Image de présentation 1024 × 500 (FR, EN) et sa source HTML |
| [credits.md](credits.md) | Photos Unsplash des captures et leurs auteurs |

## Version

- `versionName` vient du dernier tag Git `vX.Y.Z` (`AppVersion` de `build-logic`) : `1.2.0` sur le commit du tag, `1.2.0-dev.5` cinq commits plus loin, `0.0.0-dev` sans tag.
- `versionCode` est le nombre de commits (PS-110) : toujours croissant, donc accepté par le Play Store à chaque nouvelle version.
- Publier une version : Actions → Release → Run workflow avec `vX.Y.Z` (voir plus bas). Un tag poussé à la main ne peut être qu'une pré-version de debug `vX.Y.Z-dev.N`.

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

## Release par workflow (CI)

Une release officielle se publie **uniquement** par le workflow `.github/workflows/release.yml` lancé à la main (PS-130, ADR [0032](../decisions/0032-release-workflow.md)) : Actions → Release → Run workflow, sur `main`, avec la version `vX.Y.Z`.

1. la version doit avoir la forme `vX.Y.Z` et ne pas exister (ni tag, ni branche `release/vX.Y.Z`), et le run « CI » du commit de `main` doit être terminé et vert (PS-133) : tests et qualité ne sont pas relancés, le commit de release n'ajoutant qu'un fichier texte ;
2. branche `release/vX.Y.Z` depuis `main`, avec un commit des notes de version F-Droid (`scripts/fdroid-changelog.sh`, depuis le dernier tag officiel) ;
3. clé d'upload reconstituée depuis les secrets dans le dossier temporaire du runner, et `keystore.properties` qui la désigne ; tag `vX.Y.Z` posé en local sur ce commit (`versionName` = `X.Y.Z`, `versionCode` = nombre de commits) ;
4. `./gradlew bundleRelease assembleRelease`, puis vérification des signatures (`jarsigner`, `apksigner`) ;
5. seulement si tout est passé : branche poussée, PR « chore(release): vX.Y.Z » vers `main`, tag poussé, release GitHub créée (notes générées depuis le dernier tag officiel, marquée « latest ») avec `photoswipe-vX.Y.Z.aab` (Play Store), `photoswipe-vX.Y.Z.apk` (installation directe) et `version.properties` (`versionCode` et `versionName` lus dans l'APK par `aapt2`, pour F-Droid) ;
6. clé et `keystore.properties` effacés, même en cas d'échec.

La PR est ouverte avec le jeton `PHOTOSWIPE_RELEASE_TOKEN` : la vraie CI y tourne, et l'**auto-merge** la merge toute seule en **merge commit** une fois verte. Rien à faire après avoir lancé le workflow. Le merge commit garde le commit tagué dans l'historique de `main`, pour que les builds suivants s'appellent `X.Y.Z-dev.N`. Sans le jeton, la PR est ouverte par le workflow, ne lance pas la CI et se merge à la main en merge commit (contournement admin). Sans merge, la release reste valable (Play Store, F-Droid) ; seuls les noms des builds debug suivants restent sur l'ancienne version.

Créer le jeton (PS-133) : GitHub → Settings (du compte) → Developer settings → Personal access tokens → Fine-grained tokens → Generate new token ; « Repository access » : seulement `babeuloula/photoswipe` ; permissions du dépôt : **Contents** et **Pull requests** en « Read and write » ; durée au choix (le renouveler avant son expiration). Puis, dans le dépôt : Settings → Secrets and variables → Actions → New repository secret `PHOTOSWIPE_RELEASE_TOKEN`.

Réglages du dépôt requis : « Allow auto-merge » (Settings → General) et, pour le cas sans jeton, « Allow GitHub Actions to create and approve pull requests » (Settings → Actions → General).

Les branches `release/v*` ne peuvent pas être supprimées, admins compris (ruleset « release branches: no deletion », Settings → Rules → Rulesets) : elles gardent la trace de chaque release. Après le merge de la PR, GitHub ne supprime donc pas la branche malgré « Automatically delete head branches ».

**Tag ou release créé à la main** (interface de GitHub ou `git push` d'un tag) : seul un tag de pré-version `vX.Y.Z-dev.N` est accepté, par exemple `v1.2.0-dev.3` (le nom affiché par l'application, préfixé de `v`). Le workflow construit alors la version **debug** (outils développeur compris, clé de debug partagée) et joint seulement `photoswipe-vX.Y.Z-dev.N-debug.apk` à une **pré-version**, jamais « latest » : F-Droid continue de lire la dernière release officielle. Tout autre tag poussé à la main (`v1.2.0`, `v1.2`…) est supprimé avec sa release, et le workflow échoue. Les tags `-dev` ne comptent pas dans le nom de version (`git describe --exclude "v*-dev*"`).

Secrets du dépôt (Settings → Secrets and variables → Actions) :

| Secret | Valeur |
|---|---|
| `PHOTOSWIPE_UPLOAD_KEYSTORE_BASE64` | `base64 -i ~/.android/photoswipe-upload.jks` |
| `PHOTOSWIPE_UPLOAD_KEYSTORE_PASSWORD` | `storePassword` de `keystore.properties` |
| `PHOTOSWIPE_UPLOAD_KEY_PASSWORD` | `keyPassword` de `keystore.properties` |
| `PHOTOSWIPE_RELEASE_TOKEN` | jeton personnel à grain fin (voir plus haut) : ouvre la PR de release et active son auto-merge |

Sans ces secrets, le workflow s'arrête avant le build : aucune release non signée n'est publiée.

**Seulement si les jobs GitHub Actions ne démarrent pas** (crédits épuisés : `gh run list` montre des échecs en quelques secondes ; sinon le contrôle des tags supprimerait le tag), faire à la main ce que fait le workflow : notes F-Droid (`scripts/fdroid-changelog.sh vX.Y.Z`) commitées par une PR sur `main`, tag `vX.Y.Z` sur ce commit, `./gradlew :app:bundleRelease :app:assembleRelease` sur ce commit, `version.properties` écrit comme dans le workflow, puis `gh release create vX.Y.Z …` avec les trois fichiers.

## F-Droid (PS-128)

F-Droid compile l'application depuis les sources et la signe avec sa propre clé : on ne passe pas d'une installation Play Store à une installation F-Droid sans désinstaller.

- **Fiche** : `fastlane/metadata/android/en-US/` et `fr-FR/` (titre, descriptions courte et complète, icône 512 × 512, captures de `screenshots/`), lus par F-Droid dans le dépôt. Les textes reprennent la fiche du Play Store : la modifier ici aussi. L'icône est le rendu de l'icône adaptative (`ic_launcher_foreground` sur `launcher_background`), recadré sur ses 72 dp visibles.
- **Recette** : `metadata/fr.babeuloula.photoswipe.yml` dans [fdroiddata](https://gitlab.com/fdroid/fdroiddata), avec `subdir: app`, `gradle: [yes]` (build `release` non signé, faute de `keystore.properties`) et le JDK 17 installé par `sudo` (toolchain JVM 17 du projet).
- **Mises à jour automatiques** : le `versionCode` (nombre de commits) ne se lit pas dans le code, donc `UpdateCheckMode: HTTP` lit `https://github.com/babeuloula/photoswipe/releases/latest/download/version.properties`, et `AutoUpdateMode: Version v%v` compile le tag. Chaque release GitHub doit donc porter `version.properties`. F-Droid clone tout l'historique : il retrouve le même `versionCode` au même tag.
- **Bloc des dépendances** : `dependenciesInfo` désactivé dans `app/build.gradle.kts`, F-Droid refusant ce bloc chiffré pour Google dans la signature de l'APK.
- **Notes de version** (PS-129) : `fastlane/metadata/android/en-US/changelogs/default.txt`, 500 caractères au plus, que F-Droid lit dans le commit qu'il compile (les autres langues reprennent l'anglais). Le workflow `Release` l'écrit dans le commit de la release (`scripts/fdroid-changelog.sh vX.Y.Z` : notes que GitHub génère depuis le dernier tag officiel, seulement les PR `feat` et `fix`, sans préfixe ni ticket).

## Autorisations à déclarer dans la Play Console
- `SCHEDULE_EXACT_ALARM` (PS-125, ADR 0031) : le rappel quotidien et sa reprogrammation arrivent à l'heure choisie par l'utilisateur. Déclarer cet usage (« rappel choisi par l'utilisateur ») dans le formulaire des autorisations ; l'autorisation est demandée à l'utilisateur dans « Alarmes et rappels », pas `USE_EXACT_ALARM`.

## Captures d'écran

Prises sur l'émulateur `Medium_Phone_API_37.0` (1080 × 2400) avec des photos Unsplash datées par leur EXIF d'un 6 novembre de plusieurs années, barre d'état en mode démo (09:41, batterie pleine). Les visuels mis en scène (1080 × 1920) ajoutent un titre, le téléphone et les crédits des photos visibles.

## Image de présentation

[`feature-graphic/`](feature-graphic/) : l'image de présentation de la fiche Play Store (1024 × 500, JPEG sans transparence), en français (`fr-feature-graphic.jpg`) et en anglais (`en-feature-graphic.jpg`). Elle reprend le fond, les polices et les captures brutes (accueil et pile) des visuels mis en scène. Source : `feature-graphic.html` ; la régénérer après un changement des captures ou du texte :

```bash
cd docs/store/feature-graphic
for l in fr en; do
  "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" --headless --hide-scrollbars --allow-file-access-from-files \
    --force-device-scale-factor=1 --window-size=1024,500 --virtual-time-budget=3000 \
    --screenshot="$PWD/$l.png" "file://$PWD/feature-graphic.html?lang=$l"
  sips -s format jpeg -s formatOptions 92 "$l.png" --out "$l-feature-graphic.jpg" && rm -f "$l.png"
done
```
