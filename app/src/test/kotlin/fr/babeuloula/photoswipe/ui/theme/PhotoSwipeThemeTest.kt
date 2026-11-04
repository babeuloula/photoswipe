package fr.babeuloula.photoswipe.ui.theme

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import org.junit.Assert.assertEquals
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
    fun `uses the light fallback palette when dynamic color is off`() {
        val colors = colorSchemeOf { PhotoSwipeTheme(darkTheme = false, dynamicColor = false, content = it) }

        assertEquals(LightColorScheme, colors)
    }

    @Test
    fun `uses the dark fallback palette when dynamic color is off`() {
        val colors = colorSchemeOf { PhotoSwipeTheme(darkTheme = true, dynamicColor = false, content = it) }

        assertEquals(DarkColorScheme, colors)
    }

    @Test
    fun `uses wallpaper based colors when dynamic color is on`() {
        lateinit var expected: ColorScheme
        val colors =
            colorSchemeOf { content ->
                expected = dynamicDarkColorScheme(LocalContext.current)
                PhotoSwipeTheme(darkTheme = true, dynamicColor = true, content = content)
            }

        assertEquals(expected.primary, colors.primary)
        assertEquals(expected.surface, colors.surface)
    }

    @Test
    @Config(qualifiers = "night")
    fun `follows the system dark mode by default`() {
        val colors = colorSchemeOf { PhotoSwipeTheme(dynamicColor = false, content = it) }

        assertEquals(DarkColorScheme, colors)
    }

    @Test
    @Config(qualifiers = "notnight")
    fun `follows the system light mode by default`() {
        val colors = colorSchemeOf { PhotoSwipeTheme(dynamicColor = false, content = it) }

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
