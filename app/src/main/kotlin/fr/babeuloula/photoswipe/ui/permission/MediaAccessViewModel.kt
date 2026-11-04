package fr.babeuloula.photoswipe.ui.permission

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.babeuloula.photoswipe.domain.model.MediaAccess
import fr.babeuloula.photoswipe.permission.MediaAccessReader
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

/** Étape du parcours d'accès aux médias (SPEC §4.1). */
sealed interface MediaAccessUiState {
    /** Explication avant la demande système. */
    data object Rationale : MediaAccessUiState

    /**
     * Accès refusé.
     *
     * @property canAskAgain vrai si Android affichera encore sa demande ; sinon, seuls les
     * réglages système permettent d'accorder l'accès.
     */
    data class Denied(
        val canAskAgain: Boolean,
    ) : MediaAccessUiState

    /** Accès accordé, complet ou partiel : l'application peut s'afficher. */
    data object Granted : MediaAccessUiState
}

/** Action de l'utilisateur ou du système sur le parcours d'accès. */
sealed interface MediaAccessEvent {
    /** « Continuer » sur l'explication, ou « Réessayer » après un refus. */
    data object RequestAccess : MediaAccessEvent

    /**
     * Réponse à la demande système.
     *
     * @property canAskAgain ce que dit `shouldShowRequestPermissionRationale` après la réponse.
     */
    data class RequestAnswered(
        val canAskAgain: Boolean,
    ) : MediaAccessEvent

    /** « Ouvrir les réglages » après un refus. */
    data object OpenSettings : MediaAccessEvent

    /** L'application revient au premier plan : l'accès a pu changer depuis les réglages. */
    data object Resumed : MediaAccessEvent
}

/** Effet ponctuel que l'écran exécute. */
sealed interface MediaAccessEffect {
    /** Afficher la demande système de `READ_MEDIA_IMAGES` et `READ_MEDIA_VIDEO`. */
    data object LaunchPermissionRequest : MediaAccessEffect

    /** Ouvrir la page de l'application dans les réglages système. */
    data object OpenAppSettings : MediaAccessEffect
}

/**
 * Parcours d'accès aux photos et vidéos (SPEC §4.1 et §14) : explication, demande système, refus
 * avec chemin vers les réglages, et retour à l'explication si l'accès est retiré en cours d'usage.
 *
 * L'état se déduit toujours de l'accès réel lu par [MediaAccessReader] : il est relu à chaque
 * réponse et à chaque retour au premier plan. L'accès partiel (Android 14+) compte comme accordé ;
 * sa bannière arrive avec PS-34.
 */
@HiltViewModel
class MediaAccessViewModel
    @Inject
    constructor(
        private val accessReader: MediaAccessReader,
    ) : ViewModel() {
        private val state =
            MutableStateFlow(if (hasAccess()) MediaAccessUiState.Granted else MediaAccessUiState.Rationale)
        private val effects = Channel<MediaAccessEffect>(Channel.BUFFERED)

        /** Étape actuelle. */
        val uiState: StateFlow<MediaAccessUiState> = state.asStateFlow()

        /** Effets à exécuter, chacun livré une seule fois. */
        val uiEffects: Flow<MediaAccessEffect> = effects.receiveAsFlow()

        /** Traite une action de l'utilisateur ou du système. */
        fun onEvent(event: MediaAccessEvent) {
            when (event) {
                MediaAccessEvent.RequestAccess -> effects.trySend(MediaAccessEffect.LaunchPermissionRequest)
                is MediaAccessEvent.RequestAnswered -> state.value = afterAnswer(event.canAskAgain)
                MediaAccessEvent.OpenSettings -> effects.trySend(MediaAccessEffect.OpenAppSettings)
                MediaAccessEvent.Resumed -> state.value = afterResume(state.value)
            }
        }

        /** Étape après la réponse à la demande système. */
        private fun afterAnswer(canAskAgain: Boolean): MediaAccessUiState =
            if (hasAccess()) MediaAccessUiState.Granted else MediaAccessUiState.Denied(canAskAgain)

        /** Étape après un retour au premier plan, depuis l'étape [current]. */
        private fun afterResume(current: MediaAccessUiState): MediaAccessUiState =
            when {
                hasAccess() -> MediaAccessUiState.Granted
                current == MediaAccessUiState.Granted -> MediaAccessUiState.Rationale
                else -> current
            }

        private fun hasAccess(): Boolean = accessReader.currentAccess() != MediaAccess.NONE
    }
