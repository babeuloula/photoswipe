package fr.babeuloula.photoswipe.data.reminder

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.Lazy
import fr.babeuloula.photoswipe.domain.port.ReminderScheduler
import java.time.Clock
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * [ReminderScheduler] sur WorkManager (SPEC §3.6) : une tâche périodique unique de 24 heures, dont
 * la première exécution tombe à l'heure choisie. L'heure reste approximative : Android peut
 * décaler la tâche (Doze), ce que la SPEC accepte.
 *
 * Chaque planification remplace la précédente et recalcule le délai depuis maintenant. WorkManager
 * garde la tâche après un redémarrage ; un changement d'heure ou de fuseau la replanifie
 * (`RescheduleReminder`).
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

        override fun cancel() {
            workManager.get().cancelUniqueWork(WORK_NAME)
        }

        companion object {
            /** Nom unique de la tâche du rappel. */
            const val WORK_NAME: String = "daily_reminder"
        }
    }
