# Photos de test

Photos de Jérémy Reynaud (concerts, voyages, balades), fournies pour servir de fixtures aux tests de PhotoSwipe (PS-145). Elles remplacent les photos d'Unsplash de PS-143.

- Réduites à 1 280 px de côté, réencodées **sans aucune métadonnée** (ni EXIF, ni coordonnées GPS) : le dépôt est public.
- `02-la-rochelle-night-panorama.jpg` (panorama 3:1) et `04-crystal-ball-square.jpg` (carré) sont des recadrages de `08-la-rochelle-night.jpg` et de la boule de cristal.
- Les dates de prise de vue sont attribuées par `scripts/seed_photos.py` au moment de pousser les photos : l'EXIF est réécrit à chaque fois, pour qu'elles tombent toujours sur les 7 derniers jours des années passées.
- L'ordre des numéros est celui où le script les distribue : aujourd'hui reçoit un portrait, un panorama, un paysage, un carré, un portrait, puis une rafale.

Ces photos restent la propriété de leur auteur ; elles ne sont là que pour les tests du projet.
