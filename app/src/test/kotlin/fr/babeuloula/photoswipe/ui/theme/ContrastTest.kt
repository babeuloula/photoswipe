package fr.babeuloula.photoswipe.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contrastes des couleurs de repli du thème (SPEC §8) : chaque couple texte / fond atteint le
 * rapport WCAG AA de 4,5:1, en clair et en sombre, couleurs des gestes comprises. Les couleurs
 * dynamiques (Material You) sont calculées par Android, qui garantit déjà ces contrastes.
 */
class ContrastTest {
    /** Rapport de contraste WCAG 2.x entre deux couleurs opaques. */
    private fun contrast(
        a: Color,
        b: Color,
    ): Double {
        val (light, dark) = listOf(a.luminance() + 0.05, b.luminance() + 0.05).sortedDescending()
        return light.toDouble() / dark
    }

    private fun assertReadable(
        name: String,
        text: Color,
        background: Color,
    ) {
        val ratio = contrast(text, background)
        assertTrue("$name: contrast $ratio is below 4.5:1", ratio >= MIN_TEXT_CONTRAST)
    }

    private fun assertSchemeReadable(
        scheme: ColorScheme,
        darkTheme: Boolean,
    ) {
        val theme = if (darkTheme) "dark" else "light"
        assertReadable("$theme onSurface", scheme.onSurface, scheme.surface)
        assertReadable("$theme onSurfaceVariant", scheme.onSurfaceVariant, scheme.surface)
        assertReadable("$theme onBackground", scheme.onBackground, scheme.background)
        assertReadable("$theme onPrimary", scheme.onPrimary, scheme.primary)
        assertReadable("$theme primary text", scheme.primary, scheme.surface)
        assertReadable("$theme onPrimaryContainer", scheme.onPrimaryContainer, scheme.primaryContainer)
        assertReadable("$theme onSecondaryContainer", scheme.onSecondaryContainer, scheme.secondaryContainer)
        assertReadable("$theme onError", scheme.onError, scheme.error)
        assertReadable("$theme onErrorContainer", scheme.onErrorContainer, scheme.errorContainer)
        assertReadable("$theme onSurface on containers", scheme.onSurface, scheme.surfaceContainerHighest)
        assertReadable("$theme onSurfaceVariant on containers", scheme.onSurfaceVariant, scheme.surfaceContainerHighest)
        // Repère « Il y a 3 ans » et pastilles posées sur les photos : rôles fixes (ADR 0025).
        assertReadable("$theme film edge label", scheme.primaryFixedDim, scheme.onPrimaryFixed)
        assertReadable("$theme film edge chip", scheme.primaryFixed, scheme.onPrimaryFixed)
        val swipe = swipeColors(scheme, darkTheme)
        assertReadable("$theme onKeep", swipe.onKeep, swipe.keep)
        assertReadable("$theme onDelete", swipe.onDelete, swipe.delete)
        assertReadable("$theme onFavorite", swipe.onFavorite, swipe.favorite)
        assertReadable("$theme onKeepContainer", swipe.onKeepContainer, swipe.keepContainer)
        assertReadable("$theme onDeleteContainer", swipe.onDeleteContainer, swipe.deleteContainer)
        assertReadable("$theme onFavoriteContainer", swipe.onFavoriteContainer, swipe.favoriteContainer)
    }

    @Test
    fun `keeps every text readable in the light theme`() {
        assertSchemeReadable(LightColorScheme, darkTheme = false)
    }

    @Test
    fun `keeps every text readable in the dark theme`() {
        assertSchemeReadable(DarkColorScheme, darkTheme = true)
    }

    @Test
    fun `measures the contrast like the WCAG`() {
        assertTrue(contrast(Color.Black, Color.White) > 20.9)
        assertTrue(contrast(Color.Gray, Color.Gray) < 1.01)
    }

    private companion object {
        /** Contraste minimal d'un texte normal, niveau AA des WCAG. */
        const val MIN_TEXT_CONTRAST = 4.5
    }
}
