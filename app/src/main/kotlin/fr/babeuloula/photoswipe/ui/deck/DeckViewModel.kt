package fr.babeuloula.photoswipe.ui.deck

import android.content.IntentSender
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.MediaType
import fr.babeuloula.photoswipe.domain.model.ReviewDecision
import fr.babeuloula.photoswipe.domain.model.ReviewSession
import fr.babeuloula.photoswipe.domain.model.SessionDecision
import fr.babeuloula.photoswipe.domain.usecase.GetDaySession
import fr.babeuloula.photoswipe.domain.usecase.GetDeckForDay
import fr.babeuloula.photoswipe.domain.usecase.UndoLastDecision
import fr.babeuloula.photoswipe.ui.navigation.DeckRoute
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
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
     * @property cards cartes visibles, dans l'ordre de la pile : celle du dessus et, s'il y en a une,
     * la suivante, déjà composée dessous pour que son image soit chargée avant d'arriver (SPEC §14).
     * Jamais vide, jamais plus de [VISIBLE_CARDS] : une pile de plusieurs milliers de médias ne
     * prépare que deux cartes.
     * @property remainingCount médias encore à trier, carte du dessus comprise.
     * @property canUndo vrai si la dernière décision de la session peut être annulée.
     * @property returningFrom côté d'où revient la carte du dessus après un undo, celui par lequel
     * elle était sortie ; `null` si elle ne revient pas.
     * @property menuOpen vrai quand le menu favori / partage de la carte du dessus est ouvert.
     */
    data class Loaded(
        val cards: List<MediaCardUiState>,
        val remainingCount: Int = cards.size,
        val canUndo: Boolean = false,
        val returningFrom: CardSide? = null,
        val menuOpen: Boolean = false,
    ) : DeckUiState {
        init {
            require(cards.size in 1..VISIBLE_CARDS) { "a loaded deck shows 1 to $VISIBLE_CARDS cards: ${cards.size}" }
        }

        /** Carte du dessus, celle que l'utilisateur trie. */
        val current: MediaCardUiState get() = cards.first()

        /** Carte suivante, préparée sous celle du dessus ; `null` pour la dernière carte. */
        val next: MediaCardUiState? get() = cards.getOrNull(1)

        companion object {
            /** Cartes préparées à l'écran : celle du dessus et la suivante, pas plus. */
            const val VISIBLE_CARDS = 2
        }
    }
}

/** Action de l'utilisateur sur la pile. */
sealed interface DeckEvent {
    /** « Réessayer » après une erreur. */
    data object Retry : DeckEvent

    /** « Terminer » : passer au résumé. */
    data object Finish : DeckEvent

    /** Flèche de retour : revenir à l'accueil. */
    data object Back : DeckEvent

    /** Carte du dessus glissée à droite : la garder (SPEC §2). */
    data object Keep : DeckEvent

    /** Carte du dessus glissée à gauche : la marquer pour suppression, sans rien supprimer (SPEC §2). */
    data object MarkForDeletion : DeckEvent

    /** « Annuler » : remettre sur la pile le dernier média trié (SPEC §3.4). */
    data object Undo : DeckEvent

    /** Carte glissée vers le haut ou bouton « Menu » : ouvrir le menu favori / partage (SPEC §2). */
    data object OpenMenu : DeckEvent

    /** Menu fermé sans choix : la carte reste en place. */
    data object CloseMenu : DeckEvent

    /** « Favori » du menu : garder le média et demander aussitôt sa mise en favori à la galerie (SPEC §2). */
    data object Favorite : DeckEvent

    /**
     * Réponse à la demande de favori de la galerie (dialogue d'Android, ou accord immédiat avec
     * l'accès « Gestion des médias »).
     *
     * @property confirmed vrai si le favori a été accepté.
     */
    data class FavoriteRequestAnswered(
        val confirmed: Boolean,
    ) : DeckEvent

    /** « Partager » du menu : envoyer le média du dessus ailleurs, sans changer son état (SPEC §2). */
    data object Share : DeckEvent
}

