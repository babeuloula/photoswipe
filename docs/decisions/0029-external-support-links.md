# 0029 — Noter et soutenir par des liens ouverts ailleurs

- **Statut** : accepté
- **Date** : 2026-11-07
- **Tickets** : PS-120, PS-121

## Contexte
Jérémy veut, dans les réglages, de quoi noter l'application sur le Play Store et lui faire un don PayPal (https://www.paypal.com/paypalme/jeremyreynaud), et une invitation unique à le faire sur l'accueil. La SPEC §0.6 interdit la permission `INTERNET` : l'application est 100 % locale.

## Décision
- **Des liens ouverts par d'autres applications** (`SupportLinks`, `ui/common`) : `ACTION_VIEW` sur `market://details?id=…` (application Play Store), à défaut sur la page web du Play Store, et sur la page PayPal pour le don. C'est le Play Store ou le navigateur qui va sur le réseau : PhotoSwipe garde zéro permission réseau et n'envoie rien.
- **Aucune application pour ouvrir le lien** : un message le dit (`link_unavailable`), sans plantage.
- **Pas d'API In-App Review de Google Play** : elle ajouterait une bibliothèque Play Core, n'offre pas de bouton de don et ne garantit pas d'afficher la demande. L'invitation de l'accueil est un dialogue de PhotoSwipe.
- **Une section « Soutenir PhotoSwipe »** de deux lignes dans les réglages, avant « À propos ».
- Un don sans contrepartie, par un lien externe, ne débloque rien dans l'application : il reste hors du système de facturation du Play Store.

## Conséquences
- La fiche du Play Store et la déclaration de sécurité des données ne changent pas : aucune donnée ne quitte l'appareil par PhotoSwipe.
