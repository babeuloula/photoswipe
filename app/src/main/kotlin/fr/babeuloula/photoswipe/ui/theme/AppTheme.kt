package fr.babeuloula.photoswipe.ui.theme

import fr.babeuloula.photoswipe.domain.model.ThemeMode

/**
 * Vrai si le thème choisi dans les réglages ([ThemeMode]) est sombre, sachant que le téléphone est
 * en thème sombre ([systemInDarkTheme]) ou non.
 */
fun ThemeMode.isDark(systemInDarkTheme: Boolean): Boolean =
    when (this) {
        ThemeMode.SYSTEM -> systemInDarkTheme
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
