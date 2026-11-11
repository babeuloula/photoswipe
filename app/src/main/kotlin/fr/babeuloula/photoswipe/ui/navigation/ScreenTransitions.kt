package fr.babeuloula.photoswipe.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import fr.babeuloula.photoswipe.ui.theme.motion

/**
 * Transitions entre écrans en « fondu enchaîné avec zoom » de Material (PS-141, ADR 0036), à la
 * place de l'axe partagé horizontal (PS-67) : l'écran qui arrive apparaît en grossissant un peu,
 * celui qu'on quitte s'efface ; au retour, l'écran qu'on quitte rapetisse en s'effaçant, y compris
 * pendant le geste de retour prédictif, qui suit le doigt. La carte d'un jour et ses vignettes,
 * elles, s'agrandissent jusqu'à l'écran qu'elles ouvrent (`Modifier.sharedScreen`). Les ressorts viennent du thème
 * (`MaterialTheme.motion`).
 */
@Immutable
class ScreenTransitions(
    private val scale: FiniteAnimationSpec<Float>,
    private val fade: FiniteAnimationSpec<Float>,
) {
    /** Écran qui entre en avançant : il apparaît en grossissant. */
    val enter: EnterTransition get() = fadeIn(fade) + scaleIn(scale, initialScale = ENTER_SCALE)

    /** Écran qui part quand on avance : il s'efface en grossissant à peine, comme dépassé. */
    val exit: ExitTransition get() = fadeOut(fade) + scaleOut(scale, targetScale = BEHIND_SCALE)

    /** Écran qui revient quand on recule : il reprend sa taille. */
    val popEnter: EnterTransition get() = fadeIn(fade) + scaleIn(scale, initialScale = BEHIND_SCALE)

    /** Écran qui part quand on recule : il rapetisse en s'effaçant, comme pendant le geste de retour. */
    val popExit: ExitTransition get() = fadeOut(fade) + scaleOut(scale, targetScale = POP_SCALE)

    private companion object {
        /** Taille de départ de l'écran qui arrive. */
        const val ENTER_SCALE = 0.92f

        /** Écran dépassé, juste derrière celui qui arrive. */
        const val BEHIND_SCALE = 1.04f

        /** Taille d'arrivée de l'écran quitté par un retour. */
        const val POP_SCALE = 0.88f
    }
}

/** Transitions du thème courant. */
@Composable
fun rememberScreenTransitions(): ScreenTransitions {
    val motion = MaterialTheme.motion
    return remember(motion) { ScreenTransitions(motion.defaultSpatial(), motion.defaultEffects()) }
}
