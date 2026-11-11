package fr.babeuloula.photoswipe.ui.deck

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

// Geste et sortie de la carte du dessus de la pile (PS-147).

/**
 * Geste de glissement de la carte, seulement si [enabled] (la carte du dessus, PS-147) : [onDrag]
 * reçoit chaque déplacement du doigt, [onRelease] le doigt levé, ou le geste annulé (vrai).
 */
internal fun Modifier.swipeGestures(
    enabled: Boolean,
    key: Any,
    onDragStart: () -> Unit,
    onRelease: (cancelled: Boolean) -> Unit,
    onDrag: (Offset) -> Unit,
): Modifier =
    if (!enabled) {
        this
    } else {
        pointerInput(key) {
            detectDragGestures(
                onDragStart = { onDragStart() },
                onDragEnd = { onRelease(false) },
                onDragCancel = { onRelease(true) },
            ) { change, dragAmount ->
                change.consume()
                onDrag(dragAmount)
            }
        }
    }

/** Vrai si le doigt vient d'atteindre une nouvelle cible ([after]) : retour haptique. */
internal fun crossesThreshold(
    before: SwipeTarget?,
    after: SwipeTarget?,
): Boolean = after != null && after != before

/**
 * Anime la carte jusqu'à [target] et appelle [onGone] une seule fois, dès qu'elle a quitté l'écran
 * ([size]), ou à la fin de l'animation au plus tard (PS-147).
 */
internal suspend fun Animatable<Offset, AnimationVector2D>.leave(
    target: Offset,
    spec: AnimationSpec<Offset>,
    size: Offset,
    onGone: () -> Unit,
) {
    var gone = false
    animateTo(target, spec) {
        if (!gone && isOffScreen(value, size)) {
            gone = true
            onGone()
        }
    }
    if (!gone) onGone()
}

/** Vrai si une carte décalée de [offset] est entièrement sortie d'un espace de taille [size]. */
private fun isOffScreen(
    offset: Offset,
    size: Offset,
): Boolean = abs(offset.x) >= size.x * OFF_SCREEN || abs(offset.y) >= size.y * OFF_SCREEN

/** Décalage, en part de la taille de la pile, au-delà duquel une carte (même inclinée) est hors de l'écran. */
private const val OFF_SCREEN = 1f
