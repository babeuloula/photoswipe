package fr.babeuloula.photoswipe.ui.deck

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoomStateTest {
    private val zoom = ZoomState().apply { size = Size(1000f, 2000f) }
    private val center = Offset(500f, 1000f)

    @Test
    fun `starts on the whole photo`() {
        assertEquals(1f, zoom.scale)
        assertEquals(Offset.Zero, zoom.offset)
        assertFalse(zoom.zoomed)
    }

    @Test
    fun `zooms around the pinch point, which stays under the fingers`() {
        zoom.transform(centroid = Offset(700f, 1000f), pan = Offset.Zero, zoom = 2f)

        assertEquals(2f, zoom.scale)
        // Le point à 200 px à droite du centre reste sous les doigts : il passe à 400 px, d'où −200.
        assertEquals(Offset(-200f, 0f), zoom.offset)
    }

    @Test
    fun `never zooms out below the whole photo nor beyond five times`() {
        zoom.transform(center, Offset.Zero, 0.5f)
        assertEquals(1f, zoom.scale)

        zoom.transform(center, Offset.Zero, 20f)
        assertEquals(ZoomState.MAX_SCALE, zoom.scale)
    }

    @Test
    fun `keeps the zoomed photo over the whole view when panning`() {
        zoom.transform(center, Offset.Zero, 2f)

        zoom.transform(center, Offset(5000f, -5000f), 1f)

        assertEquals(Offset(500f, -1000f), zoom.offset)
    }

    @Test
    fun `does not move the whole photo`() {
        zoom.transform(center, Offset(300f, 300f), 1f)

        assertEquals(Offset.Zero, zoom.offset)
    }

    @Test
    fun `zooms on the double tapped point, then back to the whole photo`() {
        zoom.toggle(Offset(600f, 1000f))

        assertTrue(zoom.zoomed)
        assertEquals(ZoomState.DOUBLE_TAP_SCALE, zoom.scale)
        assertEquals(Offset(-150f, 0f), zoom.offset)

        zoom.toggle(Offset(600f, 1000f))

        assertEquals(1f, zoom.scale)
        assertEquals(Offset.Zero, zoom.offset)
    }
}
