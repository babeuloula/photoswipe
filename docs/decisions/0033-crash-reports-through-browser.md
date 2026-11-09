# 0033 — Rapports de plantage envoyés par le navigateur, avec l'accord de la personne

- **Statut** : accepté
- **Date** : 2026-11-09
- **Tickets** : PS-134

## Contexte
Jérémy héberge un serveur GlitchTip (`glitchtip.babeuloula.fr`, projet PhotoSwipe) et veut y recevoir les plantages de l'application, y compris ceux des installations F-Droid, que la Play Console (Android vitals) ne voit pas. Envoyer un rapport depuis l'application exigerait la permission `INTERNET` (SPEC §0.6, interdite) : sans elle, toute connexion échoue. `INTERNET` est une permission « normale », accordée sans dialogue, mais elle est affichée sur les fiches des stores et briserait la promesse « rien ne quitte votre téléphone ».

## Décision
- **Pas de SDK Sentry, pas de permission `INTERNET`.** PhotoSwipe reste 100 % locale ; c'est le navigateur qui va sur le réseau, comme pour les liens de l'ADR 0029.
- **Capture locale** : `CrashReportHandler` (`app/crash`), installé par `PhotoSwipeApplication`, écrit le rapport du plantage (`CrashReportStore`, `noBackupFilesDir/crash/last-crash.json`, un seul rapport, le dernier) puis rend la main au gestionnaire d'Android, qui ferme l'application comme avant.
- **Contenu limité** : `CrashReportFormatter` produit directement un événement au format de Sentry, que GlitchTip lit : exception et causes (5 au plus), pile d'appels (60 appels au plus par exception), messages coupés à 1 000 caractères, version de l'application, fabricant et modèle du téléphone, version d'Android, date. Ni identifiant, ni compte, ni contenu de la galerie.
- **Accord à chaque fois** : au lancement suivant, `CrashReportPrompt` (`ui/crash`, à la racine de l'interface) propose « Voir et envoyer » ou « Ignorer ». Le rapport est effacé dans les deux cas, et ne revient pas.
- **Page d'envoi** : « Voir et envoyer » ouvre `https://babeuloula.github.io/photoswipe/crash/` (`CrashReportLink`). Le rapport, compressé en gzip et encodé en Base64 URL, est dans le fragment (`#gz=…`), que le navigateur ne transmet jamais au serveur de la page. La page (`pages/crash/`, publiée par le workflow `Pages`) affiche le rapport et ne l'envoie à l'API `store` de GlitchTip qu'à l'appui sur « Envoyer le rapport ». Elle n'utilise que ses propres fichiers (polices de l'application comprises) et une politique de sécurité du contenu qui n'autorise que GlitchTip.
- **Le DSN est dans la page, pas dans l'application** : il est public par nature (il ne permet que d'envoyer des rapports) ; le changer ne demande pas de nouvelle version de l'application.
- **Android vitals** reste la source des plantages des installations du Play Store qui n'envoient rien.

## Conséquences
- Un rapport n'arrive que si la personne l'accepte : moins de rapports qu'avec un envoi automatique.
- Le manifeste ne change pas ; la fiche F-Droid reste sans anti-fonctionnalité « Tracking ».
- `PRIVACY.md` décrit le rapport, son contenu et son envoi volontaire. La déclaration de sécurité des données de la Play Console est à revoir par Jérémy (données de plantage transmises au développeur, à la demande de la personne).
- La page et ses tests (`node --test pages/crash/*.test.mjs`) tournent dans le job obligatoire de la CI.
