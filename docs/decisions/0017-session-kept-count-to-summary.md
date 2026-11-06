# 0017 — Gardés de la session transmis au résumé par la route

- **Statut** : remplacé par l'ADR [0024](0024-summary-sorted-today.md) (PS-88)
- **Date** : 2026-11-05
- **Ticket** : PS-53

## Contexte
Le résumé (SPEC §3.5) affiche les gardés de la session, en plus des marqués, des favoris et de l'espace libéré de toutes les demandes en attente (ADR 0011). La session (`ReviewSession`) vit dans le `DeckViewModel` (ADR 0010) ; le résumé est une autre destination, avec son propre ViewModel. On l'ouvre aussi depuis l'accueil, sans session.

## Décision
- **La pile compte ses gardés** : les décisions de la session sorties à droite (gardées sans favori). Une décision annulée a quitté la session et n'est pas comptée, comme le veut l'ADR 0011.
- **Le nombre passe par la route** : `SummaryRoute(keptCount: Int = 0)`, type simple, comme l'impose l'ADR 0014. L'accueil ouvre `SummaryRoute()` : 0 gardé.
- **Le `SummaryViewModel` lit le reste** par `GetSessionSummary` (marqués, favoris, espace libéré en base) et y met les gardés de la route.

## Alternatives envisagées
- **Passer toute la session au résumé** (liste de décisions dans la route ou objet partagé) : la route n'accepte que des types simples, et un objet partagé entre deux ViewModels ajouterait un état global à gérer pour un seul nombre.
- **Recalculer les gardés en base** : rien ne distingue en base les décisions de la session de celles des sessions précédentes.

## Conséquences
- Le nombre survit à la mort du process avec l'état de navigation.
- La confirmation (PS-54) ne dépend pas des gardés : elle n'utilise que les demandes en attente en base.
