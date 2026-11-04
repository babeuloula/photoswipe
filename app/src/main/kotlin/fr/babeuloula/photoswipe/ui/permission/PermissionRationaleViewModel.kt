package fr.babeuloula.photoswipe.ui.permission

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/** Action de l'utilisateur sur l'écran d'explication. */
sealed interface PermissionRationaleEvent {
    /** Bouton « Continuer ». */
    data object Continue : PermissionRationaleEvent
}

/** Effet ponctuel que l'écran demande à son hôte d'exécuter. */
sealed interface PermissionRationaleEffect {
    /** Demander l'accès aux photos et vidéos (SPEC §4.1). */
    data object RequestMediaAccess : PermissionRationaleEffect
}

/**
 * ViewModel de l'écran qui explique pourquoi l'application demande l'accès aux médias (SPEC §4.1).
 *
 * L'écran n'a pas d'état variable : son texte est fixe. Le ViewModel traduit l'action
 * « Continuer » en un effet [PermissionRationaleEffect.RequestMediaAccess], livré une seule fois
 * même si l'écran est recréé entre-temps (rotation).
 */
class PermissionRationaleViewModel : ViewModel() {
    private val effects = Channel<PermissionRationaleEffect>(Channel.BUFFERED)

    /** Effets à exécuter, chacun livré une seule fois. */
    val uiEffects: Flow<PermissionRationaleEffect> = effects.receiveAsFlow()

    /** Traite une action de l'utilisateur. */
    fun onEvent(event: PermissionRationaleEvent) {
        when (event) {
            PermissionRationaleEvent.Continue -> effects.trySend(PermissionRationaleEffect.RequestMediaAccess)
        }
    }
}
