# 0039 — Photos peut-être ratées

- **Statut** : accepté
- **Date** : 2026-11-13
- **Tickets** : PS-164

## Contexte
Une partie des souvenirs à trier sont des photos ratées : floues, ou presque noires. Les repérer aide à trier plus vite, mais une photo floue ou sombre peut être voulue (arrière-plan flou, photo de nuit). L'application reste 100 % locale et n'embarque aucun modèle.

## Décision
- **Deux mesures simples**, sur une vignette de 256 px en niveaux de gris (`ThumbnailPhotoAnalyzer`, `ContentResolver.loadThumbnail`) : la netteté est la variance du laplacien à 4 voisins, la luminosité la moyenne des niveaux de gris. Calcul pur dans `PhotoQualityRules` (`domain/service`), sans bibliothèque.
- **Seuils prudents** : très sombre sous 30 sur 255, floue sous 15 de variance (une photo nette sur une vignette de cette taille dépasse largement 100). Ce sont des valeurs de départ, à ajuster sur de vraies photos sur appareil : un seuil trop haut signalerait des photos correctes.
- **Une pastille, jamais une décision** : « Floue ? » ou « Très sombre ? » sur la carte de la pile et sur la case de la vue d'ensemble, lue par TalkBack (« Cette photo est peut-être floue »). Aucun marquage automatique.
- **Mesurée une fois** : la mesure est gardée dans Room (table `photo_measure`, base en version 6, migration automatique 5 → 6), oubliée avec les orphelins (`CleanOrphanEntries`). Seules les deux mesures sont gardées, jamais l'image.
- **Après l'affichage** : la pile et la vue d'ensemble s'affichent d'abord, puis mesurent les photos qui ne l'ont jamais été et ajoutent les pastilles.
- **Photos seulement**, pas les vidéos. Une vignette illisible ne donne rien.
- **Réglage** « Signaler les photos ratées » dans « Aide au tri », actif par défaut.

## Conséquences
- La première ouverture d'un jour lit la vignette de chacune de ses photos : quelques millisecondes par photo, une seule fois.
- Le doigt devant l'objectif n'est pas détecté (trop peu fiable sans modèle).
- La meilleure prise d'un groupe de photos proches (idée n° 70, reportée) pourra s'appuyer sur la netteté gardée.
