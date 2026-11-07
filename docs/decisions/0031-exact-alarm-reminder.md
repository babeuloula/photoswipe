# 0031 — Rappel à l'heure pile par alarme exacte

- **Statut** : accepté (remplace, pour l'heure du rappel, le choix WorkManager de PS-61)
- **Date** : 2026-11-07
- **Ticket** : PS-125

## Contexte
Le rappel quotidien et sa reprogrammation (PS-116) passaient par WorkManager. Une reprogrammation à 9h05 est arrivée à 9h07 : JobScheduler retenait la tâche (`dumpsys jobscheduler` : contrainte `FLEXIBILITY`). La SPEC §3.6 acceptait une heure approximative ; Jérémy veut l'heure pile.

## Décision
- **Alarmes exactes d'Android** (`AlarmManagerReminderScheduler`, `:data`) : `setExactAndAllowWhileIdle(RTC_WAKEUP)`, téléphone en veille compris, une alarme pour le rappel quotidien, une autre pour le rappel reprogrammé d'aujourd'hui. `ReminderAlarmReceiver` envoie le rappel (`SendDailyReminder`, seulement s'il reste des médias à trier), le confie à WorkManager si la lecture échoue, puis replanifie le lendemain (`RescheduleReminder`).
- **Permission `SCHEDULE_EXACT_ALARM`**, accordée par l'utilisateur (« Alarmes et rappels », refusée par défaut sur Android 14+). Pas `USE_EXACT_ALARM` : Google Play la réserve aux applications de réveil et d'agenda.
- **Sans l'autorisation, repli sur WorkManager** (`WorkManagerReminderScheduler`, heure approximative) ; les réglages l'expliquent et ouvrent « Alarmes et rappels » (`ACTION_REQUEST_SCHEDULE_EXACT_ALARM`). Chaque planification efface celle de l'autre moyen ; le changement d'autorisation (`ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`) replanifie.
- **Redémarrage** : les alarmes ne survivent pas ; `BOOT_COMPLETED` (`RECEIVE_BOOT_COMPLETED`) replanifie le rappel quotidien. Le rappel reprogrammé pour aujourd'hui n'est pas repris.
- `setAlarmClock` est écarté : il afficherait l'icône de réveil dans la barre d'état.

## Conséquences
- Play Console : déclarer l'usage de `SCHEDULE_EXACT_ALARM` (rappel choisi par l'utilisateur) à la publication.
- En Doze, Android limite les alarmes « while idle » à une toutes les quelques minutes par application : sans effet pour un rappel par jour.
