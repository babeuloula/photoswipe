# 0041 — Envoi de la release au Play Store par le workflow

- **Statut** : accepté
- **Date** : 2026-11-14
- **Tickets** : PS-152 (après PS-115, PS-130, ADR 0032)

## Contexte
Le workflow `Release` construit l'AAB signé par la clé d'upload et le joint à la release GitHub ; Jérémy l'envoyait ensuite à la main dans la Play Console. L'app est en test fermé : l'accès à la production d'un compte personnel récent n'est ouvert qu'après un test fermé de 12 testeurs pendant 14 jours. Rien ne doit s'ajouter au build Gradle, que F-Droid recompile depuis les sources.

## Décision
- **Envoi par l'action `r0adkll/upload-google-play`** (épinglée sur `v1.1.5`) plutôt que par `fastlane supply` : une action Node, sans Ruby ni fastlane à installer sur le runner pour un seul envoi. Elle appelle l'API Google Play Developer avec un compte de service dont la clé JSON vit dans le secret `PHOTOSWIPE_PLAY_SERVICE_ACCOUNT_JSON`, jamais dans le dépôt.
- **Après la release GitHub, sans bloquer** : l'envoi part une fois le tag et la release publiés ; un échec (ou un secret absent) ne donne qu'un avertissement, et l'AAB joint à la release s'envoie alors à la main.
- **Piste choisie au lancement** (`play_track`) : `alpha` (le test fermé, où l'app est diffusée) par défaut, `internal` pour le test interne, `production` quand l'accès sera ouvert, `none` pour ne rien envoyer. Statut `completed` : la version est aussitôt proposée aux testeurs de la piste.
- **Notes reprises des notes F-Droid** (`scripts/play_whatsnew.py`) : le même texte anglais pour `en-US` et `fr-FR`, comme sur F-Droid, coupé en lignes entières sous 500 caractères.
- **Mapping R8 joint** : les plantages remontés par Android vitals sont lisibles.

## Conséquences
- Un envoi vers `production` échoue tant que Google n'a pas ouvert l'accès à la production ; la release GitHub reste.
- La clé du compte de service n'expire pas, mais se révoque dans Google Cloud ; la remplacer met à jour le secret.
- Le compte de service n'a que les droits de publication de PhotoSwipe dans la Play Console, aucun rôle Google Cloud.
- Les utilisateurs francophones lisent des notes en anglais, comme sur F-Droid, tant que les notes ne sont écrites qu'en anglais.
