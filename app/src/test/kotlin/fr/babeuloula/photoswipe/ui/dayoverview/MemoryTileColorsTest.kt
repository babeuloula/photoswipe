package fr.babeuloula.photoswipe.ui.dayoverview

import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import coil3.ImageLoader
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.model.DayOverview
import fr.babeuloula.photoswipe.domain.model.MediaType
import fr.babeuloula.photoswipe.domain.model.OverviewMemory
import fr.babeuloula.photoswipe.domain.model.OverviewYear
import fr.babeuloula.photoswipe.domain.model.ReviewDecision
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import fr.babeuloula.photoswipe.ui.theme.swipeColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import kotlin.math.abs

/**
 * Couleur des icônes des pastilles de la vue d'ensemble (PS-191) : chaque icône prend la couleur
 * de contenu de sa pastille, et non celle de l'écran, blanche en thème sombre sur un fond clair.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, qualifiers = "xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MemoryTileColorsTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val day = LocalDate.of(2026, 5, 14)

    /** Couleur de contenu de l'écran, claire en thème sombre : celle que l'icône ne doit pas prendre. */
    private var screenContent = Color.Unspecified

    /** Affiche une seule case en thème sombre et renvoie la couleur attendue de son icône. */
    private fun showDark(
        memory: OverviewMemory,
        iconColor: @Composable () -> Color,
    ): Color {
        var expected = Color.Unspecified
        composeRule.activity.setContent {
            CompositionLocalProvider(LocalImageLoader provides ImageLoader(composeRule.activity)) {
                PhotoSwipeTheme(darkTheme = true) {
                    expected = iconColor()
                    screenContent = MaterialTheme.colorScheme.onSurface
                    DayOverviewScreen(
                        day = day,
                        state = DayOverviewUiState.Loaded(DayOverview(day, listOf(OverviewYear(2024, listOf(memory))))),
                        onEvent = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
        return expected
    }

    /** Nombre de pixels de la case proches de [color]. */
    private fun pixelsOf(
        color: Color,
        tag: String,
    ): Int {
        val pixels = composeRule.onNodeWithTag(tag).captureToImage().toPixelMap()
        var count = 0
        for (x in 0 until pixels.width) {
            for (y in 0 until pixels.height) {
                val pixel = pixels[x, y]
                if (abs(pixel.red - color.red) < TOLERANCE &&
                    abs(pixel.green - color.green) < TOLERANCE &&
                    abs(pixel.blue - color.blue) < TOLERANCE
                ) {
                    count++
                }
            }
        }
        return count
    }

    @Test
    fun `draws the play icon of a video dark on its light badge in the dark theme`() {
        val video = OverviewMemory(aMediaItem(id = 1, type = MediaType.VIDEO), decision = null, favorite = false)
        val expected = showDark(video) { MaterialTheme.colorScheme.onPrimaryFixed }

        val count = pixelsOf(expected, "${MEMORY_TILE_TAG}1")
        assertTrue("the play icon has $count pixels in onPrimaryFixed", count >= MIN_ICON_PIXELS)
        assertEquals(0, pixelsOf(screenContent, "${MEMORY_TILE_TAG}1"))
    }

    @Test
    fun `draws the check of a kept memory in the content color of its badge`() {
        val kept = OverviewMemory(aMediaItem(id = 1), ReviewDecision.KEPT, favorite = false)
        val expected = showDark(kept) { MaterialTheme.swipeColors.onKeepContainer }

        val count = pixelsOf(expected, "${MEMORY_TILE_TAG}1")
        assertTrue("the check has $count pixels in onKeepContainer", count >= MIN_ICON_PIXELS)
        assertEquals(0, pixelsOf(screenContent, "${MEMORY_TILE_TAG}1"))
    }

    private companion object {
        /** Écart toléré par canal, l'anticrénelage mêlant l'icône à sa pastille. */
        const val TOLERANCE = 0.04f

        /** Pixels d'une icône de 14 dp entièrement dessinée dans sa couleur, au minimum. */
        const val MIN_ICON_PIXELS = 20
    }
}
