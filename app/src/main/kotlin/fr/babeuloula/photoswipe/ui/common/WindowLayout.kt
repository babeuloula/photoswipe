package fr.babeuloula.photoswipe.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass

/**
 * Mise en page de la fenêtre (SPEC §8), d'après les classes de taille de Material 3 Adaptive.
 *
 * Sur téléphone, l'application reste en portrait (décision de Jérémy, PS-32) : [COMPACT]. Sur
 * tablette ou pliable déplié, Android 16+ ignore le verrouillage d'orientation des applications
 * qui le ciblent : la fenêtre peut être large ([MEDIUM], [EXPANDED]) ou basse ([SHORT], paysage).
 */
enum class WindowLayout {
    /** Fenêtre étroite et haute : téléphone en portrait. */
    COMPACT,

    /** Fenêtre moyenne (600 à 840 dp de large) : contenu centré, de largeur limitée. */
    MEDIUM,

    /** Fenêtre large (840 dp ou plus) : deux volets côte à côte. */
    EXPANDED,

    /** Fenêtre basse (moins de 480 dp de haut) et pas étroite : paysage, deux volets. */
    SHORT,
    ;

    /** Vrai si deux volets tiennent côte à côte. */
    val isTwoPane: Boolean get() = this == EXPANDED || this == SHORT
}

/** Largeur maximale d'une colonne de contenu (listes, réglages) sur une fenêtre plus large. */
val MAX_CONTENT_WIDTH: Dp = 640.dp

/** Mise en page de la fenêtre actuelle, recalculée à chaque changement de taille. */
@Composable
fun currentWindowLayout(): WindowLayout {
    val sizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val wide = sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    return when {
        wide && !sizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND) -> WindowLayout.SHORT
        sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> WindowLayout.EXPANDED
        wide -> WindowLayout.MEDIUM
        else -> WindowLayout.COMPACT
    }
}

/**
 * Colonne de contenu centrée, d'au plus [MAX_CONTENT_WIDTH] : sur tablette, les listes et les
 * réglages ne s'étirent pas d'un bord à l'autre. Sans effet sur un téléphone.
 */
@Composable
fun CenteredContent(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(
            modifier =
                Modifier
                    .widthIn(max = MAX_CONTENT_WIDTH)
                    .fillMaxSize(),
            content = content,
        )
    }
}
