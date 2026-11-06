package fr.babeuloula.photoswipe.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntOffset
import fr.babeuloula.photoswipe.ui.theme.motion

/**
 * Transitions entre écrans en « axe partagé horizontal » de Material (SPEC §8) : l'écran suivant
 * entre en glissant légèrement depuis la droite, en fondu, pendant que le précédent part vers la
 * gauche ; le retour fait l'inverse, y compris pendant le geste de retour prédictif. Ressorts du
 * thème (`MaterialTheme.motion`).
 */
@Immutable
class SharedAxisTransitions(
    private val slide: FiniteAnimationSpec<IntOffset>,
    private val fade: FiniteAnimationSpec<Float>,
) {
    /** Écran qui entre en avançant. */
    val enter: EnterTransition get() = slideInHorizontally(slide) { it / SLIDE_FRACTION } + fadeIn(fade)

    /** Écran qui part quand on avance. */
    val exit: ExitTransition get() = slideOutHorizontally(slide) { -it / SLIDE_FRACTION } + fadeOut(fade)

    /** Écran qui revient quand on recule. */
    val popEnter: EnterTransition get() = slideInHorizontally(slide) { -it / SLIDE_FRACTION } + fadeIn(fade)

    /** Écran qui part quand on recule. */
    val popExit: ExitTransition get() = slideOutHorizontally(slide) { it / SLIDE_FRACTION } + fadeOut(fade)

    private companion object {
        /** Glissement d'un dixième de la largeur : l'axe se devine, sans traverser tout l'écran. */
        const val SLIDE_FRACTION = 10
    }
}

/** Transitions du thème courant. */
@Composable
fun rememberSharedAxisTransitions(): SharedAxisTransitions {
    val motion = MaterialTheme.motion
    return remember(motion) { SharedAxisTransitions(motion.defaultSpatial(), motion.defaultEffects()) }
}
