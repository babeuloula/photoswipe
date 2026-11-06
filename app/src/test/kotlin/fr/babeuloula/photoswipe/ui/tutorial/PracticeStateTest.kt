package fr.babeuloula.photoswipe.ui.tutorial

import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.ui.deck.CardSide
import fr.babeuloula.photoswipe.ui.deck.DeckEvent
import fr.babeuloula.photoswipe.ui.deck.PREVIEW_CARD
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeStateTest {
    private val cards = (1L..3L).map { PREVIEW_CARD.copy(id = MediaId(it)) }
    private val start = PracticeState(remaining = cards)

    @Test
    fun `shows the first two sample cards, nothing to undo yet`() {
        val deck = start.deck!!

        assertEquals(cards.take(2), deck.cards)
        assertEquals(1, deck.position)
        assertEquals(3, deck.total)
        assertFalse(deck.canUndo)
    }

    @Test
    fun `keeping, marking and favoring move on to the next card with their feedback`() {
        val kept = start.after(DeckEvent.Keep)
        assertEquals(cards[1], kept.deck!!.current)
        assertEquals(PracticeFeedback.KEPT, kept.feedback)

        val marked = kept.after(DeckEvent.MarkForDeletion)
        assertEquals(PracticeFeedback.MARKED, marked.feedback)

        val favorite = marked.after(DeckEvent.OpenMenu).after(DeckEvent.CloseMenu).after(DeckEvent.Favorite)
        assertEquals(PracticeFeedback.FAVORITE, favorite.feedback)
        assertEquals(listOf(CardSide.RIGHT, CardSide.LEFT, CardSide.TOP), favorite.sorted.map { it.second })
        assertNull(favorite.deck)
    }

    @Test
    fun `undo brings the last card back from the side it left`() {
        val undone = start.after(DeckEvent.MarkForDeletion).after(DeckEvent.Undo)

        assertEquals(cards[0], undone.deck!!.current)
        assertEquals(CardSide.LEFT, undone.returningFrom)
        assertEquals(PracticeFeedback.UNDONE, undone.feedback)
        assertFalse(undone.deck!!.canUndo)
    }

    @Test
    fun `undo with nothing sorted changes nothing`() {
        assertEquals(start, start.after(DeckEvent.Undo))
    }

    @Test
    fun `the menu opens, and sharing closes it without sorting the card`() {
        val open = start.after(DeckEvent.OpenMenu)
        assertTrue(open.deck!!.menuOpen)

        val shared = open.after(DeckEvent.Share)

        assertFalse(shared.deck!!.menuOpen)
        assertEquals(cards[0], shared.deck!!.current)
        assertEquals(PracticeFeedback.SHARED, shared.feedback)
    }
}
