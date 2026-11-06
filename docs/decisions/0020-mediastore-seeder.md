# 0020 — Seeder MediaStore dans la variante debug de `:data`

- **Statut** : accepté
- **Date** : 2026-11-06
- **Ticket** : PS-69

## Contexte
La SPEC §10 (niveau 3) demande, en debug seulement, un seeder qui insère de vraies photos et vidéos datées dans MediaStore, pour tester le vrai chemin de requête sur émulateur. MediaStore recalcule `DATE_TAKEN` depuis le fichier quand il l'indexe : la valeur passée à l'insertion ne suffit pas. Les médias insérés par l'app lui appartiennent, ce qui peut faire sauter les dialogues système de corbeille et de favori.

## Décision
- **Code dans `data/src/debug`** (package `fr.babeuloula.photoswipe.data.seed`) : `MediaStoreSeeder` parle à MediaStore comme `MediaStoreMediaSource`, dans le module qui connaît MediaStore. Le jeu source `debug` d'une bibliothèque n'entre que dans l'APK debug ; `MediaSourceBindingTest` vérifie son absence en release. `:app` le relie aux outils développeur par l'interface `GallerySeeder`, liée dans `di/DeveloperToolsModule` (règle de l'ADR 0005 : `:app` ne touche à `:data` que depuis `di`).
- **Plan pur** : `SeedCatalog` (une photo par année sur 5 ans pour aujourd'hui et les 6 jours précédents, une vidéo par jour), testé sur la JVM.
- **Dates dans les fichiers** : photos JPEG générées et datées par leur EXIF (`DateTimeOriginal` et décalage) ; vidéos copiées depuis `data/src/debug/res/raw/seed_video.mp4` (3 s, 80 Ko, générée par `ffmpeg -f lavfi -i testsrc2`, sans droits tiers) dont `Mp4CreationTime` réécrit la date de création du `mvhd`.
- **Retrait** : tous les médias vont dans `Pictures/PhotoSwipe seed/` et `Movies/PhotoSwipe seed/` ; « Retirer » supprime ce dossier, corbeille comprise, et laisse en place un média qui n'appartient plus à l'app.
- **Tests** : instrumentés sur le vrai MediaStore (dates relues par `MediaStoreMediaSource`, retrait), en plus des tests JVM du plan et de l'écriture MP4.

## Alternatives envisagées
- **Encoder les vidéos à la volée** (`MediaCodec` + `MediaMuxer`) : bien plus de code, et `MediaMuxer` n'écrit pas de date de création.
- **Seeder dans `:app`** : il dupliquerait l'accès à MediaStore hors de `:data`.
- **Seeder seulement par `adb`** : ne teste pas l'insertion par l'app ; il reste prévu en complément (PS-70) pour les dialogues système.
