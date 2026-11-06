package fr.babeuloula.photoswipe.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import fr.babeuloula.photoswipe.domain.usecase.RescheduleReminder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Replanifie le rappel quotidien après un changement d'heure ou de fuseau (SPEC §3.6) : le délai
 * de la tâche WorkManager était calculé sur l'ancienne heure. Ces deux diffusions sont permises
 * aux récepteurs déclarés dans le manifeste. Après un redémarrage, WorkManager garde la tâche.
 */
@AndroidEntryPoint
class ReminderRescheduleReceiver : BroadcastReceiver() {
    /** Replanifie le rappel sur la nouvelle heure. */
    @Inject
    lateinit var rescheduleReminder: RescheduleReminder

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action !in HANDLED_ACTIONS) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                runCatching { rescheduleReminder() }
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val HANDLED_ACTIONS = setOf(Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED)
    }
}
