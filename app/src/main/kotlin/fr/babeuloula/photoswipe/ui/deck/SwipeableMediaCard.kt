package fr.babeuloula.photoswipe.ui.deck

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.ui.theme.swipeColors
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * [MediaCard] qu'on glisse horizontalement (SPEC §4.3). La carte suit le doigt en s'inclinant ;
 * relâchée au-delà du seuil, elle sort de l'écran du côté du geste puis [onKeep] (droite) ou
 * [onMarkForDeletion] (gauche) est appelé ; en deçà, elle revient au centre avec un ressort. Les deux
 * sens partagent le même seuil et les mêmes animations.
 *
 * Pendant le geste, un voile vert avec une coche (droite) ou rouge avec une corbeille (gauche)
 * recouvre la carte, d'autant plus marqué que le doigt s'éloigne du centre ; franchir le seuil
 * déclenche un retour haptique. Relâchée avant le seuil, la carte revient au centre sans aucun voile,
 * même pendant son rebond ; au-delà, elle sort avec le voile du côté choisi.
 *
 * L'état du geste vit dans la composition : l'appelant entoure la carte d'un `key` sur le média,
 * pour que chaque nouvelle carte parte du centre.
 */
@Composable
fun SwipeableMediaCard(
    card: MediaCardUiState,
    onKeep: () -> Unit,
    onMarkForDeletion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val width = with(LocalDensity.current) { maxWidth.toPx() }
        val offsetX = remember { Animatable(0f) }
        // Position du doigt, tenue à jour de façon synchrone : l'Animatable la suit dans une coroutine.
        // L'indicateur la lit, et non la position animée : il ne suit pas le rebond du retour au centre.
        val dragX = remember { mutableFloatStateOf(0f) }
        val scope = rememberCoroutineScope()
        val haptics = LocalHapticFeedback.current
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        translationX = offsetX.value
                        rotationZ = offsetX.value / width * MAX_ROTATION_DEGREES
                    }.pointerInput(card.id) {
                        detectHorizontalDragGestures(
                            onDragStart = { dragX.floatValue = offsetX.value },
                            onDragEnd = {
                                scope.launch {
                                    val direction = swipeDirection(dragX.floatValue, width)
                                    if (direction == 0f) {
                                        dragX.floatValue = 0f
                                        offsetX.animateTo(0f, returnSpring)
                                    } else {
                                        offsetX.animateTo(direction * width * EXIT_DISTANCE, exitSpring)
                                        if (direction > 0f) onKeep() else onMarkForDeletion()
                                    }
                                }
                            },
                            onDragCancel = {
                                dragX.floatValue = 0f
                                scope.launch { offsetX.animateTo(0f, returnSpring) }
                            },
                        ) { change, dragAmount ->
                            change.consume()
                            val before = swipeDirection(dragX.floatValue, width)
                            dragX.floatValue += dragAmount
                            val after = swipeDirection(dragX.floatValue, width)
                            if (after != 0f && after != before) {
                                haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                            }
                            scope.launch { offsetX.snapTo(dragX.floatValue) }
                        }
                    },
        ) {
            MediaCard(card = card, modifier = Modifier.matchParentSize())
            SwipeIndicator(offset = dragX.floatValue, width = width, modifier = Modifier.matchParentSize())
        }
    }
}

/**
 * Voile coloré du geste en cours : vert et coche vers la droite, rouge et corbeille vers la gauche,
 * avec une opacité qui suit [swipeProgress]. Décoratif : les boutons de la pile (PS-45) porteront
 * les actions pour TalkBack.
 */
@Composable
private fun SwipeIndicator(
    offset: Float,
    width: Float,
    modifier: Modifier = Modifier,
) {
    val progress = swipeProgress(offset, width)
    if (progress == 0f) return
    val keeping = offset > 0f
    val colors = MaterialTheme.swipeColors
    val color = if (keeping) colors.keep else colors.delete
    Box(
        modifier =
            modifier
                .testTag(if (keeping) KEEP_INDICATOR_TAG else DELETE_INDICATOR_TAG)
                .graphicsLayer { alpha = progress }
                .clip(MaterialTheme.shapes.medium)
                .background(color.copy(alpha = MAX_VEIL_ALPHA)),
    ) {
        Icon(
            imageVector = if (keeping) Icons.Filled.Check else Icons.Filled.Delete,
            contentDescription = null,
            tint = if (keeping) colors.onKeep else colors.onDelete,
            modifier =
                Modifier
                    .align(if (keeping) Alignment.TopStart else Alignment.TopEnd)
                    .padding(24.dp)
                    .background(color, MaterialTheme.shapes.extraLarge)
                    .padding(12.dp)
                    .size(48.dp),
        )
    }
}

/**
 * Sens d'un geste relâché à [offset] sur une carte de largeur [width] : 1 vers la droite, -1 vers la
 * gauche, 0 si le seuil n'est pas atteint.
 */
internal fun swipeDirection(
    offset: Float,
    width: Float,
): Float =
    when {
        offset > width * SWIPE_THRESHOLD -> 1f
        offset < -width * SWIPE_THRESHOLD -> -1f
        else -> 0f
    }

/**
 * Intensité de l'indicateur, de 0 (carte au centre) à 1 (seuil atteint ou dépassé), proportionnelle
 * à la distance parcourue, dans un sens comme dans l'autre.
 */
internal fun swipeProgress(
    offset: Float,
    width: Float,
): Float = if (width <= 0f) 0f else (abs(offset) / (width * SWIPE_THRESHOLD)).coerceIn(0f, 1f)

/** Part de la largeur de la carte à dépasser pour que le geste compte. */
internal const val SWIPE_THRESHOLD = 0.35f

/** Étiquette de test de l'indicateur « garder ». */
internal const val KEEP_INDICATOR_TAG = "keep_indicator"

/** Étiquette de test de l'indicateur « supprimer ». */
internal const val DELETE_INDICATOR_TAG = "delete_indicator"

/** Opacité maximale du voile coloré : la photo reste visible dessous. */
private const val MAX_VEIL_ALPHA = 0.35f

/** Distance de sortie, en largeurs de carte : assez pour quitter l'écran malgré l'inclinaison. */
private const val EXIT_DISTANCE = 1.5f

/** Inclinaison quand la carte a parcouru toute sa largeur. */
private const val MAX_ROTATION_DEGREES = 12f

/** Retour au centre souple, avec un léger rebond. */
private val returnSpring =
    spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)

/** Sortie franche, sans rebond. */
private val exitSpring =
    spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
