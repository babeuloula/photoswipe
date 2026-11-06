# 0023 — Accès « Gestion des médias » (`MANAGE_MEDIA`)

- **Statut** : accepté
- **Date** : 2026-11-06
- **Ticket** : PS-84

## Contexte
Chaque demande de corbeille ou de favori sur un média que PhotoSwipe ne possède pas affiche un dialogue d'Android (SPEC §3.5). Jérémy veut pouvoir s'en passer. Depuis Android 12, l'accès spécial « Gestion des médias » (`MANAGE_MEDIA`), donné par l'utilisateur dans les réglages d'Android, permet à une application qui lit déjà la galerie d'appliquer ces demandes sans dialogue.

## Décision
- **Vérification** : `ManageMediaInstrumentedTest` (`:data`, émulateur, aussi en CI) donne ou retire l'accès par `appops` sur une photo du shell : avec l'accès, `createTrashRequest` et `createFavoriteRequest` s'appliquent sans dialogue ; sans lui, Android attend l'utilisateur.
- **Déclaration** : `MANAGE_MEDIA` dans le manifeste ; l'accès se lit par `MediaStore.canManageMedia` (`MediaManagementReader`, package `permission`), relu à chaque calcul de l'accueil, donc au retour des réglages.
- **Conseil sur l'accueil** : état `ManageMediaTip` (`NOT_OFFERED`, `OFFERED`, `DISMISSED`) enregistré par `SettingsRepository`. Le résumé appelle `OfferManageMediaTip` quand une confirmation vient de changer la galerie sans l'accès, c'est-à-dire après un dialogue d'Android validé. L'accueil affiche le conseil s'il est `OFFERED` et que l'accès manque ; « paramètres » ouvre `Settings.ACTION_REQUEST_MANAGE_MEDIA` (repli : informations de l'application), la croix le passe à `DISMISSED`.
- **Confirmation de PhotoSwipe** : avec l'accès, « Confirmer la suppression » ouvre d'abord un dialogue Material 3 (« Mettre N médias à la corbeille ? », restaurables 30 jours) : rien ne part sans un dernier accord. Les favoris seuls n'en demandent pas.

## Alternatives envisagées
- **Demander l'accès dès le premier lancement** : il n'a de sens qu'après avoir vu les dialogues d'Android, et ajouterait une étape de plus à l'accueil.
- **Supprimer sans aucune confirmation avec l'accès** : contraire à la SPEC §0 (rien n'est supprimé sans confirmation).
- **`MANAGE_EXTERNAL_STORAGE`** : bien plus large que nécessaire, refusé par le Play Store pour ce cas.
