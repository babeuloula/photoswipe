package fr.babeuloula.photoswipe.data.reminder

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import fr.babeuloula.photoswipe.domain.usecase.SendDailyReminder

/**
 * Tâche quotidienne du rappel (SPEC §3.6), planifiée par [WorkManagerReminderScheduler] : elle
 * n'envoie la notification que s'il reste des médias à trier (`SendDailyReminder`). Si la base ou
 * la galerie ne peut pas être lue, WorkManager la relance plus tard.
 */
@HiltWorker
class ReminderWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted parameters: WorkerParameters,
        private val sendDailyReminder: SendDailyReminder,
    ) : CoroutineWorker(context, parameters) {
        override suspend fun doWork(): Result =
            runCatching { sendDailyReminder() }.fold(
                onSuccess = { Result.success() },
                onFailure = { Result.retry() },
            )
    }
