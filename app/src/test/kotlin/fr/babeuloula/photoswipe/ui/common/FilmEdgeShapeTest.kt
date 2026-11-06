package fr.babeuloula.photoswipe.ui.common

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Forme du repère de bord de film : coins arrondis, coin du haut en fin de ligne coupé. */
@RunWith(RobolectricTestRunner::class)
class FilmEdgeShapeTest {
    private val size = Size(200f, 32f)
    private val density = Density(1f)

    private fun path(direction: LayoutDirection): Path {
        val outline = FilmEdgeShape(cornerRadius = 12.dp, cut = 10.dp).createOutline(size, direction, density)
        return (outline as Outline.Generic).path
    }

    /** Vrai si le point [at] est dans [path] : l'intersection avec un petit carré n'est pas vide. */
    private fun contains(
        path: Path,
        at: Offset,
    ): Boolean {
        val probe = Path().apply { addRect(Rect(at, Size(0.5f, 0.5f))) }
        return !Path.combine(PathOperation.Intersect, path, probe).isEmpty
    }

    @Test
    fun `cuts the top end corner and keeps the rest of the label`() {
        val path = path(LayoutDirection.Ltr)

        assertEquals(Rect(0f, 0f, 200f, 32f), path.getBounds())
        assertFalse(contains(path, Offset(198f, 1f)))
        assertTrue(contains(path, Offset(100f, 16f)))
        assertTrue(contains(path, Offset(4f, 4f)) || contains(path, Offset(12f, 1f)))
    }

    @Test
    fun `cuts the top left corner in a right to left language`() {
        val path = path(LayoutDirection.Rtl)

        assertFalse(contains(path, Offset(1f, 1f)))
        assertTrue(contains(path, Offset(190f, 16f)))
    }
}
