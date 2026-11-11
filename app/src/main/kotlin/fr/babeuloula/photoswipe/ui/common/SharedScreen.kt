package fr.babeuloula.photoswipe.ui.common

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import java.time.LocalDate

/** Portée des éléments partagés, posée autour du graphe de navigation (PS-141) ; `null` ailleurs. */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

/** Animation d'entrée et de sortie de l'écran courant, fournie par sa destination ; `null` ailleurs. */
val LocalNavAnimatedScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** Ce qu'un élément de l'accueil devient en s'agrandissant (PS-141). */
sealed interface SharedScreenKey {
    /**
     * La carte ou la ligne d'un jour, qui devient sa pile.
     *
     * @property day jour cible.
     */
    data class Deck(
        val day: LocalDate,
    ) : SharedScreenKey

    /**
     * Les vignettes d'un jour, qui deviennent sa vue d'ensemble.
     *
     * @property day jour cible.
     */
    data class Overview(
        val day: LocalDate,
    ) : SharedScreenKey
}

/**
 * Transformation de conteneur (PS-141, ADR 0036) : l'élément touché (carte du jour, ligne d'un jour,
 * vignettes) s'agrandit jusqu'à devenir l'écran qu'il ouvre, qui porte la même [key], et s'y
 * rétracte au retour, y compris pendant le geste de retour prédictif. Les écrans existent dès le
 * départ, avant d'avoir chargé leurs photos : l'effet ne dépend pas de leur chargement. [shape] est
 * la forme de l'élément, gardée pendant qu'il grandit. Sans graphe de navigation (aperçus, tests
 * d'un écran seul), rien ne change.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedScreen(
    key: SharedScreenKey,
    shape: Shape,
): Modifier {
    val shared = LocalSharedTransitionScope.current
    val animated = LocalNavAnimatedScope.current
    if (shared == null || animated == null) return this
    return with(shared) {
        this@sharedScreen.sharedBounds(
            rememberSharedContentState(key),
            animated,
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
            clipInOverlayDuringTransition = OverlayClip(shape),
        )
    }
}
