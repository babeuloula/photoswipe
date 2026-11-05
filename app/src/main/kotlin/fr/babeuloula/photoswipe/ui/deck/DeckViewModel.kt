package fr.babeuloula.photoswipe.ui.deck

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.ReviewSession
import fr.babeuloula.photoswipe.domain.model.SessionDecision
import fr.babeuloula.photoswipe.domain.usecase.GetDeckForDay
import fr.babeuloula.photoswipe.domain.usecase.KeepMedia
import fr.babeuloula.photoswipe.domain.usecase.MarkForDeletion
import fr.babeuloula.photoswipe.domain.usecase.UndoLastDecision
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

    /**
     * Plus rien à trier pour ce jour.
     *
     * @property canUndo vrai si la dernière décision de la session peut être annulée.
     */
    data class Empty(
        val canUndo: Boolean = false,
    ) : DeckUiState

    /**
     * Paquet chargé.
     *
     * @property cards médias encore à trier, dans l'ordre de la pile ; jamais vide.
     * @property canUndo vrai si la dernière décision de la session peut être annulée.
     * @property returningFrom côté d'où revient la carte du dessus après un undo : 1 pour la droite
     * (elle avait été gardée), -1 pour la gauche (marquée) ; 0 si elle ne revient pas.
     */
    data class Loaded(
        val cards: List<MediaCardUiState>,
        val canUndo: Boolean = false,
        val returningFrom: Float = 0f,
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

    /** Carte du dessus glissée à gauche : la marquer pour suppression, sans rien supprimer (SPEC §2). */
    data object MarkForDeletion : DeckEvent

    /** « Annuler » : remettre sur la pile le dernier média trié (SPEC §3.4). */
    data object Undo : DeckEvent
}

/** Navigation demandée par la pile. */
sealed interface DeckEffect {
    /** Ouvrir le résumé de la session (SPEC §3.5). */
    data object OpenSummary : DeckEffect
}

/**
 * ViewModel de la pile d'un jour : charge une fois le paquet du jour de la route (ADR 0009), puis
 * retire lui-même chaque carte triée et enregistre la décision. Les décisions de la session sont
 * gardées dans une [ReviewSession] : « Annuler » remet la dernière carte triée sur la pile.
 */
@HiltViewModel
class DeckViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val getDeckForDay: GetDeckForDay,
        private val keepMedia: KeepMedia,
        private val markForDeletion: MarkForDeletion,
        private val undoLastDecision: UndoLastDecision,
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

        /** Côté de sortie de chaque décision de [session], dans le même ordre : l'undo la fait revenir de là. */
        private var exitSides: List<Float> = emptyList()

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
                DeckEvent.Keep -> decideTopCard(KEEP_SIDE, keepMedia::invoke)
                DeckEvent.MarkForDeletion -> decideTopCard(DELETE_SIDE, markForDeletion::invoke)
                DeckEvent.Undo -> undo()
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
         * Applique la décision [decide] (garder, marquer) à la carte du dessus : elle quitte la pile
         * tout de suite, sans attendre l'écriture, pour que la carte suivante apparaisse sous le
         * doigt. Si l'écriture échoue, la carte revient sur le dessus de la pile, rien n'est perdu.
         * Un marquage n'est qu'une décision en base : rien n'est supprimé avant la confirmation
         * du résumé (SPEC §3.5).
         */
        private fun decideTopCard(
            side: Float,
            decide: suspend (MediaItem, LocalDate) -> SessionDecision,
        ) {
            val media = deck.firstOrNull() ?: return
            show(deck.drop(1))
            viewModelScope.launch {
                runCatching { decide(media, day) }.fold(
                    onSuccess = {
                        session = session.record(it)
                        exitSides = exitSides + side
                        show(deck)
                    },
                    onFailure = { show(listOf(media) + deck) },
                )
            }
        }

        /**
         * Annule la dernière décision : l'entrée d'avant est restaurée en base et le média revient sur
         * le dessus de la pile, du côté par lequel il était parti. Une décision déjà confirmée ou une
         * écriture impossible laissent la pile telle quelle.
         */
        private fun undo() {
            if (session.isEmpty) return
            viewModelScope.launch {
                val result = runCatching { undoLastDecision(session) }.getOrNull()
                if (result is UndoLastDecision.Result.Undone) {
                    val side = exitSides.last()
                    session = result.session
                    exitSides = exitSides.dropLast(1)
                    show(listOf(result.decision.media) + deck, returningFrom = side)
                }
            }
        }

        private fun show(
            media: List<MediaItem>,
            returningFrom: Float = 0f,
        ) {
            deck = media
            val canUndo = !session.isEmpty
            state.value =
                if (media.isEmpty()) {
                    DeckUiState.Empty(canUndo)
                } else {
                    DeckUiState.Loaded(
                        cards = media.map { MediaCardUiState.of(it, day, clock.zone) },
                        canUndo = canUndo,
                        returningFrom = returningFrom,
                    )
                }
        }

        private companion object {
            /** Côté de sortie d'une carte gardée. */
            const val KEEP_SIDE = 1f

            /** Côté de sortie d'une carte marquée pour suppression. */
            const val DELETE_SIDE = -1f
        }
    }
