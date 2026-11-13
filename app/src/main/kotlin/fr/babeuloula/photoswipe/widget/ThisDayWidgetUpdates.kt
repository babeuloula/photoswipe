package fr.babeuloula.photoswipe.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** Demande de redessiner les widgets de l'écran d'accueil, après un tri (PS-170). */
fun interface WidgetRefresher {
    /** Redessine les widgets posés, sans attendre ; rien s'il n'y en a aucun. */
    fun refresh()
}

/** Dit si au moins un widget « Ce jour-là » est posé sur l'écran d'accueil. */
fun interface PlacedWidgets {
    /** Vrai si au moins un widget est posé. */
    fun any(): Boolean
}

/** [PlacedWidgets] lu dans l'[AppWidgetManager] d'Android. */
class AppWidgetPlacedWidgets
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : PlacedWidgets {
        override fun any(): Boolean =
            AppWidgetManager
                .getInstance(context)
                .getAppWidgetIds(ComponentName(context, ThisDayWidgetReceiver::class.java))
                .isNotEmpty()
    }

/**
 * Mises à jour du widget « Ce jour-là » par WorkManager (PS-170) : une fois par jour, juste après
 * minuit, pour passer au nouveau jour ([scheduleDaily]), et après chaque tri ([refresh]), une
 * tâche unique qui remplace la précédente si plusieurs tris se suivent.
 *
 * @param workManager obtenu seulement à la première demande.
 * @param clock horloge de l'appareil, pour le délai jusqu'au prochain minuit.
 */
class ThisDayWidgetUpdates
    @Inject
    constructor(
        private val workManager: Lazy<WorkManager>,
        private val clock: Clock,
        private val placedWidgets: PlacedWidgets,
    ) : WidgetRefresher {
        /** Planifie la mise à jour quotidienne, à la pose du premier widget. */
        fun scheduleDaily() {
            val request =
                PeriodicWorkRequestBuilder<ThisDayWidgetWorker>(1, TimeUnit.DAYS)
                    .setInitialDelay(delayUntilNextDay())
                    .build()
            workManager.get().enqueueUniquePeriodicWork(
                DAILY_WORK_NAME,
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                request,
            )
        }

        /** Annule les mises à jour, au retrait du dernier widget. */
        fun cancel() {
            workManager.get().cancelUniqueWork(DAILY_WORK_NAME)
            workManager.get().cancelUniqueWork(REFRESH_WORK_NAME)
        }

        override fun refresh() {
            if (!placedWidgets.any()) return
            val request = OneTimeWorkRequestBuilder<ThisDayWidgetWorker>().build()
            workManager.get().enqueueUniqueWork(REFRESH_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }

        /** Délai jusqu'à une minute après le prochain minuit, dans le fuseau de l'horloge. */
        private fun delayUntilNextDay(): Duration {
            val now = ZonedDateTime.now(clock)
            val next =
                now
                    .toLocalDate()
                    .plusDays(1)
                    .atTime(AFTER_MIDNIGHT)
                    .atZone(clock.zone)
            return Duration.between(now, next)
        }

        companion object {
            /** Nom unique de la mise à jour quotidienne. */
            const val DAILY_WORK_NAME: String = "this_day_widget_daily"

            /** Nom unique de la mise à jour après un tri. */
            const val REFRESH_WORK_NAME: String = "this_day_widget_refresh"

            /** Heure de la mise à jour quotidienne : le nouveau jour est bien commencé. */
            private val AFTER_MIDNIGHT: LocalTime = LocalTime.of(0, 1)
        }
    }

/** Tâche qui relit les souvenirs de chaque widget « Ce jour-là » posé et le redessine. */
class ThisDayWidgetWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        ThisDayWidget.refreshAll(applicationContext)
        return Result.success()
    }
}
