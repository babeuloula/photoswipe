package fr.babeuloula.photoswipe.ui.common

import android.content.IntentSender
import fr.babeuloula.photoswipe.domain.port.MediaActionRequest

/**
 * Donne le dialogue système d'une demande de corbeille ou de favori (SPEC §3.5). La demande est
 * opaque pour le domaine (ADR 0007) ; seul l'assemblage DI (`di`) connaît son implémentation
 * MediaStore et sait en tirer l'`IntentSender` à lancer (ADR 0018).
 */
fun interface MediaActionIntents {
    /** `IntentSender` du dialogue de [request]. */
    fun intentSender(request: MediaActionRequest): IntentSender
}
