# 0016 — Chargeur d'images Coil injecté par Hilt

- **Statut** : accepté
- **Date** : 2026-11-04
- **Ticket** : PS-41

## Contexte
La SPEC §7 impose Coil 3 pour afficher les médias. L'artefact habituel `coil-compose` installe un chargeur global (`SingletonImageLoader`), lu en statique par `AsyncImage` : c'est un singleton statique, que la SPEC §0 et CLAUDE.md interdisent. L'application n'a pas la permission `INTERNET` : les médias sont des URI `content://` locales.

## Décision
- **`coil-compose-core`** seulement : `AsyncImage` y reçoit son `ImageLoader` en paramètre, sans chargeur global. Aucun module `coil-network-*`.
- **Un `ImageLoader` par application, fourni par Hilt** (`di/ImageModule`, `@Singleton` du composant Hilt) : un seul cache mémoire partagé par toutes les cartes.
- **Transmis à Compose par `LocalImageLoader`** (`ui/common`) : `MainActivity` l'injecte et le fournit autour de l'interface. Sans fournisseur, la lecture échoue avec un message clair ; les aperçus et les tests qui composent une image hors de l'application en fournissent un eux-mêmes (`ImageLoader(context)`).

## Alternatives envisagées
- **`coil-compose` et `SingletonImageLoader.Factory` sur l'`Application`** : configuration standard de Coil, mais le chargeur reste lu en statique.
- **Passer l'`ImageLoader` en paramètre de chaque écran** : il traverserait toute la navigation (`NavHost`, routes) sans y servir.

## Conséquences
- Tout composable qui affiche une image lit `LocalImageLoader.current` ; un test Compose qui remplace le contenu de `MainActivity` (`setContent`) le refournit.
- Les miniatures vidéo (epic 6) s'ajouteront au même chargeur, avec le décodeur vidéo de Coil déclaré dans `ImageModule`.
