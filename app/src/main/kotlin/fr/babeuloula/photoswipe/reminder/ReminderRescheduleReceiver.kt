package fr.babeuloula.photoswipe.reminder

import android.app.AlarmManager
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
 * de la tâche WorkManager était calculé sur l'ancienne heure. Aussi après un redémarrage, qui efface
 * les alarmes exactes, et quand l'autorisation des alarmes exactes change : le rappel passe des
 * alarmes à WorkManager ou l'inverse (PS-125). Ces diffusions sont permises aux récepteurs déclarés
 * dans le manifeste. Le rappel reprogrammé pour aujourd'hui n'est pas repris après un redémarrage.
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
        val HANDLED_ACTIONS =
            setOf(
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
                Intent.ACTION_BOOT_COMPLETED,
                AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            )
    }
}
