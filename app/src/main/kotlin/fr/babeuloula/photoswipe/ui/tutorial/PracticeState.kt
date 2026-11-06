package fr.babeuloula.photoswipe.ui.tutorial

import androidx.annotation.StringRes
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.ui.deck.CardSide
import fr.babeuloula.photoswipe.ui.deck.DeckEvent
import fr.babeuloula.photoswipe.ui.deck.DeckUiState
import fr.babeuloula.photoswipe.ui.deck.MediaCardUiState

/**
 * Ce que le mode entraînement dit de la dernière action, sous son titre.
 *
 * @property text phrase affichée.
 */
enum class PracticeFeedback(
    @param:StringRes val text: Int,
) {
    /** Carte gardée. */
    KEPT(R.string.tutorial_practice_kept),

    /** Carte marquée pour suppression. */
    MARKED(R.string.tutorial_practice_marked),

    /** Carte mise en favori. */
    FAVORITE(R.string.tutorial_practice_favorite),

    /** « Partager » : rien ne s'ouvre pendant l'entraînement. */
    SHARED(R.string.tutorial_practice_shared),

    /** Dernière décision annulée. */
    UNDONE(R.string.tutorial_practice_undone),
}

/**
 * Pile du mode entraînement du tutoriel (SPEC §4) : des cartes d'exemple, triées en mémoire
 * seulement. Aucune décision n'est enregistrée et aucun média de la galerie n'est lu : elle ne
 * connaît ni use case ni port.
 *
 * Elle reçoit les mêmes [DeckEvent] que la vraie pile, dont elle réutilise l'écran (`LoadedDeck`).
 *
 * @property remaining cartes encore à trier, celle du dessus en premier.
 * @property sorted cartes triées, la dernière en dernier, avec le côté par lequel elles sont sorties.
 * @property menuOpen vrai quand le menu favori / partage est ouvert.
 * @property returningFrom côté d'où revient la carte du dessus après un « Annuler ».
 * @property feedback ce qu'a fait la dernière action, `null` au départ.
 */
data class PracticeState(
    val remaining: List<MediaCardUiState>,
    val sorted: List<Pair<MediaCardUiState, CardSide>> = emptyList(),
    val menuOpen: Boolean = false,
    val returningFrom: CardSide? = null,
    val feedback: PracticeFeedback? = null,
) {
    /** Pile à afficher, ou `null` quand toutes les cartes sont triées. */
    val deck: DeckUiState.Loaded?
        get() =
            if (remaining.isEmpty()) {
                null
            } else {
                DeckUiState.Loaded(
                    cards = remaining.take(DeckUiState.Loaded.VISIBLE_CARDS),
                    remainingCount = remaining.size,
                    sortedCount = sorted.size,
                    canUndo = sorted.isNotEmpty(),
                    returningFrom = returningFrom,
                    menuOpen = menuOpen,
                )
            }

    /** État après [event] ; les événements propres à la vraie pile sont ignorés. */
    fun after(event: DeckEvent): PracticeState =
        when (event) {
            DeckEvent.Keep -> sort(CardSide.RIGHT, PracticeFeedback.KEPT)
            DeckEvent.MarkForDeletion -> sort(CardSide.LEFT, PracticeFeedback.MARKED)
            DeckEvent.Favorite -> sort(CardSide.TOP, PracticeFeedback.FAVORITE)
            DeckEvent.Undo -> undo()
            DeckEvent.OpenMenu -> copy(menuOpen = true)
            DeckEvent.CloseMenu -> copy(menuOpen = false)
            DeckEvent.Share -> copy(menuOpen = false, feedback = PracticeFeedback.SHARED)
            else -> this
        }

    private fun sort(
        side: CardSide,
        result: PracticeFeedback,
    ): PracticeState {
        val card = remaining.firstOrNull() ?: return this
        return PracticeState(
            remaining = remaining.drop(1),
            sorted = sorted + (card to side),
            feedback = result,
        )
    }

    private fun undo(): PracticeState {
        val (card, side) = sorted.lastOrNull() ?: return this
        return PracticeState(
            remaining = listOf(card) + remaining,
            sorted = sorted.dropLast(1),
            returningFrom = side,
            feedback = PracticeFeedback.UNDONE,
        )
    }
}
