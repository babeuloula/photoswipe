# 0009 — Paquet du jour : photo instantanée et ordre remplaçable

- **Statut** : accepté
- **Date** : 2026-11-04
- **Ticket** : PS-17

## Contexte
La pile de swipe affiche les médias encore à trier d'un jour cible, dans un ordre fixé par la SPEC §3.2 : année décroissante, puis heure croissante. La SPEC veut cet ordre dans une stratégie `DeckOrdering`, remplaçable sans toucher au reste. Elle ne dit pas si le paquet est un flux (comme les compteurs de l'accueil, ADR 0008) ou une lecture ponctuelle, ni comment départager deux médias pris au même instant.

## Décision
- **`GetDeckForDay` est `suspend`** et renvoie une liste : une photo instantanée du paquet. La pile la charge une fois et retire elle-même chaque carte décidée. Un `Flow` réémettrait à chaque décision et changerait la pile sous le doigt de l'utilisateur ; l'undo (PS-20) remettra la carte dans la pile de la même façon.
- **Même règle « à trier » que `GetReviewDays`** : un média est exclu dès qu'il a une entrée pour l'année de revue du jour, quelle que soit la décision. Un test vérifie que la taille du paquet est égale au compteur de l'accueil pour chaque jour de la fenêtre.
- **`DeckOrdering` est une `fun interface` du package `service`** (pas `port` : ce n'est pas un accès à l'extérieur). Elle reçoit le fuseau à chaque appel, comme `SameDayMatcher` (ADR 0006), car l'année d'une prise de vue dépend du fuseau de l'appareil.
- **`RecentYearsFirstOrdering`** est l'implémentation par défaut : année décroissante (dans le fuseau reçu), puis instant croissant, puis identifiant croissant. Ce dernier critère, absent de la SPEC, rend l'ordre déterministe, car `MediaSource` ne garantit pas l'ordre de ses résultats.

## Alternatives envisagées
- **Paquet en `Flow`** : la pile devrait ignorer les émissions provoquées par ses propres décisions, pour un gain nul (un média ajouté à la galerie pendant le tri peut attendre la session suivante).
- **`Comparator<MediaItem>` à la place d'une stratégie** : il ne reçoit pas le fuseau, qu'il faudrait figer à la construction, alors que la SPEC demande celui de l'appareil au moment du calcul.
- **Ordre fait par `MediaSource` (`ORDER BY`)** : l'ordre dépendrait de la couche données, et la règle de la SPEC serait hors du domaine.

## Conséquences
- `:app` lie `DeckOrdering` à `RecentYearsFirstOrdering` dans un module Hilt (epic 3 ou 5). Changer d'ordre se fera en changeant cette liaison.
- La pile (epic 5) garde l'état de la session (cartes restantes, undo) dans son ViewModel ; elle rappelle `GetDeckForDay` pour repartir d'un état à jour, par exemple à la reprise après la mort du process.
