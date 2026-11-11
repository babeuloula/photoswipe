# Vidéo « Un jour, un rituel » (storyline A)

La vidéo retenue parmi les trois histoires de [`../STORYLINES.md`](../STORYLINES.md) : une seule prise continue
de l'émulateur, jouée **en temps réel, sans accéléré ni coupe**. La notification du rappel arrive, on
l'ouvre et on trie les 8 souvenirs du jour (garder, écarter le doublon, mettre la balade en favori…),
puis le résumé et le dialogue de la corbeille ; la caméra s'approche et recule autour du téléphone.

| Dossier | Langue | Durée |
|---|---|---|
| [`a/`](a/) | français | 41 s |
| [`a-en/`](a-en/) | anglais | 42 s |

Chaque dossier est un projet HyperFrames : `index.html` (généré), la prise `assets/footage/take.mp4`
et ses instants `timings.json`. Musique : bibliothèque musicale HeyGen ; bruitages : bibliothèque de
`/media-use` (Pixabay), un souffle par glissement, aucun son sur les appuis (un clic sonne « souris »).

```bash
cd docs/store/video/storylines
python3 build.py                     # régénère a/index.html et a-en/index.html
cd a && npx hyperframes check && npx hyperframes render -q delivery -o ../../renders/storylines/A-fr.mp4
cd ../a-en && npx hyperframes render -q delivery -o ../../renders/storylines/A-en.mp4
```

## Refaire une prise

Les photos du tournage sont celles de Jérémy (hors Git) ; [`tools/`](tools/) contient le reste.

1. `python3 tools/seed_photos.py DOSSIER_DES_PHOTOS` : 16 photos datées par EXIF autour
   d'aujourd'hui dans `DCIM/Camera` (8 souvenirs du jour de 2017 à 2025, quelques jours à rattraper).
2. Sauvegarder l'état de l'app (`run-as fr.babeuloula.photoswipe` : `databases/`, `files/datastore/`)
   pour le remettre après : la prise enregistre des décisions et touche aux réglages du rappel.
3. Anglais seulement : `adb shell cmd locale set-device-locale en-US` (puis `fr-FR` à la fin), et
   attendre que l'émulateur soit au repos (`adb shell cat /proc/loadavg` sous 1,5).
4. Dans les réglages de l'app : activer le rappel (accepter les notifications), puis
   `python3 tools/reminder_time.py HH:MM` (`--12h` en anglais) sur un multiple de 5 minutes à venir ;
   `adb shell appops set fr.babeuloula.photoswipe SCHEDULE_EXACT_ALARM allow` pour l'heure pile.
5. Vider le volet des notifications et fermer Google Photos (`am force-stop com.google.android.apps.photos`),
   qui charge l'émulateur après l'ajout des photos.
6. `python3 tools/take.py journal.json HH:MM` (`--no-cancel` en anglais : on ferme le dialogue à la
   main), puis `adb pull /sdcard/take.mp4`.
7. `python3 tools/frames.py smooth take.mp4 journal.json` : moins de 10 images par glissement, la
   prise saccade, on la refait. Puis passer la prise à 30 i/s et tenir la dernière image (screenrecord
   n'enregistre rien quand l'écran est figé, sur le dialogue) :
   `ffmpeg -i take.mp4 -vf "fps=30,tpad=stop_mode=clone:stop_duration=3.5" -c:v libx264 -crf 14 -pix_fmt yuv420p -an a/assets/footage/take.mp4`.
8. `python3 tools/frames.py motion a/assets/footage/take.mp4` donne les instants où l'image bouge :
   reporter dans `timings.json` chaque geste du journal (+ ~0,05 s), l'arrivée de la notification,
   du résumé et du dialogue, et la fin de la prise.
9. Remettre l'app comme avant : rappel désactivé, base et réglages sauvegardés recopiés,
   `pm revoke … POST_NOTIFICATIONS`, `appops set … SCHEDULE_EXACT_ALARM default`, favori de la photo
   de 2024 retiré dans la galerie.
