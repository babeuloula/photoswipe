package fr.babeuloula.photoswipe.data.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import fr.babeuloula.photoswipe.domain.usecase.RescheduleReminder
import fr.babeuloula.photoswipe.domain.usecase.SendDailyReminder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Reçoit les alarmes exactes du rappel (PS-125) et les confie à [ReminderAlarmHandler], hors du
 * thread principal (`goAsync`).
 */
@AndroidEntryPoint
class ReminderAlarmReceiver : BroadcastReceiver() {
    /** Envoie le rappel s'il reste des médias à trier. */
    @Inject
    lateinit var sendDailyReminder: SendDailyReminder

    /** Replanifie le rappel quotidien au lendemain. */
    @Inject
    lateinit var rescheduleReminder: RescheduleReminder

    /** Pour relancer le rappel par WorkManager si la base ou la galerie ne peut pas être lue. */
    @Inject
    lateinit var workManager: Lazy<WorkManager>

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val alarm = ReminderAlarm.of(intent.action) ?: return
        val handler =
            ReminderAlarmHandler(
                sendReminder = { sendDailyReminder() },
                rescheduleDaily = { rescheduleReminder() },
                retryLater = { workManager.get().enqueue(OneTimeWorkRequestBuilder<ReminderWorker>().build()) },
            )
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                handler.handle(alarm)
            } finally {
                pending.finish()
            }
        }
    }
}

/**
 * Ce que fait une alarme du rappel : envoyer le rappel (seulement s'il reste des médias à trier),
 * le confier à WorkManager pour un nouvel essai si la lecture échoue, puis, pour le rappel
 * quotidien, replanifier le lendemain.
 */
class ReminderAlarmHandler(
    private val sendReminder: suspend () -> Unit,
    private val rescheduleDaily: suspend () -> Unit,
    private val retryLater: () -> Unit,
) {
    /** Traite l'alarme [alarm]. */
    suspend fun handle(alarm: ReminderAlarm) {
        runCatching { sendReminder() }.onFailure { runCatching(retryLater) }
        if (alarm == ReminderAlarm.DAILY) runCatching { rescheduleDaily() }
    }
}
