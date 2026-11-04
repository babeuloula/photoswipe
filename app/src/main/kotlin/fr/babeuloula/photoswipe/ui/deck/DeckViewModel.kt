package fr.babeuloula.photoswipe.ui.deck

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.ReviewSession
import fr.babeuloula.photoswipe.domain.usecase.GetDeckForDay
import fr.babeuloula.photoswipe.domain.usecase.KeepMedia
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

    /** Carte du dessus glissée à droite : la garder (SPEC §2). */
    data object Keep : DeckEvent
}

/** Navigation demandée par la pile. */
sealed interface DeckEffect {
    /** Ouvrir le résumé de la session (SPEC §3.5). */
    data object OpenSummary : DeckEffect
}

/**
 * ViewModel de la pile d'un jour : charge une fois le paquet du jour de la route (ADR 0009), puis
 * retire lui-même chaque carte triée et enregistre la décision. Les décisions de la session sont
 * gardées dans une [ReviewSession], pour l'undo.
 */
@HiltViewModel
class DeckViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val getDeckForDay: GetDeckForDay,
        private val keepMedia: KeepMedia,
        private val clock: Clock,
    ) : ViewModel() {
        /** Jour cible de la pile. */
        val day: LocalDate = savedStateHandle.toRoute<DeckRoute>().day

        private val state = MutableStateFlow<DeckUiState>(DeckUiState.Loading)
        private val effects = Channel<DeckEffect>(Channel.BUFFERED)

        /** Médias encore à trier, dans l'ordre de la pile ; vide tant que le paquet n'est pas chargé. */
        private var deck: List<MediaItem> = emptyList()

        /** Décisions de la session en cours, de la plus ancienne à la plus récente. */
        var session: ReviewSession = ReviewSession()
            private set

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
                DeckEvent.Keep -> keepTopCard()
            }
        }

        private fun load() {
            state.value = DeckUiState.Loading
            viewModelScope.launch {
                runCatching { getDeckForDay(day) }.fold(
                    onSuccess = { show(it) },
                    onFailure = { state.value = DeckUiState.Error },
                )
            }
        }

        /**
         * Garde la carte du dessus : elle quitte la pile tout de suite, sans attendre l'écriture,
         * pour que la carte suivante apparaisse sous le doigt. Si l'écriture échoue, la carte revient
         * sur le dessus de la pile, rien n'est perdu.
         */
        private fun keepTopCard() {
            val media = deck.firstOrNull() ?: return
            show(deck.drop(1))
            viewModelScope.launch {
                runCatching { keepMedia(media, day) }.fold(
                    onSuccess = { session = session.record(it) },
                    onFailure = { show(listOf(media) + deck) },
                )
            }
        }

        private fun show(media: List<MediaItem>) {
            deck = media
            state.value =
                if (media.isEmpty()) {
                    DeckUiState.Empty
                } else {
                    DeckUiState.Loaded(media.map { MediaCardUiState.of(it, day, clock.zone) })
                }
        }
    }