/** Navigation demandée par la pile. */
sealed interface DeckEffect {
    /** Ouvrir le résumé (SPEC §3.5), qui lit lui-même le tri du jour (ADR 0024). */
    data object OpenSummary : DeckEffect

    /** Quitter la pile, vers l'écran précédent. */
    data object Close : DeckEffect

    /**
     * Ouvrir la feuille de partage d'Android pour un média.
     *
     * @property uri URI de contenu du média.
     * @property type photo ou vidéo, pour le type MIME annoncé aux autres applications.
     */
    data class Share(
        val uri: String,
        val type: MediaType,
    ) : DeckEffect

    /**
     * Lancer la demande de favori de la galerie ; la réponse revient par [DeckEvent.FavoriteRequestAnswered].
     *
     * @property intentSender demande système d'ajout aux favoris.
     */
    data class LaunchFavoriteRequest(
        val intentSender: IntentSender,
    ) : DeckEffect
}

/**
 * ViewModel de la pile d'un jour : charge une fois le paquet du jour de la route (ADR 0009), puis
 * retire lui-même chaque carte triée et enregistre la décision. Les décisions du jour sont
 * gardées dans une [ReviewSession], reprise du journal au chargement (`GetDaySession`, ADR 0019) :
 * « Annuler » remet la dernière carte triée sur la pile, même après être sorti de la pile ou après
 * la mort du process.
 */
