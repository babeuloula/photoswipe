# 0037 — Activité de tri par jour

- **Statut** : accepté
- **Date** : 2026-11-13
- **Tickets** : PS-154 (graphiques de la page « Progression »)

## Contexte
La page « Progression » doit montrer l'activité des derniers mois : médias gardés, médias supprimés, espace libéré. Rien de ce qui existe ne le permet. Les statistiques d'utilisation (ADR 0030) sont des totaux sans date. Le journal des décisions (ADR 0019) oublie un jour dès qu'il sort de la fenêtre, et un média dès que sa suppression est confirmée. L'historique des suppressions (ADR 0034) est borné à la fenêtre. Les décisions elles-mêmes (`review_entry`) disparaissent avec les orphelins.

## Décision
- **Une table Room `daily_activity`** (base en version 5, migration automatique 4 → 5, nouvelle table seulement) : une ligne par jour, avec le nombre de médias gardés, de favoris, de médias envoyés à la corbeille et les octets libérés. Modèle `DailyActivity`, port `ActivityHistory`, implémenté par `RoomActivityHistory` (`data/activity`).
- **Le jour est celui où la décision est prise**, dans le fuseau de l'appareil, pas le jour de revue trié : rattraper le 10 un souvenir du 8 compte pour le 10.
- **Les médias gardés entrent avec la décision elle-même** : `DecisionJournalDao.record` et `undo` mettent à jour `daily_activity` dans la transaction qui écrit la décision et sa ligne de journal. Une décision annulée sort donc du compte, et un arrêt brutal ne peut pas laisser le compte en désaccord avec les décisions. Le compteur ne passe jamais sous zéro : une décision prise avant la version 5 peut être annulée sans avoir été comptée. Un favori est un média gardé avec `favorite_pending` au moment de la décision ; il compte dans les deux.
- **Les suppressions entrent à la confirmation**, par `RecordTrashResult` (`ActivityHistory.recordTrashed`), le jour de la confirmation, avec le poids enregistré au marquage. Un média marqué n'est pas compté tant qu'il n'est pas confirmé : il n'est pas encore « trié ».
- **Lue par `GetActivityReport`**, qui regroupe les six derniers mois, sans trou, et donne le premier jour enregistré.
- **Aucune bibliothèque de graphiques** : les barres sont dessinées par Compose (`ActivityCharts.kt`), leurs libellés sont du texte, chaque graphique est une image décrite pour TalkBack.

## Conséquences
- L'historique commence à cette version : la page le dit (« Historique depuis le… »). Les totaux d'avant restent dans les statistiques d'utilisation.
- Garder un média pour plusieurs années de revue d'un coup (PS-113) écrit une décision par année : chacune compte.
- La table ne grossit que d'une ligne par jour d'utilisation ; elle n'est pas purgée.
- Une restauration depuis la corbeille du téléphone n'est pas suivie (comme pour les ADR 0030 et 0034).
- La remise à zéro des outils développeur vide aussi cette table (`clearAllTables`).
- Le fake `FakeDecisionJournal` ne compte pas l'activité : c'est le rôle de la transaction Room, testée sur une vraie base (`RoomDecisionJournalTest`).
