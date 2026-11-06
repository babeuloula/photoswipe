package fr.babeuloula.photoswipe.ui.theme

import fr.babeuloula.photoswipe.domain.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class AppThemeTest {
    @Test
    fun `follows the phone for the phone theme`() {
        assertEquals(true, ThemeMode.SYSTEM.isDark(systemInDarkTheme = true))
        assertEquals(false, ThemeMode.SYSTEM.isDark(systemInDarkTheme = false))
    }

    @Test
    fun `keeps the light or dark theme whatever the phone uses`() {
        assertEquals(false, ThemeMode.LIGHT.isDark(systemInDarkTheme = true))
        assertEquals(true, ThemeMode.DARK.isDark(systemInDarkTheme = false))
    }
}
