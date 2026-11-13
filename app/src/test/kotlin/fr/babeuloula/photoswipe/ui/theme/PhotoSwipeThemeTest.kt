package fr.babeuloula.photoswipe.ui.theme

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/**
 * Vérifie les choix de couleurs, de typographie et de formes de `PhotoSwipeTheme`.
 *
 * Le thème est composé dans une `ComponentActivity` créée par Robolectric : ces tests lisent
 * seulement les valeurs fournies par `MaterialTheme`, sans interaction avec l'interface.
 */
@RunWith(RobolectricTestRunner::class)
class PhotoSwipeThemeTest {
    @Test
    fun `uses the light amber palette`() {
        val colors = colorSchemeOf { PhotoSwipeTheme(darkTheme = false, content = it) }

        assertEquals(LightColorScheme, colors)
    }

    @Test
    fun `uses the dark amber palette`() {
        val colors = colorSchemeOf { PhotoSwipeTheme(darkTheme = true, content = it) }

        assertEquals(DarkColorScheme, colors)
    }

    @Test
    fun `turns the dark backgrounds pure black, raised surfaces still apart from them`() {
        val colors = colorSchemeOf { PhotoSwipeTheme(darkTheme = true, pureBlack = true, content = it) }

        assertEquals(PureBlackColorScheme, colors)
        listOf(colors.background, colors.surface, colors.surfaceDim, colors.surfaceContainerLowest).forEach {
            assertEquals(Color.Black, it)
        }
        listOf(
            colors.surfaceContainerLow,
            colors.surfaceContainer,
            colors.surfaceContainerHigh,
            colors.surfaceContainerHighest,
        ).zipWithNext()
            .forEach { (lower, higher) -> assertNotEquals(lower, higher) }
        assertNotEquals(Color.Black, colors.surfaceContainerLow)
        assertEquals(DarkColorScheme.surfaceContainer, colors.surfaceContainer)
        assertEquals(DarkColorScheme.primary, colors.primary)
    }

    @Test
    fun `keeps the light palette when pure black is on`() {
        val colors = colorSchemeOf { PhotoSwipeTheme(darkTheme = false, pureBlack = true, content = it) }

        assertEquals(LightColorScheme, colors)
    }

    @Test
    fun `builds the fallback palettes from the amber seed, fixed roles included`() {
        assertEquals(Color(0xFF785A0B), LightColorScheme.primary)
        assertEquals(Color(0xFFEAC16C), DarkColorScheme.primary)
        assertEquals(Color(0xFFFFF8F2), LightColorScheme.surface)
        assertEquals(Color(0xFF15130F), DarkColorScheme.surface)
        listOf(LightColorScheme, DarkColorScheme).forEach {
            assertEquals(Color(0xFFEAC16C), it.primaryFixedDim)
            assertEquals(Color(0xFF261A00), it.onPrimaryFixed)
        }
    }

    @Test
    fun `ignores the wallpaper colors, even where Android offers them`() {
        lateinit var wallpaper: ColorScheme
        val colors =
            colorSchemeOf { content ->
                wallpaper = dynamicDarkColorScheme(LocalContext.current)
                PhotoSwipeTheme(darkTheme = true, content = content)
            }

        assertEquals(DarkColorScheme, colors)
        assertNotEquals(wallpaper.primary, colors.primary)
    }

    @Test
    @Config(qualifiers = "night")
    fun `follows the system dark mode by default`() {
        val colors = colorSchemeOf { PhotoSwipeTheme(content = it) }

        assertEquals(DarkColorScheme, colors)
    }

    @Test
    @Config(qualifiers = "notnight")
    fun `follows the system light mode by default`() {
        val colors = colorSchemeOf { PhotoSwipeTheme(content = it) }

        assertEquals(LightColorScheme, colors)
    }

    @Test
    fun `provides the app typography and shapes`() {
        lateinit var typography: Typography
        lateinit var shapes: Shapes
        compose {
            PhotoSwipeTheme {
                typography = MaterialTheme.typography
                shapes = MaterialTheme.shapes
            }
        }

        assertSame(PhotoSwipeTypography, typography)
        assertSame(PhotoSwipeShapes, shapes)
    }

    @Test
    fun `sets numbers in IBM Plex Mono and the rest in Atkinson Hyperlegible Next, on the Material 3 scale`() {
        val baseline = Typography()

        listOf(PhotoSwipeTypography.displayLarge, PhotoSwipeTypography.displaySmall).forEach {
            assertEquals(IbmPlexMono, it.fontFamily)
        }
        listOf(
            PhotoSwipeTypography.headlineSmall,
            PhotoSwipeTypography.titleLarge,
            PhotoSwipeTypography.titleMedium,
            PhotoSwipeTypography.bodyLarge,
            PhotoSwipeTypography.bodyMedium,
            PhotoSwipeTypography.labelLarge,
        ).forEach { assertEquals(AtkinsonHyperlegibleNext, it.fontFamily) }
        assertEquals(baseline.displayLarge.fontSize, PhotoSwipeTypography.displayLarge.fontSize)
        assertEquals(baseline.titleMedium.fontWeight, PhotoSwipeTypography.titleMedium.fontWeight)
        assertEquals(baseline.bodyMedium.lineHeight, PhotoSwipeTypography.bodyMedium.lineHeight)
    }

    /** Compose `theme` autour d'un contenu qui relève le `ColorScheme` reçu. */
    private fun colorSchemeOf(theme: @Composable (content: @Composable () -> Unit) -> Unit): ColorScheme {
        lateinit var colors: ColorScheme
        compose {
            theme { colors = MaterialTheme.colorScheme }
        }
        return colors
    }

    private fun compose(content: @Composable () -> Unit) {
        Robolectric
            .buildActivity(ComponentActivity::class.java)
            .setup()
            .get()
            .setContent(content = content)
        ShadowLooper.idleMainLooper()
    }
}
