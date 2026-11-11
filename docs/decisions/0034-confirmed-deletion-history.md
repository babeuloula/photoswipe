# 0034 — Historique des suppressions confirmées

- **Statut** : accepté
- **Date** : 2026-11-11
- **Tickets** : PS-135 (résumé), PS-136 (jours de la semaine vidés)

## Contexte
Retours d'utilisateurs : après avoir confirmé la suppression de 10 photos, rouvrir le résumé depuis la carte du jour affichait « 0 octet » ; un jour à rattraper dont l'unique photo avait été supprimée s'affichait comme « aucun souvenir ». Une fois la corbeille confirmée, le média n'est plus lisible dans MediaStore, ses entrées `TRASHED` sont purgées par le nettoyage des orphelins et le journal l'oublie (ADR 0019) : plus rien ne dit ce qui a été fait. Les statistiques d'utilisation (ADR 0030) ne sont que des totaux, sans jour.

## Décision
- **Une table Room `confirmed_deletion`** (base en version 4, migration automatique 3 → 4, nouvelle table seulement) : une ligne par marquage confirmé, avec l'identifiant du média, le jour de revue de la fenêtre qui l'a marqué (ou `null` s'il était déjà sorti de la fenêtre), sa taille au marquage et l'instant de la confirmation. Modèle `ConfirmedDeletion`, port `DeletionHistory`, implémenté par `RoomDeletionHistory` (`data/deletion`).
- **Écrite par `RecordTrashResult`** pour chaque lot confirmé. Le jour est lu dans le journal des jours de la fenêtre avant qu'il n'oublie les médias ; un média marqué depuis la pile des marqués l'est aussi dans le journal de son jour de revue.
- **Bornée à la fenêtre** : à chaque enregistrement, les lignes confirmées avant le premier jour de la fenêtre sont oubliées. Une confirmation a toujours lieu après le jour qui a marqué le média, donc rien d'utile n'est perdu.
- **Lue par `GetSessionSummary`** (suppressions confirmées aujourd'hui, un média ne comptant qu'une fois) et par `GetReviewDays` (suppressions de chaque jour de la fenêtre, PS-136).
- **Aucune vignette** n'est gardée : rien du média supprimé n'est copié, seulement son identifiant et sa taille.

## Conséquences
- Le résumé ne montre plus « 0 octet » : avec des marquages, l'espace estimé ; tout confirmé, l'espace libéré aujourd'hui ; sinon, pas d'espace du tout.
- Une restauration depuis la corbeille du téléphone n'est pas suivie (comme pour l'ADR 0030) : le média restauré redevient un souvenir, et la ligne reste jusqu'à sortir de la fenêtre.
- Les suppressions confirmées avant cette version ne sont pas connues.
- La remise à zéro des outils développeur vide aussi cette table (`clearAllTables`).
