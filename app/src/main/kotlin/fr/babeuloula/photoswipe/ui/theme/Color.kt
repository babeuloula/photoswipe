package fr.babeuloula.photoswipe.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * Palette « planche-contact » (ADR 0025), toujours appliquée : l'application n'utilise pas les
 * couleurs dynamiques (Material You, PS-102). Générée avec material-color-utilities (schéma Tonal spot) à
 * partir de la graine #8A6A1E, l'ambre de la lampe inactinique du labo ; les neutres sont un gris
 * papier photo (teinte 85, chroma 3, et chroma 5 pour les variantes), plus calme que celui du schéma.
 * Chaque rôle Material 3 est défini, rôles fixes compris, pour le clair comme pour le sombre.
 */

/** Palette de repli du thème clair. */
internal val LightColorScheme =
    lightColorScheme(
        primary = Color(0xFF785A0B),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFDF9F),
        onPrimaryContainer = Color(0xFF5C4300),
        inversePrimary = Color(0xFFEAC16C),
        secondary = Color(0xFF6B5C3F),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFF5E0BB),
        onSecondaryContainer = Color(0xFF53452A),
        tertiary = Color(0xFF4A6546),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFCCEBC4),
        onTertiaryContainer = Color(0xFF334D30),
        background = Color(0xFFFFF8F2),
        onBackground = Color(0xFF1E1B17),
        surface = Color(0xFFFFF8F2),
        onSurface = Color(0xFF1E1B17),
        surfaceVariant = Color(0xFFEAE1D6),
        onSurfaceVariant = Color(0xFF4B463E),
        surfaceTint = Color(0xFF785A0B),
        inverseSurface = Color(0xFF33302C),
        inverseOnSurface = Color(0xFFF7F0E9),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF93000A),
        outline = Color(0xFF7D766D),
        outlineVariant = Color(0xFFCEC5BB),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFFFFF8F2),
        surfaceContainer = Color(0xFFF4EDE6),
        surfaceContainerHigh = Color(0xFFEEE7E0),
        surfaceContainerHighest = Color(0xFFE8E1DB),
        surfaceContainerLow = Color(0xFFFAF2EC),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceDim = Color(0xFFE0D9D2),
        primaryFixed = Color(0xFFFFDF9F),
        primaryFixedDim = Color(0xFFEAC16C),
        onPrimaryFixed = Color(0xFF261A00),
        onPrimaryFixedVariant = Color(0xFF5C4300),
        secondaryFixed = Color(0xFFF5E0BB),
        secondaryFixedDim = Color(0xFFD8C4A0),
        onSecondaryFixed = Color(0xFF241A04),
        onSecondaryFixedVariant = Color(0xFF53452A),
        tertiaryFixed = Color(0xFFCCEBC4),
        tertiaryFixedDim = Color(0xFFB1CFA9),
        onTertiaryFixed = Color(0xFF082008),
        onTertiaryFixedVariant = Color(0xFF334D30),
    )

/** Palette de repli du thème sombre. */
internal val DarkColorScheme =
    darkColorScheme(
        primary = Color(0xFFEAC16C),
        onPrimary = Color(0xFF402D00),
        primaryContainer = Color(0xFF5C4300),
        onPrimaryContainer = Color(0xFFFFDF9F),
        inversePrimary = Color(0xFF785A0B),
        secondary = Color(0xFFD8C4A0),
        onSecondary = Color(0xFF3B2F15),
        secondaryContainer = Color(0xFF53452A),
        onSecondaryContainer = Color(0xFFF5E0BB),
        tertiary = Color(0xFFB1CFA9),
        onTertiary = Color(0xFF1D361B),
        tertiaryContainer = Color(0xFF334D30),
        onTertiaryContainer = Color(0xFFCCEBC4),
        background = Color(0xFF15130F),
        onBackground = Color(0xFFE8E1DB),
        surface = Color(0xFF15130F),
        onSurface = Color(0xFFE8E1DB),
        surfaceVariant = Color(0xFF4B463E),
        onSurfaceVariant = Color(0xFFCEC5BB),
        surfaceTint = Color(0xFFEAC16C),
        inverseSurface = Color(0xFFE8E1DB),
        inverseOnSurface = Color(0xFF33302C),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        outline = Color(0xFF979086),
        outlineVariant = Color(0xFF4B463E),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFF3C3934),
        surfaceContainer = Color(0xFF221F1B),
        surfaceContainerHigh = Color(0xFF2C2A25),
        surfaceContainerHighest = Color(0xFF373430),
        surfaceContainerLow = Color(0xFF1E1B17),
        surfaceContainerLowest = Color(0xFF100E0A),
        surfaceDim = Color(0xFF15130F),
        primaryFixed = Color(0xFFFFDF9F),
        primaryFixedDim = Color(0xFFEAC16C),
        onPrimaryFixed = Color(0xFF261A00),
        onPrimaryFixedVariant = Color(0xFF5C4300),
        secondaryFixed = Color(0xFFF5E0BB),
        secondaryFixedDim = Color(0xFFD8C4A0),
        onSecondaryFixed = Color(0xFF241A04),
        onSecondaryFixedVariant = Color(0xFF53452A),
        tertiaryFixed = Color(0xFFCCEBC4),
        tertiaryFixedDim = Color(0xFFB1CFA9),
        onTertiaryFixed = Color(0xFF082008),
        onTertiaryFixedVariant = Color(0xFF334D30),
    )

/**
 * Palette du thème sombre en noir pur (PS-176), pour les écrans OLED et la nuit : les fonds
 * (`background`, `surface`, `surfaceDim`, `surfaceContainerLowest`) passent au noir, le conteneur
 * bas prend l'ancien fond ; les surfaces surélevées (`surfaceContainer` et au-dessus) gardent les
 * gris du thème sombre, pour rester distinctes du fond. Le reste du schéma est inchangé.
 */
internal val PureBlackColorScheme =
    DarkColorScheme.copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceDim = Color.Black,
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color(0xFF15130F),
    )
