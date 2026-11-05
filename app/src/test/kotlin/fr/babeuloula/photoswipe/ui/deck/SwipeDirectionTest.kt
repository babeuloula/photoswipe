package fr.babeuloula.photoswipe.ui.deck

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Cible d'un geste (seuils partagés par la gauche et la droite, menu vers le haut), et intensité de l'indicateur. */
class SwipeDirectionTest {
    private val width = 1000f
    private val size = Offset(width, 2000f)

    @Test
    fun `goes right past the threshold`() {
        assertEquals(SwipeTarget.RIGHT, swipeTarget(Offset(width * SWIPE_THRESHOLD + 1, 0f), size))
    }

    @Test
    fun `goes left past the same threshold`() {
        assertEquals(SwipeTarget.LEFT, swipeTarget(Offset(-width * SWIPE_THRESHOLD - 1, 0f), size))
    }

    @Test
    fun `stays put within the threshold on both sides`() {
        assertNull(swipeTarget(Offset(width * SWIPE_THRESHOLD, 0f), size))
        assertNull(swipeTarget(Offset(-width * SWIPE_THRESHOLD, 0f), size))
        assertNull(swipeTarget(Offset.Zero, size))
    }

    @Test
    fun `opens the menu when mostly swiped up past its threshold`() {
        assertEquals(SwipeTarget.MENU, swipeTarget(Offset(100f, -size.y * MENU_THRESHOLD - 1), size))
    }

    @Test
    fun `does nothing on a short swipe up or any swipe down`() {
        assertNull(swipeTarget(Offset(0f, -size.y * MENU_THRESHOLD / 2), size))
        assertNull(swipeTarget(Offset(0f, size.y), size))
    }

    @Test
    fun `follows the dominant direction of a diagonal swipe`() {
        assertEquals(SwipeTarget.RIGHT, swipeTarget(Offset(width * 0.6f, -width * 0.5f), size))
        assertEquals(SwipeTarget.MENU, swipeTarget(Offset(width * 0.4f, -width * 0.5f), size))
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
