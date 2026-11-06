package fr.babeuloula.photoswipe.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Thème Material 3 de PhotoSwipe (SPEC §8) : couleurs, typographie et formes, plus les couleurs
 * des indicateurs de geste ([swipeColors]) et le mouvement.
 *
 * Toujours la palette « planche-contact » ambre (ADR 0025), en clair ou en sombre : c'est la
 * direction artistique de l'application. Les couleurs du fond d'écran (Material You) ne sont pas
 * utilisées, à la demande de Jérémy (PS-102).
 *
 * @param darkTheme `true` pour le thème sombre ; suit le réglage du système par défaut.
 */
@Composable
fun PhotoSwipeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalSwipeColors provides swipeColors(colorScheme, darkTheme),
        LocalPhotoSwipeMotion provides PhotoSwipeMotion(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PhotoSwipeTypography,
            shapes = PhotoSwipeShapes,
            content = content,
        )
    }
}
