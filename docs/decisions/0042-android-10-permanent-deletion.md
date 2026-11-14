# 0042 — Suppression définitive sur Android 10

- **Statut** : accepté
- **Date** : 2026-11-14
- **Tickets** : PS-184 (epic 14 « Android 10 », après ADR 0018 et 0023)

## Contexte
PhotoSwipe s'installe dès Android 10 (PS-181) pour toucher plus de téléphones. Principe de l'epic : une fonction absente de la version d'Android n'est pas proposée, ou son texte s'adapte. La suppression des médias marqués ne peut pas être cachée : c'est le cœur de l'application. Or Android 10 n'a ni corbeille dans la galerie, ni demande groupée (`MediaStore.createTrashRequest` et `createDeleteRequest` arrivent avec Android 11). Avec le stockage limité (scoped storage), supprimer la photo d'une autre application lève une `RecoverableSecurityException` : un dialogue d'Android par média, inacceptable pour des dizaines de médias marqués.

## Décision
- **Stockage hérité sur Android 10** : `android:requestLegacyExternalStorage="true"` et `WRITE_EXTERNAL_STORAGE` (`maxSdkVersion="29"`), demandée avec la lecture (`READ_EXTERNAL_STORAGE`, même groupe, un seul dialogue). Android ignore l'attribut à partir d'Android 11, où rien ne change.
- **Suppression directe et définitive** : sur Android 10, `MediaStoreActionRequestFactory.trashRequests` donne des `DirectDeletionRequest`, que `LegacyMediaDeleter` (`:data`) supprime par `ContentResolver.delete`, sans dialogue. Un média refusé ou disparu reste marqué. Le résumé passe par `DirectDeletion` (`ui/common`), relié dans `di/MediaActionModule`, comme les dialogues par `MediaActionIntents` (ADR 0018).
- **Confirmation de PhotoSwipe obligatoire** : comme avec « Gestion des médias » (ADR 0023), le résumé demande « Supprimer définitivement 12 médias ? » avant toute suppression, et dit que rien ne pourra être restauré. L'accueil annonce « 12 médias supprimés définitivement ».
- **Rien d'autre n'est reconstruit** : pas de corbeille interne à PhotoSwipe (copier les fichiers ne libère aucune place). Restaurer, la page « Corbeille » et les favoris de la galerie ne sont pas proposés sur Android 10 (PS-183, PS-185).

## Conséquences
- Sur Android 10, une suppression confirmée est irréversible ; les textes du résumé et du tutoriel le disent (PS-185).
- La politique de confidentialité et la déclaration du Play Store mentionnent `WRITE_EXTERNAL_STORAGE`, limitée à Android 10 et à la suppression des médias que l'utilisateur a marqués.
- L'historique des suppressions (ADR 0034) et les statistiques comptent ces suppressions comme les autres ; elles ne sont jamais dans une corbeille.
- Vérifié par `LegacyMediaDeleterTest` (faux MediaStore au schéma d'Android 10) ; le test instrumenté sur un émulateur Android 10 arrive avec PS-186.
