# 0035 — Photos proches repérées par leur heure de prise de vue

- **Statut** : accepté
- **Date** : 2026-11-11
- **Ticket** : PS-140

## Contexte
Retour d'utilisateur : avant de décider d'une photo, il veut savoir s'il en a d'autres semblables (« si non, je vais être plus enclin à garder »). L'application est 100 % locale, sans permission `INTERNET`, et n'ajoute pas de bibliothèque sans raison. Comparer le contenu des images (hachage perceptuel, modèle d'apprentissage) demanderait de décoder chaque photo du jour, d'en garder une empreinte et d'en payer le coût en temps et en batterie à chaque ouverture.

## Décision
- **Une règle sur la seule heure de prise de vue** : des photos prises à la suite, chacune moins de **30 secondes** après la précédente, forment un groupe de photos proches (rafale, plusieurs essais de la même scène). Les vidéos n'en font pas partie. Service pur `SimilarMediaGrouping` (`:domain/service`), modèle `SimilarGroup` (« 2 sur 3 »).
- **Calculée sur tous les souvenirs du jour**, triés ou non (`GetSimilarMemories`, `GetDayOverview`) : la pile dit qu'une photo a des voisines même si celles-ci sont déjà triées.
- **Affichage** : une puce « ⧉ 1/3 » au bas de la carte de la pile (TalkBack : « Photo 1 sur 3 prises à la suite »), et une pastille sur la case de la vue d'ensemble du jour (PS-139), où l'on peut comparer les voisines.
- **Rien n'est enregistré** : le calcul est refait à l'ouverture, sans décoder d'image.

## Conséquences
- Deux photos semblables prises à plus de 30 secondes d'écart ne sont pas reliées ; deux photos différentes prises coup sur coup le sont. C'est une aide, pas une vérité : la vue d'ensemble permet de vérifier.
- L'écart se règle dans `SimilarMediaGrouping` (paramètre `maxGap`). Une comparaison du contenu pourra venir plus tard, par une nouvelle ADR, sans changer l'affichage.
- Une erreur de calcul n'empêche pas d'ouvrir la pile (repli sur aucune photo proche).
