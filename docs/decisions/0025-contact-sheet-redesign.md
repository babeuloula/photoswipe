# 0025 — Refonte visuelle « planche-contact »

- **Statut** : accepté
- **Date** : 2026-11-06
- **Ticket** : PS-89 (epic 12, PS-89 à PS-101)

## Contexte
Le thème Material 3 de base (graine bleue #769CDF, typographie et formes par défaut) était correct mais générique. Jérémy a fait dessiner une refonte dans Claude Design ([maquettes](https://claude.ai/artifact/C3WwuLrAdQtd5vemmqyBLP)), en restant dans Material 3 : la planche-contact du labo photo, gris papier en clair, noir de labo en sombre, ambre de la lampe inactinique en accent, et un repère de bord de film « Il y a 3 ans · 6 nov. 2023 » comme signature.

## Décision
- **Palette de repli** générée avec material-color-utilities (schéma Tonal spot) depuis la graine **#8A6A1E** ; les neutres du schéma sont remplacés par un gris papier photo (teinte 85, chroma 3 ; chroma 5 pour les variantes), ceux des maquettes. Tous les rôles sont écrits dans `Color.kt`, rôles fixes compris.
- **Couleurs dynamiques gardées** (SPEC §8) : sur un téléphone, l'ambre n'apparaît que si Material You est désactivé ; les aperçus et les tests utilisent la palette de repli.
- **Garder et favori** : #1E8A4A et #2F6FD0 harmonisés vers la graine (`Blend.harmonize`), écrits en dur dans `SwipeColors` avec leurs conteneurs (tons 40 / 100 / 90 / 10 en clair, 80 / 20 / 30 / 90 en sombre) : appeler `MaterialColors.harmonize` à l'exécution ferait entrer la bibliothèque Material Components (Views). Supprimer reste le rôle `error` et son conteneur.
- **Rôles fixes** pour ce qui est posé sur les photos (repère, pastilles, boutons de la vidéo) : `primaryFixed`, `primaryFixedDim`, `onPrimaryFixed` de `ColorScheme` (Material 3 1.4), identiques en clair et en sombre.
- **Mouvement** : la carte sort sans rebond (`fastExit`, amortissement 1, raideur 1 400, vitesse du doigt reprise) ; la carte suivante remonte avec le ressort rapide du schéma standard (`fastSpatialStandard`, 0,9 / 1 400). Le retour au centre garde son léger rebond.
- **Formes** : l'échelle Material 3 par défaut, que les maquettes reprennent.
- **Le seuil du geste reste celui de la SPEC** (35 % de la largeur) ; les maquettes parlaient de 120 dp.
- **Les photos des maquettes n'entrent pas dans l'APK** : les illustrations sont faites de formes du thème.
- La suite de la refonte (typographie, icônes, repère, écrans, icône de l'app) est découpée en tickets PS-90 à PS-101.

## Alternatives envisagées
- **Désactiver les couleurs dynamiques** pour imposer l'ambre : contraire à la SPEC §8 ; à trancher plus tard par un réglage si Jérémy le veut.
- **Garder les neutres du schéma Tonal spot** : plus jaunes que le gris papier des maquettes.
