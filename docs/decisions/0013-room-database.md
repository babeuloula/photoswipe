# 0013 — Base Room des décisions

- **Statut** : accepté
- **Date** : 2026-11-04
- **Ticket** : PS-25

## Contexte
Les décisions de tri sont persistées dans une table `review_entry` (SPEC §6), avec un index unique `(media_id, review_year)` et un schéma exporté pour tester les migrations. Room demande KSP et son plugin Gradle ; ses tests ont besoin d'Android, donc de Robolectric, jusqu'ici configuré à la main dans `:app` seulement.

## Décision
- **Room 2.8.5** dans `:data`, appliqué par une convention **`photoswipe.android.room`** (KSP, plugin `androidx.room`, schémas dans `schemas/` du module, `room-runtime` et `room-compiler`), comme Hilt (ADR 0001).
- **Clé primaire composite `(media_id, review_year)`** : elle tient lieu d'index unique, sans colonne `id` artificielle. `@Upsert` remplace la décision d'un même couple.
- **Décision stockée en texte**, sous le nom de la constante de `ReviewDecision` (conversion d'enum intégrée à Room). Lisible dans la base ; renommer une constante impose une migration.
- **DAO complet dès maintenant** : une requête par besoin du port `ReviewRepository`, pour que `RoomReviewRepository` (PS-26) ne fasse que convertir et découper les listes trop longues.
- **Configuration Robolectric sortie dans une convention `photoswipe.android.robolectric`**, appliquée à `:app` et `:data`, au lieu d'être recopiée. Chaque module garde son `robolectric.properties` (`sdk=34`, ADR 0002).

## Alternatives envisagées
- **Colonne `id` auto-incrémentée + index unique** : une colonne inutile, aucune requête ne passe par elle.
- **Décision en entier (ordinal)** : réordonner les constantes changerait silencieusement le sens des lignes existantes.
- **Tests du DAO seulement en instrumenté** : plus lents, et absents de la CI tant que PS-30 n'est pas fait ; Robolectric fait tourner la vraie SQLite du framework sur la JVM.

## Conséquences
- `data/schemas/…/1.json` est commité ; toute modification de l'entité monte la version de la base et ajoute une migration testée (PS-30).
- Les listes passées à `IN (…)` doivent rester sous la limite de paramètres SQLite : le dépôt les découpe.
- La base n'est pas encore fournie par Hilt : son module arrive avec le câblage de `:data` (PS-31).
