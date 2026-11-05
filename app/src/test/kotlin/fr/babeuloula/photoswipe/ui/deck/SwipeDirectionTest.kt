package fr.babeuloula.photoswipe.ui.deck

import org.junit.Assert.assertEquals
import org.junit.Test

/** Seuil partagé par les deux sens du geste. */
class SwipeDirectionTest {
    private val width = 1000f

    @Test
    fun `goes right past the threshold`() {
        assertEquals(1f, swipeDirection(width * SWIPE_THRESHOLD + 1, width))
    }

    @Test
    fun `goes left past the same threshold`() {
        assertEquals(-1f, swipeDirection(-width * SWIPE_THRESHOLD - 1, width))
    }

    @Test
    fun `stays put within the threshold on both sides`() {
        assertEquals(0f, swipeDirection(width * SWIPE_THRESHOLD, width))
        assertEquals(0f, swipeDirection(-width * SWIPE_THRESHOLD, width))
        assertEquals(0f, swipeDirection(0f, width))
    }
}
