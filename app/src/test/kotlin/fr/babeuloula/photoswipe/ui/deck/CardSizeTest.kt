package fr.babeuloula.photoswipe.ui.deck

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Taille de la carte à la forme de la photo (PS-142). */
class CardSizeTest {
    private val maxWidth = 380.dp
    private val maxHeight = 600.dp

    @Test
    fun `takes the whole space while the proportions are unknown`() {
        assertEquals(DpSize(maxWidth, maxHeight), fittedCardSize(maxWidth, maxHeight, null))
    }

    @Test
    fun `fits a landscape photo to the width, without any band`() {
        val size = fittedCardSize(maxWidth, maxHeight, 16f / 9f)

        assertEquals(maxWidth, size.width)
        assertEquals(213.75f, size.height.value, 0.01f)
        assertFalse(fillsByCropping(size, 16f / 9f))
    }

    @Test
    fun `fits a tall portrait photo to the height`() {
        val size = fittedCardSize(maxWidth, maxHeight, 0.5f)

        assertEquals(DpSize(300.dp, maxHeight), size)
        assertFalse(fillsByCropping(size, 0.5f))
    }

    @Test
    fun `keeps a minimum height for a panorama and crops it to fill the card`() {
        val size = fittedCardSize(maxWidth, maxHeight, 3f)

        assertEquals(DpSize(maxWidth, MIN_CARD_HEIGHT), size)
        assertTrue(fillsByCropping(size, 3f))
    }

    @Test
    fun `keeps a minimum width for a very narrow photo`() {
        val size = fittedCardSize(maxWidth, maxHeight, 0.2f)

        assertEquals(DpSize(MIN_CARD_WIDTH, maxHeight), size)
        assertTrue(fillsByCropping(size, 0.2f))
    }

    @Test
    fun `never grows beyond a space smaller than the minimums`() {
        assertEquals(DpSize(100.dp, 50.dp), fittedCardSize(100.dp, 50.dp, 3f))
    }
}
