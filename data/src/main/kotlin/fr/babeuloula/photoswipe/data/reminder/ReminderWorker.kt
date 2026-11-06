package fr.babeuloula.photoswipe.data.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Tâche quotidienne du rappel (SPEC §3.6), planifiée par [WorkManagerReminderScheduler].
 *
 * Elle ne fait encore rien : vérifier s'il reste des médias à trier et n'envoyer la notification
 * que dans ce cas arrive avec PS-62.
 */
class ReminderWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = Result.success()
}
