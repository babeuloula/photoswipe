# 0030 — Statistiques d'utilisation cumulées dans DataStore

- **Statut** : accepté
- **Date** : 2026-11-07
- **Ticket** : PS-122

## Contexte
Jérémy veut, dans les réglages, depuis quand l'application sert, combien de photos ont été supprimées et combien de place a été gagnée. Une fois la corbeille confirmée, les entrées `TRASHED` de la base sont purgées par le nettoyage des orphelins (le média a quitté la galerie) : l'historique des suppressions ne peut pas être relu dans `review_entry`.

## Décision
- **Des totaux cumulés, pas un historique** : `UsageStatistics` (premier jour, nombre de médias, octets libérés) derrière un port `UsageStatisticsRepository`, implémenté sur le DataStore des réglages (`DataStoreUsageStatisticsRepository`, clés `statistics_*`). Trois valeurs n'ont pas besoin d'une table Room ni d'une migration.
- **Compté par `RecordTrashResult`**, seulement pour un lot confirmé : nombre de médias de la demande, poids enregistré au marquage, un média marqué pour plusieurs années ne comptant qu'une fois. L'écriture suit celle de la base, sans transaction commune (Room et DataStore n'en partagent pas) : un arrêt entre les deux oublierait un lot, ce qui reste sans conséquence pour des statistiques.
- **Premier jour** : la date d'installation de l'application (`firstInstallTime`), à défaut aujourd'hui, fixée à la première lecture. Une installation en service avant ces statistiques garde son vrai premier jour ; ses totaux partent de zéro.
- **Une restauration depuis la corbeille du téléphone n'est pas suivie** : MediaStore ne le signale pas.
- **Les outils développeur** remettent les totaux à zéro avec les décisions.

## Conséquences
- Les suppressions faites avant cette version ne sont pas comptées.
