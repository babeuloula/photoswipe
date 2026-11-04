package fr.babeuloula.photoswipe.ui.deck

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.babeuloula.photoswipe.domain.usecase.GetDeckForDay
import fr.babeuloula.photoswipe.ui.navigation.DeckRoute
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** État de la pile de swipe (SPEC §4.3). */
sealed interface DeckUiState {
    /** Paquet en cours de chargement. */
    data object Loading : DeckUiState

    /** La galerie ou la base n'a pas pu être lue. */
    data object Error : DeckUiState

    /** Plus rien à trier pour ce jour. */
    data object Empty : DeckUiState

    /**
     * Paquet chargé.
     *
     * @property cards médias encore à trier, dans l'ordre de la pile ; jamais vide.
     */
    data class Loaded(
        val cards: List<MediaCardUiState>,
    ) : DeckUiState {
        init {
            require(cards.isNotEmpty()) { "a loaded deck has at least one card" }
        }

        /** Carte du dessus, celle que l'utilisateur trie. */
        val current: MediaCardUiState get() = cards.first()
    }
}

/** Action de l'utilisateur sur la pile. */
sealed interface DeckEvent {
    /** « Réessayer » après une erreur. */
    data object Retry : DeckEvent

    /** « Terminer » : passer au résumé. */
    data object Finish : DeckEvent
}

/** Navigation demandée par la pile. */
sealed interface DeckEffect {
    /** Ouvrir le résumé de la session (SPEC §3.5). */
    data object OpenSummary : DeckEffect
}

/**
 * ViewModel de la pile d'un jour : charge une fois le paquet du jour de la route (ADR 0009) et
 * prépare la carte de chaque média. Les gestes de tri arrivent avec les tickets suivants.
 */
@HiltViewModel
class DeckViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val getDeckForDay: GetDeckForDay,
        private val clock: Clock,
    ) : ViewModel() {
        /** Jour cible de la pile. */
        val day: LocalDate = savedStateHandle.toRoute<DeckRoute>().day

        private val state = MutableStateFlow<DeckUiState>(DeckUiState.Loading)
        private val effects = Channel<DeckEffect>(Channel.BUFFERED)

        /** État de la pile. */
        val uiState: StateFlow<DeckUiState> = state.asStateFlow()

        /** Navigations à exécuter, chacune livrée une seule fois. */
        val uiEffects: Flow<DeckEffect> = effects.receiveAsFlow()

        init {
            load()
        }

        /** Traite une action de l'utilisateur. */
        fun onEvent(event: DeckEvent) {
            when (event) {
                DeckEvent.Retry -> load()
                DeckEvent.Finish -> effects.trySend(DeckEffect.OpenSummary)
            }
        }

        private fun load() {
            state.value = DeckUiState.Loading
            viewModelScope.launch {
                state.value =
                    runCatching { getDeckForDay(day) }.fold(
                        onSuccess = { deck ->
                            if (deck.isEmpty()) {
                                DeckUiState.Empty
                            } else {
                                DeckUiState.Loaded(deck.map { MediaCardUiState.of(it, day, clock.zone) })
                            }
                        },
                        onFailure = { DeckUiState.Error },
                    )
            }
        }
    }
