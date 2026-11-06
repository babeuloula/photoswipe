# 0025 — Refonte visuelle « planche-contact »

- **Statut** : accepté
- **Date** : 2026-11-06
- **Ticket** : PS-89 (epic 12, PS-89 à PS-101)

## Contexte
Le thème Material 3 de base (graine bleue #769CDF, typographie et formes par défaut) était correct mais générique. Jérémy a fait dessiner une refonte dans Claude Design ([maquettes](https://claude.ai/artifact/C3WwuLrAdQtd5vemmqyBLP)), en restant dans Material 3 : la planche-contact du labo photo, gris papier en clair, noir de labo en sombre, ambre de la lampe inactinique en accent, et un repère de bord de film « Il y a 3 ans · 6 nov. 2023 » comme signature.

## Décision
- **Palette de repli** générée avec material-color-utilities (schéma Tonal spot) depuis la graine **#8A6A1E** ; les neutres du schéma sont remplacés par un gris papier photo (teinte 85, chroma 3 ; chroma 5 pour les variantes), ceux des maquettes. Tous les rôles sont écrits dans `Color.kt`, rôles fixes compris.
- **Toujours l'ambre** (PS-102, décision de Jérémy du 2026-11-06, SPEC §8 mise à jour) : c'est la direction artistique ; les couleurs dynamiques (Material You) ne sont plus utilisées, sans réglage pour y revenir. Seule l'icône à thème du lanceur suit encore le fond d'écran, si l'utilisateur l'a activée dans Android.
- **Garder et favori** : #1E8A4A et #2F6FD0 harmonisés vers la graine (`Blend.harmonize`), écrits en dur dans `SwipeColors` avec leurs conteneurs (tons 40 / 100 / 90 / 10 en clair, 80 / 20 / 30 / 90 en sombre) : appeler `MaterialColors.harmonize` à l'exécution ferait entrer la bibliothèque Material Components (Views). Supprimer reste le rôle `error` et son conteneur.
- **Rôles fixes** pour ce qui est posé sur les photos (repère, pastilles, boutons de la vidéo) : `primaryFixed`, `primaryFixedDim`, `onPrimaryFixed` de `ColorScheme` (Material 3 1.4), identiques en clair et en sombre.
- **Mouvement** : la carte sort sans rebond (`fastExit`, amortissement 1, raideur 1 400, vitesse du doigt reprise) ; la carte suivante remonte avec le ressort rapide du schéma standard (`fastSpatialStandard`, 0,9 / 1 400). Le retour au centre garde son léger rebond.
- **Typographie** (PS-90) : l'échelle Material 3 inchangée, en Atkinson Hyperlegible Next (police variable, graisses 400 / 500 / 600), sauf les styles display en IBM Plex Mono 400 pour les grands nombres ; polices embarquées dans `res/font` (pas de polices téléchargeables : aucune connexion), sous SIL OFL 1.1, citées dans l'écran des licences.
- **Icônes** (PS-91) : Material Symbols Rounded seulement, contour (remplissage 0) sauf les icônes pleines (`_filled` : cœur, lecture, pause, coche ronde), en vecteurs `res/drawable/ic_*.xml` tirés de `google/material-design-icons` ; `material-icons-core` retiré.
- **Repère de bord de film** (PS-92) : `FilmEdgeLabel` (32 dp, coins de 12 dp, coin haut de fin de ligne coupé à 10 dp par `FilmEdgeShape`, texte `primaryFixedDim` sur `onPrimaryFixed` en Plex Mono 500) et `FilmEdgeChip` (même conteneur, sans coupe, texte `primaryFixed`) ; la date du repère est abrégée (`FormatStyle.MEDIUM`).
- **Pile** (PS-93) : barre de progression « 4 sur 23 » (décisions de la session + médias restants) ; boutons libellés, celui du côté glissé se remplit et s'arrondit (rayon animé, faute de `ButtonGroup` stable) ; voile avec pastille et libellé ; carte suivante qui remonte avec le geste ; la flèche de retour est gardée (les maquettes l'omettaient) ; la pile vidée propose « Voir le résumé » et « Retour à l'accueil ».
- **Accueil** (PS-95) : carte du jour dominante (bande de vignettes par année, nombre en chasse fixe), bannière et conseil calmes (surface et contour), jours à rattraper en liste segmentée (16 dp aux extrémités, 4 dp entre deux) ; sans souvenir sur la semaine, la liste grisée reste affichée sous la carte de texte.
- **Résumé** (PS-96) : espace libéré en grand, sans carte, et ce dont il dépend ; vignettes des cinq derniers marqués (`GetMarkedPreviews`, aperçu facultatif : son échec n'empêche pas le résumé) ; « Confirmer la suppression » en `primary` (une seule couleur d'accent par écran) ; dialogue de corbeille avec icône.
- **Réglages** (PS-97) : sections en listes segmentées ; le thème se choisit par boutons radio dans la liste, sans dialogue (une étape de moins, comme dans les maquettes) ; l'heure du rappel garde le `TimePicker` Material 3.
- **Accès aux photos** (PS-98) : pile de tirages dessinée avec les conteneurs du thème et une icône (aucune photo), repère d'exemple daté d'il y a trois ans ; tirages vides au refus.
- **Notification** (PS-99) : titre « … à trier aujourd'hui », texte avec le jour et les années, vignette d'un souvenir en grande icône, couleur ambre fixe (`notification_accent`, le primary de repli : Android colore la notification hors de Compose).
- **Icône et démarrage** (PS-100) : icône adaptative en vecteurs (tracé de la grille 64 des maquettes, tenu dans la zone sûre), couche monochrome, même tracé pour la notification ; écran de démarrage de la plateforme (`windowSplashScreen*`, API 31+), sans bibliothèque `core-splashscreen` puisque `minSdk` vaut 33.
- **Tablette** (PS-101) : accueil en liste et détail (planche-contact du jour à droite, une vignette par année faute de lire tous les souvenirs du jour) ; la colonne « À suivre » de la pile n'est pas reprise (la pile ne prépare que deux cartes, SPEC §14).
- **Formes** : l'échelle Material 3 par défaut, que les maquettes reprennent.
- **Le seuil du geste reste celui de la SPEC** (35 % de la largeur) ; les maquettes parlaient de 120 dp.
- **Les photos des maquettes n'entrent pas dans l'APK** : les illustrations sont faites de formes du thème.
- La refonte est découpée en tickets PS-89 à PS-102, une PR chacun.

## Alternatives envisagées
- **Garder Material You**, avec l'ambre en repli (PS-89) : l'ambre ne se voyait sur aucun téléphone récent.
- **Un réglage « Couleurs du fond d'écran »** : écarté par Jérémy, l'ambre est l'identité de l'app.
- **Garder les neutres du schéma Tonal spot** : plus jaunes que le gris papier des maquettes.
