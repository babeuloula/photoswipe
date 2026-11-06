package fr.babeuloula.photoswipe.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import fr.babeuloula.photoswipe.R

/**
 * Police d'interface : Atkinson Hyperlegible Next, du Braille Institute, dessinée pour distinguer
 * chaque lettre et chaque chiffre (ADR 0025). Une seule police variable, embarquée dans `res/font`
 * (l'app n'a pas accès à internet), lue aux graisses 400, 500 et 600.
 */
val AtkinsonHyperlegibleNext: FontFamily =
    FontFamily(
        atkinson(FontWeight.Normal),
        atkinson(FontWeight.Medium),
        atkinson(FontWeight.SemiBold),
    )

/**
 * Police des chiffres expressifs : IBM Plex Mono, à chasse fixe, donc aux chiffres tabulaires.
 * Réservée aux grands nombres (styles display) et au repère « Il y a 3 ans » (ADR 0025).
 */
val IbmPlexMono: FontFamily =
    FontFamily(
        Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
        Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
    )

/** Graisse [weight] de la police variable Atkinson Hyperlegible Next. */
private fun atkinson(weight: FontWeight): Font =
    Font(
        resId = R.font.atkinson_hyperlegible_next,
        weight = weight,
        variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
    )

/** Échelle Material 3 par défaut, dont on ne change que les polices. */
private val Baseline = Typography()

/** Ce style de l'échelle Material 3, en police [family]. */
private fun TextStyle.withFamily(family: FontFamily): TextStyle = copy(fontFamily = family)

/**
 * Typographie de l'application : l'échelle Material 3 (tailles, interlignes, graisses 400 et 500),
 * en Atkinson Hyperlegible Next, sauf les styles display, en IBM Plex Mono pour les grands nombres
 * (« 412 Mo », « 23 »). C'est le seul endroit où l'on personnalise les styles de texte (SPEC §8) ;
 * les écrans utilisent toujours `MaterialTheme.typography`.
 */
internal val PhotoSwipeTypography =
    Typography(
        displayLarge = Baseline.displayLarge.withFamily(IbmPlexMono),
        displayMedium = Baseline.displayMedium.withFamily(IbmPlexMono),
        displaySmall = Baseline.displaySmall.withFamily(IbmPlexMono),
        headlineLarge = Baseline.headlineLarge.withFamily(AtkinsonHyperlegibleNext),
        headlineMedium = Baseline.headlineMedium.withFamily(AtkinsonHyperlegibleNext),
        headlineSmall = Baseline.headlineSmall.withFamily(AtkinsonHyperlegibleNext),
        titleLarge = Baseline.titleLarge.withFamily(AtkinsonHyperlegibleNext),
        titleMedium = Baseline.titleMedium.withFamily(AtkinsonHyperlegibleNext),
        titleSmall = Baseline.titleSmall.withFamily(AtkinsonHyperlegibleNext),
        bodyLarge = Baseline.bodyLarge.withFamily(AtkinsonHyperlegibleNext),
        bodyMedium = Baseline.bodyMedium.withFamily(AtkinsonHyperlegibleNext),
        bodySmall = Baseline.bodySmall.withFamily(AtkinsonHyperlegibleNext),
        labelLarge = Baseline.labelLarge.withFamily(AtkinsonHyperlegibleNext),
        labelMedium = Baseline.labelMedium.withFamily(AtkinsonHyperlegibleNext),
        labelSmall = Baseline.labelSmall.withFamily(AtkinsonHyperlegibleNext),
    )

/**
 * Style du repère de bord de film et de ses puces (ADR 0025) : `labelMedium` (12 / 16) en IBM Plex
 * Mono 500, chiffres tabulaires. Seul usage de la chasse fixe en petit.
 */
val Typography.monoLabel: TextStyle
    get() = labelMedium.copy(fontFamily = IbmPlexMono, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum")
