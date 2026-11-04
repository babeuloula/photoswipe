package fr.babeuloula.photoswipe.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Thème Material 3 de PhotoSwipe (SPEC §8) : couleurs, typographie et formes.
 *
 * Les couleurs dynamiques (Material You, tirées du fond d'écran) sont disponibles sur tous les
 * appareils visés, puisque `minSdk` vaut 33 et qu'elles existent depuis l'API 31. La palette de
 * repli sert quand elles sont désactivées : aperçus, tests, et futur réglage éventuel.
 *
 * @param darkTheme `true` pour le thème sombre ; suit le réglage du système par défaut.
 * @param dynamicColor `true` pour les couleurs du fond d'écran, `false` pour la palette de repli.
 */
@Composable
fun PhotoSwipeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && darkTheme -> dynamicDarkColorScheme(LocalContext.current)
        dynamicColor -> dynamicLightColorScheme(LocalContext.current)
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PhotoSwipeTypography,
        shapes = PhotoSwipeShapes,
        content = content,
    )
}
