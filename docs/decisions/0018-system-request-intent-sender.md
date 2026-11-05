# 0018 — Dialogues système : de la demande opaque à l'`IntentSender`

- **Statut** : accepté
- **Date** : 2026-11-05
- **Ticket** : PS-54

## Contexte
La confirmation (SPEC §3.5) passe par des dialogues système : `MediaStore.createTrashRequest` (et plus tard `createFavoriteRequest`) donne un `PendingIntent` dont l'écran lance l'`IntentSender`. Le domaine ne voit qu'une `MediaActionRequest` opaque (ADR 0007). L'implémentation vit dans `:data`, et `:app` n'importe `:data` que depuis son package `di` (règle Konsist, ADR 0005).

## Décision
- **`:data`** : `MediaStoreActionRequestFactory` crée une `IntentSenderMediaActionRequest` par lot de 500 médias au plus ; elle porte les identifiants des médias et l'`IntentSender` du dialogue. Les fonctions de création sont injectables, pour tester le découpage sans MediaStore.
- **`:app`** : `MediaActionIntents` (`ui/common`, interface fonctionnelle) donne l'`IntentSender` d'une demande. Seul `di/MediaActionModule` la fournit, en reconnaissant l'implémentation de `:data`.
- **Écran** : le `SummaryViewModel` présente les lots un par un (effet avec l'`IntentSender`) ; `SummaryRoute` les lance par `rememberLauncherForActivityResult(StartIntentSenderForResult)` et renvoie la réponse (`RESULT_OK` ou non) par un événement, enregistrée par `RecordTrashResult`.

## Alternatives envisagées
- **Exposer l'`IntentSender` dans le port du domaine** : `:domain` est du Kotlin pur, sans Android.
- **Lancer le dialogue depuis `:data`** : il faut une activité et un résultat, propres à l'écran.

## Conséquences
- Les favoris système (PS-56) suivront le même chemin, avec `favoriteRequests`.
- Si le process meurt pendant un dialogue, la demande en cours est perdue : les marquages restent en base et une nouvelle confirmation les reprend (SPEC §3.5).
