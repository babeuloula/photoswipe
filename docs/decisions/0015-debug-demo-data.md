# 0015 — Données de démo en debug et liaison de `MediaSource` par variante

- **Statut** : accepté
- **Date** : 2026-11-04
- **Ticket** : PS-36

## Contexte
La SPEC §10 (niveau 2) demande, en debug seulement, de pouvoir remplacer la galerie par un `FakeMediaSource` rempli d'un jeu de données généré (plusieurs années pour les 7 jours de la fenêtre, 29 février, plusieurs médias le même jour, grosse vidéo, média sans `DATE_TAKEN`), sans rien laisser dans la variante release. Le port `MediaSource` était lié à `MediaStoreMediaSource` dans `:data` (PS-31), ce qui empêche toute autre liaison.

## Décision
- **`MediaSource` est lié dans `:app`, par variante** : `src/release/…/di/MediaSourceModule` lie `MediaStoreMediaSource` ; `src/debug/…/di/MediaSourceModule` lie `SwitchableMediaSource`, qui délègue à MediaStore ou aux données de démo selon un réglage relu à chaque appel. Les autres ports restent liés dans `:data`.
- **Jeu de démo** : `DemoMediaCatalog` (debug) le génère pour le jour courant de la `Clock` injectée, avec les builders des `testFixtures` de `:domain`, ajoutés en `debugImplementation` seulement. Les médias n'ont pas de fichier : le jeu sert à l'accueil et aux compteurs ; les vraies images viendront du seeder MediaStore (niveau 3, epic 10).
- **Bascule** : `DeveloperToolsActivity`, déclarée par le manifeste debug avec sa propre entrée dans le lanceur (« Outils dev PhotoSwipe »), enregistre le réglage dans un fichier `SharedPreferences` propre au debug. Aucun code de `src/main` ne la référence.
- **Tests par variante** : `src/testDebug` pour la bascule et le jeu de démo, `src/testRelease` pour vérifier que la release lit MediaStore et ne contient pas les outils. AGP 9 n'active les tests unitaires qu'en debug : ils sont activés pour release, filtrés sur les seuls tests de `src/testRelease`.

## Alternatives envisagées
- **Écran « Outils développeur » dans la navigation** : il faudrait un point d'entrée dans `src/main`, présent en release.
- **Indicateur `BuildConfig` figé à la compilation** : impossible de basculer sans réinstaller.
- **Garder la liaison dans `:data` avec un qualificatif** : `:data` ne connaît pas les variantes de l'application.

## Conséquences
- Une nouvelle source de données de démo (par exemple pour le dépôt des décisions) suivra le même schéma : liaison par variante dans `:app`.
- La règle Konsist « `:app` n'importe `:data` que depuis `di` » s'applique aussi aux sources debug et release : la source commutable vit dans `di`.
