# 0040 — Photos animées

- **Statut** : accepté
- **Date** : 2026-11-13
- **Tickets** : PS-167

## Contexte
Les « Motion Photos » de Google et de Samsung sont un JPEG suivi d'une courte vidéo MP4. MediaStore les range parmi les photos (`image/jpeg`) et l'application n'en montrait que l'image fixe. On veut pouvoir revoir le mouvement avant de décider, depuis la carte, sans gêner le maintien du doigt (comparaison dans un groupe, PS-160), ni ajouter de permission ou de bibliothèque.

## Ce que sait Media3 (1.11.1, version du catalogue)
Vérifié dans `media3-extractor-1.11.1` :
- `DefaultExtractorsFactory` contient `JpegExtractor`, qui, sans `FLAG_READ_IMAGE`, passe la main à `JpegMotionPhotoExtractor` : il lit le XMP du JPEG (`XmpMotionPhotoDescriptionParser` : `Camera:MotionPhoto`, `GCamera:MotionPhoto`, `Camera:MicroVideo`, `GCamera:MicroVideo`, `GCamera:MicroVideoOffset`, conteneurs `Container:Directory` et `GContainer:Directory`) et lit la vidéo MP4 placée après la photo. ExoPlayer sait donc lire une photo animée **de Google** en lui donnant directement l'URI du JPEG (à condition que le reniflage de l'extracteur choisisse le JPEG, l'URI `content://` n'ayant pas d'extension).
- Media3 ne lit **pas** le trailer `SEFT` de Samsung (bloc `MotionPhoto_Data` décrit par le répertoire `SEFH` en fin de fichier) : aucun extracteur ne le connaît. Les appareils Samsung récents écrivent aussi le XMP de Google, les plus anciens seulement le trailer.
- `HeicMotionPhotoExtractor` existe aussi (photos animées HEIC), hors du périmètre de ce ticket.

## Décision
- **Reconnaissance par les métadonnées**, jamais par le nom : `MotionPhotoDetector` (`domain/service`, Kotlin pur) lit le début du fichier (128 Kio, où se trouve le XMP) et sa fin (trailer Samsung), puis l'en-tête de la vidéo annoncée. Il retient la vidéo seulement si elle tient dans le fichier et commence par une boîte `ftyp` : sinon la photo reste une photo ordinaire, sans erreur.
- **Une seule façon de lire, pour les deux formats** : le détecteur connaît la plage d'octets de la vidéo (fin du fichier moins `MicroVideoOffset`, longueurs du conteneur, ou bloc `MotionPhoto_Data`). Le lecteur de la pile reçoit une URI à part (`content://…#photoswipe-motion=début-longueur`, `MotionVideoRange`) que `MotionPhotoDataSource` (source Media3 posée devant `DefaultDataSource`) ramène à cette plage du fichier de la photo ; Media3 la lit comme un MP4 ordinaire (`Mp4Extractor`). On n'utilise donc pas `JpegMotionPhotoExtractor` : le format Samsung l'exige de toute façon, et on ne dépend pas du reniflage d'une URI sans extension. Aucune copie sur disque, aucune bibliothèque ajoutée.
- **Lecture du fichier** par le `ContentResolver` (`ContentMotionPhotoReader`, `:data`, port `MotionPhotoReader`), hors du thread principal ; le résultat est gardé **en mémoire** pour la durée du process, par média et par taille (une photo retouchée est relue). Pas de table Room : la lecture coûte quelques Kio par photo.
- **Après l'affichage**, comme les photos peut-être ratées (ADR 0039) : la pile et la vue d'ensemble s'affichent, puis ajoutent les pastilles (`CardHints`).
- **Un bouton, pas de lecture automatique** : pastille « Photo animée » sur la carte et sur la case de la vue d'ensemble ; sur la carte du dessus seulement, un bouton (« Lire la photo animée ») joue l'animation une fois sur le lecteur de la pile (`CardVideoPlayer`), par-dessus l'image, puis l'image fixe revient ; pendant la lecture, il l'arrête. Le son suit le dernier réglage choisi pour les vidéos (coupé au départ). Le maintien du doigt garde la comparaison (PS-160).
- **Lecture impossible** : le lecteur arrête la lecture (`playingUri` remis à `null`) et l'image reste, sans message.

## Conséquences
- La première ouverture d'un jour lit le début et la fin de chacune de ses photos, une fois par process.
- Le format Samsung est écrit d'après sa description publique (ExifTool) et vérifié sur des octets fabriqués : à confirmer sur une vraie photo d'un téléphone Samsung.
- Les photos animées HEIC ne sont pas reconnues.
