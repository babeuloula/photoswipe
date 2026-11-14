package fr.babeuloula.photoswipe.share

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import androidx.core.content.IntentCompat
import dagger.hilt.android.AndroidEntryPoint
import fr.babeuloula.photoswipe.domain.port.ShareTargetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Reçoit l'application choisie dans la feuille de partage (PS-163) et la retient, pour proposer
 * « Renvoyer avec … » au partage suivant. Android ne dit que l'application, jamais le contact.
 */
@AndroidEntryPoint
class ShareTargetReceiver : BroadcastReceiver() {
    /** Où retenir l'application choisie. */
    @Inject
    lateinit var shareTargets: ShareTargetRepository

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val chosen = chosenTarget(intent) ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                runCatching { shareTargets.saveLastTarget(chosen) }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        /** Application choisie que porte [intent] (`paquet/classe`), `null` si Android ne la donne pas. */
        internal fun chosenTarget(intent: Intent): String? =
            IntentCompat
                .getParcelableExtra(intent, Intent.EXTRA_CHOSEN_COMPONENT, ComponentName::class.java)
                ?.flattenToString()

        /**
         * Rappel à passer à la feuille de partage : Android y ajoute l'application choisie
         * ([Intent.EXTRA_CHOSEN_COMPONENT]) et l'envoie à ce récepteur.
         */
        fun callback(context: Context): IntentSender =
            PendingIntent
                .getBroadcast(
                    context,
                    0,
                    Intent(context, ShareTargetReceiver::class.java),
                    // Modifiable : Android doit pouvoir y écrire l'application choisie.
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                ).intentSender
    }
}
