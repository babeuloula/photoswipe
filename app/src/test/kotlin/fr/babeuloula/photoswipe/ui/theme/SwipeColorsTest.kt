package fr.babeuloula.photoswipe.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SwipeColorsTest {
    @Test
    fun `uses the error role of the scheme for deletion`() {
        val colors = swipeColors(DarkColorScheme, darkTheme = true)

        assertEquals(DarkColorScheme.error, colors.delete)
        assertEquals(DarkColorScheme.onError, colors.onDelete)
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
