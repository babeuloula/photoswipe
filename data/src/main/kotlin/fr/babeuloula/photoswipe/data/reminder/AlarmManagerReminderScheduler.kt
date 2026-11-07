package fr.babeuloula.photoswipe.data.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.babeuloula.photoswipe.domain.port.ReminderScheduler
import java.time.Clock
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

/**
 * [ReminderScheduler] à l'heure pile (PS-125, ADR 0031) : une alarme exacte d'Android
 * (`setExactAndAllowWhileIdle`, téléphone en veille compris) pour le rappel quotidien, une autre
 * pour le rappel reprogrammé d'aujourd'hui. À l'heure, [ReminderAlarmReceiver] envoie le rappel
 * (seulement s'il reste des médias à trier) et replanifie le lendemain.
 *
 * Les alarmes exactes demandent l'autorisation « Alarmes et rappels » (`SCHEDULE_EXACT_ALARM`),
 * que l'utilisateur accorde dans les réglages d'Android 14+. Sans elle, la planification passe par
 * WorkManager ([fallback]), dont l'heure est approximative. Chaque planification efface celle de
 * l'autre moyen.
 */
class AlarmManagerReminderScheduler
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val clock: Clock,
        private val fallback: WorkManagerReminderScheduler,
    ) : ReminderScheduler {
        private val alarmManager: AlarmManager get() = context.getSystemService(AlarmManager::class.java)

        override fun schedule(time: LocalTime) {
            if (alarmManager.canScheduleExactAlarms()) {
                fallback.cancelDaily()
                setExact(ReminderAlarm.DAILY, nextOccurrence(time, clock).toInstant().toEpochMilli())
            } else {
                alarmManager.cancel(pendingIntent(ReminderAlarm.DAILY))
                fallback.schedule(time)
            }
        }

        override fun scheduleOnce(at: LocalDateTime) {
            if (alarmManager.canScheduleExactAlarms()) {
                fallback.cancelOnce()
                setExact(ReminderAlarm.SNOOZE, at.atZone(clock.zone).toInstant().toEpochMilli())
            } else {
                alarmManager.cancel(pendingIntent(ReminderAlarm.SNOOZE))
                fallback.scheduleOnce(at)
            }
        }

        override fun cancel() {
            ReminderAlarm.entries.forEach { alarmManager.cancel(pendingIntent(it)) }
            fallback.cancel()
        }

        private fun setExact(
            alarm: ReminderAlarm,
            triggerAtMillis: Long,
        ) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent(alarm))
        }

        private fun pendingIntent(alarm: ReminderAlarm): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                alarm.requestCode,
                Intent(context, ReminderAlarmReceiver::class.java).setAction(alarm.action),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
    }

/**
 * Alarmes du rappel.
 *
 * @property action action de l'`Intent` reçu par [ReminderAlarmReceiver].
 * @property requestCode code distinct de chaque alarme, pour qu'elles ne se remplacent pas.
 */
enum class ReminderAlarm(
    val action: String,
    val requestCode: Int,
) {
    /** Rappel quotidien, replanifié au lendemain une fois envoyé. */
    DAILY("fr.babeuloula.photoswipe.action.DAILY_REMINDER", 1),

    /** Rappel reprogrammé pour aujourd'hui, unique. */
    SNOOZE("fr.babeuloula.photoswipe.action.SNOOZED_REMINDER", 2),
    ;

    companion object {
        /** Alarme d'une action reçue, `null` si l'action est inconnue. */
        fun of(action: String?): ReminderAlarm? = entries.find { it.action == action }
    }
}
