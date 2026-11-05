package fr.babeuloula.photoswipe.ui.deck

import org.junit.Assert.assertEquals
import org.junit.Test

/** Seuil partagé par les deux sens du geste, et intensité de l'indicateur. */
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

    @Test
    fun `grows the indicator with the distance`() {
        assertEquals(0f, swipeProgress(0f, width))
        assertEquals(0.5f, swipeProgress(width * SWIPE_THRESHOLD / 2, width), DELTA)
        assertEquals(0.5f, swipeProgress(-width * SWIPE_THRESHOLD / 2, width), DELTA)
    }

    @Test
    fun `caps the indicator once the threshold is reached`() {
        assertEquals(1f, swipeProgress(width * SWIPE_THRESHOLD, width), DELTA)
        assertEquals(1f, swipeProgress(-width, width))
    }

    @Test
    fun `shows no indicator before the card is measured`() {
        assertEquals(0f, swipeProgress(100f, 0f))
    }

    private companion object {
        const val DELTA = 0.0001f
    }
}
