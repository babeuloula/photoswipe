# 0024 — Le résumé compte ce qui a été trié aujourd'hui

- **Statut** : accepté (remplace l'ADR 0017)
- **Date** : 2026-11-06
- **Ticket** : PS-88

## Contexte
Depuis PS-85, les favoris sont appliqués dès la pile : le résumé ne les comptait plus du tout, car il ne lisait que les favoris encore en attente. Jérémy veut retrouver à la fin combien de favoris il a ajoutés pendant sa session, qu'il ait trié un seul jour ou plusieurs. Or les gardés passaient par la route depuis la seule pile qui venait de finir (ADR 0017), et valaient 0 depuis l'accueil.

## Décision
- **La session du résumé, c'est aujourd'hui** (choix de Jérémy) : gardés et favoris sont les décisions prises aujourd'hui, dans toutes les piles de la fenêtre (aujourd'hui et jours rattrapés). `GetSessionSummary` les lit dans le journal des décisions (ADR 0019) : `made.decidedAt` tombe aujourd'hui dans le fuseau de l'horloge, `KEPT` sans favori pour les gardés, `KEPT` avec favori pour les favoris (la ligne du journal garde l'état du swipe, donc un favori déjà appliqué reste compté).
- Ce choix survit à la fermeture de l'app ; une décision annulée quitte le journal et n'est plus comptée ; le compte repart de zéro à minuit.
- **Marqués et espace libéré inchangés** : toutes les demandes de corbeille en attente (ADR 0011).
- **Plus de nombre dans la route** : `SummaryRoute` devient un objet sans paramètre, `DeckEffect.OpenSummary` aussi.
- **Écran** : deux groupes, « Trié aujourd'hui » (gardés, favoris) et « En attente de votre confirmation » (marqués).

## Alternatives envisagées
- **Depuis l'ouverture de l'app** : remis à zéro quand Android tue le process, donc de façon imprévisible.
- **La pile qui vient de finir** (ADR 0017) : ne couvre pas un tri sur plusieurs jours, et vaut 0 depuis l'accueil.
