# SPEC : « Un 9 novembre » (storyline A, tournée le 9 novembre 2026 ; ordre final : garder, favori, supprimer, voir index.html)

GOAL:    à la fin, on a compris qu'un petit rituel quotidien fait revivre ses souvenirs et trier sans rien perdre, et on veut installer l'app.
OUTPUT:  HyperFrames, 1920 × 1080, 30 i/s ; versions FR et EN par variables (`lang`).
TIME:    30 s × 30 = 900 images ; tempo calé sur la musique choisie (à défaut 96 BPM, 18,75 images par temps).

LOOK:
- Sol : dégradé ambre de la fiche Play Store, 160°, #F5E3BE → #EDD29A (55 %) → #E2BC72, sans grain.
- Encre : #2A1E08 ; accent : #F2C96B sur #2A1E08, seulement dans le repère de bord de film et le mot mis en avant d'un titre.
- Titres : Atkinson Hyperlegible Next 600, 84 px (7,8 % de la hauteur), interlettrage −1 %, au plus 2 lignes, ferrés à gauche à x = 144.
- Repères : IBM Plex Mono Medium 34 px, encre #F2C96B sur #2A1E08, coin haut droit coupé de 18 px (forme `FilmEdgeShape` de l'app).
- Matière : le produit lui-même, de vrais enregistrements d'écran de l'émulateur (1080 × 2400) dans un téléphone #231A0E (bord 22 px, rayon 84 px), ombre 0 40 80 rgba(60,40,5,.35).

OBJECTS:
- Le téléphone : cadre brun foncé, écran arrondi, barre d'état réelle en mode démo (18:00).
- La notification du rappel, la pile, le résumé et le dialogue de corbeille : seulement des enregistrements réels de l'app.
- L'icône de l'app : `fastlane/metadata/android/en-US/images/icon.png`.

MOTION:
- Arrivée : expo.out 0,6 s ; caméra : power2.inOut 0,5 s ; sortie : power3.in 0,3 s.
- Titres : entrée par mots, flou 6 px → 0 et opacité 0 → 1 en 0,13 s, 0,067 s d'écart, sans translation ; immobiles jusqu'à leur sortie (flou + fondu en 0,2 s).
- Caméra : le téléphone (hauteur 1000 px) est le héros ; gros plans ×1,6 sur la zone utile, déplacement en 0,5 s.

SHOTS:
1. 0–4 s : téléphone à droite sur l'ambre, écran d'accueil ; la notification du rappel descend, la caméra pousse dessus ×1,8. Titre « 18:00, vos souvenirs vous attendent ». Son : la notification, la musique démarre.
2. 4–7 s : l'appui ouvre la pile, la caméra recule ×1. Titre « Les photos prises ce jour-là, les années passées ». Son : souffle.
3. 7–11 s : glisser à droite, voile vert « Garder ». Titre « Glissez à droite pour garder ».
4. 11–15 s : glisser à gauche, voile rouge. Titre « À gauche pour écarter ».
5. 15–18,5 s : glisser vers le haut, voile bleu, menu favori. Titre « Vers le haut : favori ou partage ».
6. 18,5–23 s : le résumé, gros plan ×1,6 sur l'espace libéré, puis le dialogue de corbeille. Titre « Rien n'est supprimé sans votre accord ».
7. 23–26 s : le téléphone recule ; titre « Vos souvenirs, un jour à la fois ».
8. 26–30 s : écran final : l'icône arrive, « PhotoSwipe » s'écrit à côté, puis « Sur Google Play et F-Droid », tenu 2,5 s.

BANNED: fond sombre, lueurs, particules, cartes en verre, trois cartes en rangée, logo dans un coin, titres qui glissent après leur arrivée, entrées identiques à chaque plan, interface inventée, téléphone incliné en 3D.

QA: 0 s (vignette : téléphone et titre lisibles), 2 s (notification lisible), 9 s (voile vert), 13 s (voile rouge), 17 s (menu favori), 20 s (espace libéré lisible), 22 s (dialogue), 28 s (icône et nom), 29,9 s (appel à l'action tenu).
