package fr.babeuloula.photoswipe.data.reminder

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.Lazy
import fr.babeuloula.photoswipe.domain.port.ReminderScheduler
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * [ReminderScheduler] sur WorkManager (SPEC §3.6), utilisé sans l'autorisation des alarmes exactes
 * (`AlarmManagerReminderScheduler`, PS-125) : une tâche périodique unique de 24 heures, dont
 * la première exécution tombe à l'heure choisie. L'heure reste approximative : Android peut
 * décaler la tâche (Doze), ce que la SPEC accepte.
 *
 * Chaque planification remplace la précédente et recalcule le délai depuis maintenant. WorkManager
 * garde la tâche après un redémarrage ; un changement d'heure ou de fuseau la replanifie
 * (`RescheduleReminder`).
 *
 * Le rappel reprogrammé pour aujourd'hui ([scheduleOnce]) est une tâche unique à part, sous son
 * propre nom : la tâche périodique n'est pas touchée.
 *
 * @param workManager obtenu seulement à la première planification : l'écran des réglages peut
 * s'ouvrir sans initialiser WorkManager.
 * @param clock horloge de l'appareil, pour le délai jusqu'à la première exécution.
 */
class WorkManagerReminderScheduler
    @Inject
    constructor(
        private val workManager: Lazy<WorkManager>,
        private val clock: Clock,
    ) : ReminderScheduler {
        override fun schedule(time: LocalTime) {
            val request =
                PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                    .setInitialDelay(delayUntilNext(time, clock))
                    .build()
            workManager.get().enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                request,
            )
        }

        override fun scheduleOnce(at: LocalDateTime) {
            val delay = Duration.between(LocalDateTime.now(clock), at).coerceAtLeast(Duration.ZERO)
            val request = OneTimeWorkRequestBuilder<ReminderWorker>().setInitialDelay(delay).build()
            workManager.get().enqueueUniqueWork(SNOOZE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }

        override fun cancel() {
            cancelDaily()
            cancelOnce()
        }

        /** Annule la tâche du rappel quotidien seule (passage aux alarmes exactes, PS-125). */
        fun cancelDaily() {
            workManager.get().cancelUniqueWork(WORK_NAME)
        }

        /** Annule la tâche du rappel reprogrammé seule. */
        fun cancelOnce() {
            workManager.get().cancelUniqueWork(SNOOZE_WORK_NAME)
        }

        companion object {
            /** Nom unique de la tâche du rappel. */
            const val WORK_NAME: String = "daily_reminder"

            /** Nom unique de la tâche du rappel reprogrammé pour aujourd'hui. */
            const val SNOOZE_WORK_NAME: String = "snoozed_reminder"
        }
    }
