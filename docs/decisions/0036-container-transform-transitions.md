# 0036 — Transitions : transformation de conteneur et fondu avec zoom

- **Statut** : accepté
- **Date** : 2026-11-11
- **Ticket** : PS-141 (remplace le glissement latéral de PS-67)

## Contexte
Jérémy a montré une vidéo d'une autre application dont il aime les transitions : la carte « Reprendre l'écoute » s'agrandit jusqu'à devenir le lecteur, les réglages apparaissent en fondu en grossissant légèrement, et pendant le geste de retour l'écran rapetisse. PhotoSwipe enchaînait ses écrans en « axe partagé horizontal » (glissement d'un dixième de la largeur et fondu).

## Décision
- **Fondu avec zoom pour tous les écrans** (`ScreenTransitions`) : l'écran qui arrive passe de 92 % à 100 % en apparaissant, celui qu'on quitte grossit à peine (104 %) en s'effaçant ; au retour, l'écran quitté rapetisse à 88 % en s'effaçant. Navigation Compose fait suivre cette sortie au geste de retour prédictif. Ressorts du thème (`MaterialTheme.motion`).
- **Transformation de conteneur** par les éléments partagés de Compose (`SharedTransitionLayout` autour du `NavHost`, `sharedBounds`) : la carte du jour et chaque ligne de « À rattraper » s'agrandissent jusqu'à l'écran de la pile du jour, la bande de vignettes jusqu'à la vue d'ensemble du jour, et s'y rétractent au retour. Clés `SharedScreenKey.Deck(jour)` et `SharedScreenKey.Overview(jour)`, appliquées par `Modifier.sharedScreen` (`ui/common/SharedScreen.kt`) ; les destinations fournissent leur animation par `LocalNavAnimatedScope`.
- **Pas d'élément partagé photo par photo** : essayé (la vignette devient la carte de la pile), mais la pile et la vue d'ensemble chargent leurs médias après l'ouverture de l'écran ; la cible n'existait pas au moment de la transition. Le conteneur, lui, existe dès le départ.
- **La carte de la pile prend sa forme en douceur** quand son image se charge (PS-142), au lieu de sauter.

## Conséquences
- Sans graphe de navigation (aperçus, tests d'un écran seul), `sharedScreen` ne fait rien.
- Les réglages, le résumé, les licences et le tutoriel n'ont que le fondu avec zoom.
- Une option « Supprimer les animations » d'Android ramène les durées à zéro, comme pour les autres animations.
