package fr.babeuloula.photoswipe.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SwipeColorsTest {
    @Test
    fun `uses the error role of the scheme for deletion`() {
        val colors = swipeColors(DarkColorScheme, darkTheme = true)

        assertEquals(DarkColorScheme.error, colors.delete)
        assertEquals(DarkColorScheme.onError, colors.onDelete)
        assertEquals(DarkColorScheme.errorContainer, colors.deleteContainer)
        assertEquals(DarkColorScheme.onErrorContainer, colors.onDeleteContainer)
    }

    @Test
    fun `uses the keep and favorite colors harmonized toward the amber seed`() {
        val light = swipeColors(LightColorScheme, darkTheme = false)
        val dark = swipeColors(DarkColorScheme, darkTheme = true)

        assertEquals(Color(0xFF316B16), light.keep)
        assertEquals(Color(0xFFB1F48F), light.keepContainer)
        assertEquals(Color(0xFF00629D), light.favorite)
        assertEquals(Color(0xFF004A78), dark.favoriteContainer)
        assertEquals(Color(0xFF96D876), dark.keep)
    }

    @Test
    fun `uses a lighter blue in the dark theme for the favorite menu`() {
        val light = swipeColors(LightColorScheme, darkTheme = false)
        val dark = swipeColors(DarkColorScheme, darkTheme = true)

        assertNotEquals(light.favorite, dark.favorite)
        assertNotEquals(light.favorite, light.onFavorite)
        assertNotEquals(dark.favorite, dark.onFavorite)
    }

    @Test
    fun `uses a lighter green in the dark theme`() {
        val light = swipeColors(LightColorScheme, darkTheme = false)
        val dark = swipeColors(DarkColorScheme, darkTheme = true)

        assertNotEquals(light.keep, dark.keep)
        assertNotEquals(light.keep, light.onKeep)
        assertNotEquals(dark.keep, dark.onKeep)
    }
}
