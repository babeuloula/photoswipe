package fr.babeuloula.photoswipe.ui.postcard

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

/** Carte postale d'une photo (PS-177) : taille bornée, proportions, repère et mention. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PostcardRendererTest {
    private val style = PostcardStyle.of(RuntimeEnvironment.getApplication())

    private fun photo(
        width: Int,
        height: Int,
    ): Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }

    private fun render(
        width: Int,
        height: Int,
        brand: String? = "PhotoSwipe",
    ): Bitmap = renderPostcard(photo(width, height), "5 years ago · Nov 6, 2021", brand, style)

    @Test
    fun `keeps the proportions of a landscape photo, at a bounded size`() {
        val card = render(4000, 3000)
        val layout = PostcardLayout.of(4000, 3000)

        assertEquals(layout.width, card.width)
        assertEquals(layout.height, card.height)
        assertEquals(PHOTO_LONG_SIDE, layout.photo.width())
        assertEquals(4f / 3f, layout.photo.width().toFloat() / layout.photo.height(), 0.01f)
    }

    @Test
    fun `keeps the proportions of a portrait photo`() {
        val layout = PostcardLayout.of(3000, 4000)
        val card = render(3000, 4000)

        assertEquals(PHOTO_LONG_SIDE, layout.photo.height())
        assertEquals(3f / 4f, layout.photo.width().toFloat() / layout.photo.height(), 0.01f)
        assertEquals(layout.width to layout.height, card.width to card.height)
        assertTrue(card.height > card.width)
    }

    @Test
    fun `crops a panorama beyond two to one, centered`() {
        val layout = PostcardLayout.of(8000, 1000)

        assertEquals(MAX_RATIO, layout.photo.width().toFloat() / layout.photo.height(), 0.01f)
        val crop = postcardCrop(8000, 1000)
        assertEquals(2000, crop.width())
        assertEquals(1000, crop.height())
        assertEquals(3000, crop.left)
    }

    @Test
    fun `crops a very tall screenshot to nine by sixteen`() {
        val layout = PostcardLayout.of(1000, 4000)

        assertEquals(MIN_RATIO, layout.photo.width().toFloat() / layout.photo.height(), 0.01f)
    }

    @Test
    fun `never makes a huge photo bigger than its bounded size`() {
        val small = PostcardLayout.of(400, 300)
        val huge = PostcardLayout.of(12000, 9000)

        assertEquals(small, huge)
    }

    @Test
    fun `draws the photo inside a paper frame, and the film edge label under it`() {
        val card = render(1600, 1200)
        val layout = PostcardLayout.of(1600, 1200)

        assertEquals(style.paper, card.getPixel(layout.margin / 2, layout.margin / 2))
        assertEquals(Color.BLUE, card.getPixel(layout.photo.centerX(), layout.photo.centerY()))
        // Le repère, brun du bord de film, au début du pied de carte.
        assertEquals(style.edge, card.getPixel(layout.footer.left + 4, layout.footer.centerY()))
    }

    @Test
    fun `writes the PhotoSwipe mention only when asked`() {
        val layout = PostcardLayout.of(1600, 1200)

        assertTrue(inkInBrandArea(render(1600, 1200), layout) > 0)
        assertEquals(0, inkInBrandArea(render(1600, 1200, brand = null), layout))
    }

    /** Pixels qui ne sont pas du papier dans le dernier cinquième du pied de carte, où se pose la mention. */
    private fun inkInBrandArea(
        card: Bitmap,
        layout: PostcardLayout,
    ): Int {
        val footer = layout.footer
        return (footer.right - footer.width() / 5 until footer.right).sumOf { x ->
            (footer.top until footer.bottom).count { y -> !card.getPixel(x, y).isCloseTo(style.paper) }
        }
    }

    private fun Int.isCloseTo(other: Int): Boolean =
        abs(Color.red(this) - Color.red(other)) < 8 &&
            abs(Color.green(this) - Color.green(other)) < 8 &&
            abs(Color.blue(this) - Color.blue(other)) < 8
}
