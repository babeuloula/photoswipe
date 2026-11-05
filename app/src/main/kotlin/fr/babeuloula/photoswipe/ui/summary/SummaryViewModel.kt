package fr.babeuloula.photoswipe.ui.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.babeuloula.photoswipe.domain.model.ReviewSession
import fr.babeuloula.photoswipe.domain.model.SessionSummary
import fr.babeuloula.photoswipe.domain.usecase.GetSessionSummary
import fr.babeuloula.photoswipe.ui.navigation.SummaryRoute
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** État du résumé (SPEC §3.5). */
sealed interface SummaryUiState {
    /** Résumé en cours de calcul. */
    data object Loading : SummaryUiState

    /** La base n'a pas pu être lue. */
    data object Error : SummaryUiState

    /**
     * Résumé calculé.
     *
     * @property summary gardés de la session ; marqués, favoris et espace libéré de toutes les
     * demandes en attente (ADR 0011).
     */
    data class Loaded(
        val summary: SessionSummary,
    ) : SummaryUiState
}

/** Action de l'utilisateur sur le résumé. */
sealed interface SummaryEvent {
    /** « Réessayer » après une erreur. */
    data object Retry : SummaryEvent

    /** « Plus tard » : revenir à l'accueil, les marquages restent en attente. */
    data object Later : SummaryEvent

    /** Flèche de retour. */
    data object Back : SummaryEvent
}

/** Navigation demandée par le résumé. */
sealed interface SummaryEffect {
    /** Revenir à l'accueil. */
    data object GoHome : SummaryEffect

    /** Revenir à l'écran précédent. */
    data object Close : SummaryEffect
}

/**
 * ViewModel du résumé : les gardés viennent de la route (la session de la pile, ADR 0017), le
 * reste des demandes en attente en base, par `GetSessionSummary` (ADR 0011).
 */
@HiltViewModel
class SummaryViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val getSessionSummary: GetSessionSummary,
    ) : ViewModel() {
        /** Médias gardés pendant la session qui mène au résumé. */
        private val keptCount: Int = savedStateHandle.toRoute<SummaryRoute>().keptCount

        private val state = MutableStateFlow<SummaryUiState>(SummaryUiState.Loading)
        private val effects = Channel<SummaryEffect>(Channel.BUFFERED)

        /** État du résumé. */
        val uiState: StateFlow<SummaryUiState> = state.asStateFlow()

        /** Navigations à exécuter, chacune livrée une seule fois. */
        val uiEffects: Flow<SummaryEffect> = effects.receiveAsFlow()

        init {
            load()
        }

        /** Traite une action de l'utilisateur. */
        fun onEvent(event: SummaryEvent) {
            when (event) {
                SummaryEvent.Retry -> load()
                SummaryEvent.Later -> effects.trySend(SummaryEffect.GoHome)
                SummaryEvent.Back -> effects.trySend(SummaryEffect.Close)
            }
        }

        private fun load() {
            state.value = SummaryUiState.Loading
            viewModelScope.launch {
                state.value =
                    runCatching { getSessionSummary(ReviewSession()) }.fold(
                        onSuccess = { SummaryUiState.Loaded(it.copy(keptCount = keptCount)) },
                        onFailure = { SummaryUiState.Error },
                    )
            }
        }
    }
