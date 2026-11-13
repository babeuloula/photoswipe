# Couverture du tutoriel

Le tutoriel (SPEC §4, epic 11) est fait en dernier pour couvrir les fonctionnalités réellement livrées. Cette liste les compare à son contenu ; elle se relit à chaque nouvelle fonctionnalité visible. Si le contenu du tutoriel change fortement, `Tutorial.CURRENT_VERSION` (`domain/model/Tutorial.kt`) est incrémenté pour le proposer de nouveau.

Revue du 2026-11-06 (PS-83). Version du tutoriel : **1**. Elle n'a pas été incrémentée : le tutoriel n'a encore été livré à personne, la version 1 est ce contenu.

| Fonctionnalité livrée | Tickets | Où dans le tutoriel |
|---|---|---|
| Souvenirs du même jour des années précédentes, photos et vidéos | epics 3 à 6 | Page 1 « Les souvenirs du même jour » (animation des années) |
| Glisser à droite : garder, le média revient l'année suivante | PS-42 | Page 2 « Garder ou supprimer » (animation : à droite avec le voile vert « Garder », puis à gauche avec le voile rouge « Supprimer », PS-76) ; mode entraînement |
| Glisser à gauche : marquer, rien n'est supprimé sans confirmation | PS-43 | Page 2 ; mode entraînement |
| Boutons équivalents aux gestes, « Annuler » | PS-45, PS-46 | Page 2 (« Annuler ») ; mode entraînement (mêmes boutons) |
| Glisser vers le haut : favori (gardé et ajouté aux favoris) ou partage | PS-47, PS-48, PS-85 | Page 3 « Favori ou partage » (animation) ; mode entraînement |
| Derniers jours à rattraper depuis l'accueil (7 par défaut, 14 ou 30 au choix dans les réglages, PS-169) | PS-38, PS-169 | Page 4 « Rattrapez les jours manqués » (liste verticale des jours, comme sur l'accueil, PS-137 : un utilisateur avait pris l'ancienne frise horizontale pour un widget) ; le texte dit « une semaine par défaut, jusqu'à 30 jours dans les réglages » ; l'animation garde une semaine d'exemple. Version du tutoriel inchangée (simple précision du texte) |
| Retrier un jour déjà trié | PS-108, PS-111 | Page 4 (texte) |
| Rappel quotidien, seulement s'il reste des souvenirs, à activer dans les réglages | epic 8 | Page 4 (texte et notification d'exemple) ; le tutoriel ne l'active pas |
| Résumé de fin de tri, espace libéré estimé | PS-53, PS-88 | Page 5 « Rien n'est définitif » (animation) |
| Confirmation groupée, corbeille du système restaurable environ 30 jours | PS-54, PS-55 | Page 5 (animation du dialogue et texte) ; aucune promesse sur Google Photos (SPEC §16.7) |
| Revoir les médias marqués en grand depuis le résumé | PS-112 | Page 5 (texte) |
| Page « Corbeille » : restaurer ou supprimer définitivement ce que PhotoSwipe a supprimé | PS-168 | Page 5 (texte : restaurable environ 30 jours) ; la page elle-même n'est pas montrée, elle s'ouvre depuis le résumé |
| Essai sans risque des gestes | PS-80 | Page 6 « Essayez » (cartes d'exemple) |
| Accès aux photos, accès partiel d'Android 14+ | epic 4 | Hors tutoriel : écrans d'accès, juste après lui |
| Lecture des vidéos (muette au départ, son au ▶) | epic 6, PS-105 | Non détaillé : découvert sur la carte même, sans risque |
| Accès « Gestion des médias » | PS-84 | Hors tutoriel : conseil de l'accueil, proposé au bon moment |
| Comparer avec la photo précédente d'un groupe de photos proches, en maintenant le doigt sur la carte | PS-160 | Hors tutoriel : la puce ⧉ signale le groupe, découverte sans risque |
| Vidéo lue en ×2 en maintenant le doigt dessus, sur la carte et en plein écran | PS-161 | Hors tutoriel : geste courant des applications vidéo, découvert sans risque |
| « Ouvrir avec » une autre application (voir ou retoucher) depuis le menu de la carte | PS-162 | Hors tutoriel : dans le menu, à côté de « Partager » |
| « Renvoyer avec … » la dernière application de partage, depuis le menu de la carte | PS-163 | Hors tutoriel : apparaît après un premier partage |
| Pastille « Floue ? » / « Très sombre ? » sur les photos peut-être ratées, réglage « Signaler les photos ratées » | PS-164 | Hors tutoriel : pastille explicite, sans conséquence |
| « Partager en carte postale » une photo, avec son repère « Il y a 5 ans » et la mention PhotoSwipe à retirer dans l'aperçu | PS-177 | Hors tutoriel : dans le menu de la carte, sous « Partager », avec aperçu avant l'envoi |
| Pastille « Doublon » sur les copies d'une même image parmi les souvenirs du jour | PS-165 | Hors tutoriel : pastille explicite, sans conséquence |
| Vue d'ensemble d'un jour (tous ses souvenirs par année) | PS-139 | Hors tutoriel : ouverte en touchant les vignettes de la carte du jour ou l'icône de grille de la pile, découverte sans risque |
| Page « Progression » (statistiques d'utilisation, graphiques des six derniers mois, série de jours, calendrier de l'année qui ouvre n'importe quel jour, même à l'avance, objectif de place à libérer, badges), ouverte par le bouton de la barre de l'accueil, qui porte la série en pastille | PS-153 à PS-158 | Hors tutoriel : découverte depuis l'accueil ou les réglages, sans risque |
| Liens profonds `photoswipe://`, raccourcis de l'icône (« Trier aujourd'hui », « Résumé ») et tuile des réglages rapides | PS-171, PS-172 | Hors tutoriel : raccourcis découverts par l'appui long habituel sur l'icône, tuile proposée par Android dans l'édition des réglages rapides, liens documentés dans le README pour les automatisations |
| Widget « Ce jour-là » de l'écran d'accueil (vignette, date, nombre à trier, option sans photo) | PS-170 | Hors tutoriel : découvert dans le sélecteur de widgets du téléphone, avec son aperçu |
| Langue, thème (dont le noir pur du thème sombre), animations, licences, revoir le tutoriel | epic 9, PS-81, PS-87, PS-146, PS-176 | Hors tutoriel : réglages |
| Secouer le téléphone pour annuler la dernière décision (désactivé par défaut, proposé seulement avec un accéléromètre) | PS-175 | Hors tutoriel : réglage de la section « Gestes » ; le tutoriel présente le bouton « Annuler » (page 2), que la secousse ne fait que doubler, et l'entraînement ne l'écoute pas |
| Célébration de fin de pile (confettis à la dernière carte triée d'un jour, désactivée par défaut) | PS-174 | Hors tutoriel : réglage de la section « Apparence », une surprise qu'on découvre en l'activant ; pas de confettis dans l'entraînement |
| Vue d'ensemble d'un jour (tous ses souvenirs par année), avec tri par lot (appui long, garder ou supprimer plusieurs souvenirs, « Annuler ») | PS-139, PS-159 | Hors tutoriel : ouverte en touchant les vignettes de la carte du jour ou l'icône de grille de la pile, découverte sans risque |
| Page « Progression » (statistiques d'utilisation, graphiques des six derniers mois, série de jours), ouverte par le bouton de la barre de l'accueil, qui porte la série en pastille | PS-153, PS-154, PS-155 | Hors tutoriel : découverte depuis l'accueil ou les réglages, sans risque |
| Langue, thème, licences, revoir le tutoriel | epic 9, PS-81, PS-87 | Hors tutoriel : réglages |
| Nouveautés après une mise à jour, relues dans « À propos » des réglages | PS-173 | Hors tutoriel : la feuille ne s'affiche qu'après une mise à jour, jamais à la première installation, où le tutoriel présente déjà tout |

## Écart connu
Aucun : l'animation de la page « Garder ou supprimer » (PS-76) est livrée.