@HiltViewModel
class DeckViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val getDeckForDay: GetDeckForDay,
        private val decisions: DeckDecisions,
        private val getDaySession: GetDaySession,
        private val clock: Clock,
        private val galleryFavorite: GalleryFavorite,
    ) : ViewModel() {
        /** Jour cible de la pile. */
        val day: LocalDate = savedStateHandle.toRoute<DeckRoute>().day

        private val state = MutableStateFlow<DeckUiState>(DeckUiState.Loading)
        private val effects = Channel<DeckEffect>(Channel.BUFFERED)

        /**
         * Médias encore à trier, dans l'ordre de la pile ; vide tant que le paquet n'est pas chargé.
         * File à double entrée : trier retire le dessus, annuler l'y remet, sans recopier la pile.
         */
        private val deck = ArrayDeque<MediaItem>()

        /** Décisions du jour encore annulables, de la plus ancienne à la plus récente. */
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
                DeckEvent.Retry -> {
                    load()
                }

                DeckEvent.Finish -> {
                    effects.trySend(DeckEffect.OpenSummary)
                }

                DeckEvent.Back -> {
                    effects.trySend(DeckEffect.Close)
                }

                DeckEvent.Keep -> {
                    decideTopCard(decisions.keepMedia::invoke)
                }

                DeckEvent.MarkForDeletion -> {
                    decideTopCard(decisions.markForDeletion::invoke)
                }

                DeckEvent.Favorite -> {
                    decideTopCard(decisions.favoriteMedia::invoke) { media ->
                        // Le favori est demandé aussitôt à la galerie (SPEC §2) ; sinon, il reste en attente.
                        galleryFavorite.request(media)?.let { effects.trySend(DeckEffect.LaunchFavoriteRequest(it)) }
                    }
                }

                is DeckEvent.FavoriteRequestAnswered -> {
                    viewModelScope.launch { if (galleryFavorite.answer(event.confirmed)) openSummaryWhenDone() }
                }

                DeckEvent.Undo -> {
                    undo()
                }

                DeckEvent.OpenMenu -> {
                    setMenuOpen(true)
                }

                DeckEvent.CloseMenu -> {
                    setMenuOpen(false)
                }

                DeckEvent.Share -> {
                    shareTopCard()
                }
            }
        }

        private fun load() {
            state.value = DeckUiState.Loading
            viewModelScope.launch {
                runCatching {
                    galleryFavorite.loadPending()
                    getDeckForDay(day) to getDaySession(day)
                }.fold(
                    onSuccess = { (cards, resumed) ->
                        deck.clear()
                        deck.addAll(cards)
                        session = resumed
                        render()
                    },
                    onFailure = { state.value = DeckUiState.Error },
                )
            }
        }

        /**
         * Applique la décision [decide] (garder, marquer) à la carte du dessus : elle quitte la pile
         * tout de suite, sans attendre l'écriture, pour que la carte suivante apparaisse sous le
         * doigt. Si l'écriture échoue, la carte revient sur le dessus de la pile, rien n'est perdu.
         * Un marquage n'est qu'une décision en base : rien n'est supprimé avant la confirmation
         * du résumé (SPEC §3.5). La dernière carte triée mène au résumé (SPEC §3.4).
         */
        private fun decideTopCard(
            decide: suspend (MediaItem, LocalDate) -> SessionDecision,
            afterDecision: (MediaItem) -> Unit = {},
        ) {
            val media = deck.removeFirstOrNull() ?: return
            render()
            viewModelScope.launch {
                runCatching { decide(media, day) }.fold(
                    onSuccess = {
                        session = session.record(it)
                        render()
                        afterDecision(media)
                        openSummaryWhenDone()
                    },
                    onFailure = {
                        deck.addFirst(media)
                        render()
                    },
                )
            }
        }

        /**
         * La dernière carte triée mène au résumé (SPEC §3.4), une fois la demande de favori traitée :
         * quitter la pile avant perdrait sa réponse.
         */
        private fun openSummaryWhenDone() {
            if (deck.isEmpty() && !galleryFavorite.isPending) effects.trySend(DeckEffect.OpenSummary)
        }

        /**
         * Annule la dernière décision : l'entrée d'avant est restaurée en base et le média revient sur
         * le dessus de la pile, du côté par lequel il était parti. Une décision déjà confirmée ou une
         * écriture impossible laissent la pile telle quelle.
         */
        private fun undo() {
            if (session.isEmpty) return
            viewModelScope.launch {
                val result = runCatching { decisions.undoLastDecision(session) }.getOrNull()
                if (result is UndoLastDecision.Result.Undone) {
                    session = result.session
                    if (result.decision.exitSide() ==
                        CardSide.TOP
                    ) {
                        galleryFavorite.forgetPending(result.decision.media.id)
                    }
                    deck.addFirst(result.decision.media)
                    render(returningFrom = result.decision.exitSide())
                }
            }
        }

        /**
         * Côté par lequel la carte est sortie, lu sur l'entrée écrite au swipe : marquée à gauche,
         * favori en haut, gardée à droite. L'undo la fait revenir de là.
         */
        private fun SessionDecision.exitSide(): CardSide =
            when {
                made.decision == ReviewDecision.MARKED_FOR_DELETION -> CardSide.LEFT
                made.favoritePending -> CardSide.TOP
                else -> CardSide.RIGHT
            }

        /** Ferme le menu et demande le partage du média du dessus, qui reste en place. */
        private fun shareTopCard() {
            val media = deck.firstOrNull() ?: return
            setMenuOpen(false)
            effects.trySend(DeckEffect.Share(media.uri, media.type))
        }

        /** Ouvre ou ferme le menu de la carte du dessus ; sans carte, il n'y a pas de menu. */
        private fun setMenuOpen(open: Boolean) {
            state.update { current -> if (current is DeckUiState.Loaded) current.copy(menuOpen = open) else current }
        }

        /** Publie l'état de la pile : seules les deux premières cartes sont préparées. */
        private fun render(returningFrom: CardSide? = null) {
            val canUndo = !session.isEmpty
            state.value =
                if (deck.isEmpty()) {
                    DeckUiState.Empty(canUndo)
                } else {
                    DeckUiState.Loaded(
                        cards =
                            deck
                                .take(
                                    DeckUiState.Loaded.VISIBLE_CARDS,
                                ).map { MediaCardUiState.of(it, day, clock.zone, galleryFavorite.isFavorite(it)) },
                        remainingCount = deck.size,
                        canUndo = canUndo,
                        returningFrom = returningFrom,
                    )
                }
        }
    }
