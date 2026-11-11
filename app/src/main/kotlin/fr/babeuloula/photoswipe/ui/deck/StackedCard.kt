package fr.babeuloula.photoswipe.ui.deck

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import fr.babeuloula.photoswipe.ui.theme.motion

// Pile de cartes de la pile de tri (PS-147) : celles du dessous, comme une pile de tirages.

/**
 * Une carte de la pile, à la profondeur [depth] (0 : celle du dessus). Celles du dessous sont un peu
 * plus bas, plus petites et légèrement inclinées, comme une pile de tirages ; elles remontent d'un
 * cran, en douceur, à chaque carte triée, et la dernière arrivée au fond apparaît en fondu (PS-147).
 *
 * Chaque carte garde sa propre forme : [bottomAlign] la descend pour caler son bas sur celui de la
 * carte du dessus (vers le haut si elle est plus haute), d'où ses bords qui dépassent. Arrivée en
 * haut, elle remonte au centre sans se déformer ni zoomer son image. Plus haute que la carte du
 * dessus, elle est réduite d'autant ([fitScale]) pour ne jamais dépasser au-dessus de la pile. Tant que son image n'est pas
 * chargée ([ready] faux), une carte du dessous reste invisible.
 */
@Composable
internal fun StackedCard(
    card: MediaCardUiState,
    depth: Int,
    bottomAlign: Dp,
    actions: TopCardActions,
    fitScale: Float = 1f,
    ready: Boolean = true,
) {
    val top = depth == 0
    val motion = MaterialTheme.motion
    val stacked by animateFloatAsState(depth.toFloat(), motion.defaultSpatial(), label = "stackDepth")
    // Une nouvelle carte du dessus d'une autre hauteur : les cartes du dessous suivent son bas en douceur.
    val align by animateDpAsState(bottomAlign, motion.defaultSpatial(), label = "stackAlign")
    val fit by animateFloatAsState(fitScale, motion.defaultSpatial(), label = "stackFit")
    val appear = remember { Animatable(if (top) 1f else 0f) }
    // Une carte du dessous n'apparaît qu'avec son image : avant, elle n'a pas encore sa forme.
    LaunchedEffect(ready) { if (ready) appear.animateTo(1f, motion.defaultEffects()) }
    SwipeableMediaCard(
        card = card,
        onEvent = actions.onEvent,
        enabled = top,
        requestedExit = actions.requestedExit.takeIf { top },
        returningFrom = actions.returningFrom.takeIf { top },
        onIndicationChange = if (top) actions.onIndicationChange else { _ -> },
        onOpen = actions.onOpen.takeIf { top },
        modifier =
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Réduite par l'affichage seulement : l'image garde sa pleine résolution en arrivant en haut.
                    val scale = (1f - STACK_SCALE_STEP * stacked) * lerp(1f, fit, stacked.coerceAtMost(1f))
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(STACK_PIVOT_X, 1f)
                    // Le bas calé sur celui de la carte du dessus, puis un cran plus bas par profondeur ;
                    // en arrivant en haut, la carte remonte au centre sans changer de forme.
                    translationY = align.toPx() * stacked.coerceAtMost(1f) + STACK_OFFSET_STEP.toPx() * stacked
                    rotationZ = stackRotation(stacked)
                    alpha = if (top) 1f else appear.value
                }.then(if (top) Modifier else Modifier.clearAndSetSemantics { testTag = stackTag(depth) }),
    )
}

/** Inclinaison d'une carte à la profondeur [depth], entre celles de ses deux crans voisins. */
private fun stackRotation(depth: Float): Float {
    val lower = depth.toInt().coerceIn(0, STACK_ROTATIONS.lastIndex)
    val upper = (lower + 1).coerceAtMost(STACK_ROTATIONS.lastIndex)
    return lerp(STACK_ROTATIONS[lower], STACK_ROTATIONS[upper], depth - lower)
}

/** Étiquette de test d'une carte du dessous : [NEXT_CARD_TAG] pour la suivante. */
internal fun stackTag(depth: Int): String = if (depth == 1) NEXT_CARD_TAG else "$STACK_CARD_TAG$depth"

/** Pivot horizontal des cartes du dessous : leur milieu, pour qu'elles grandissent vers le haut depuis leur bas. */
private const val STACK_PIVOT_X = 0.5f

/** Rétrécissement par cran de profondeur dans la pile. */
private const val STACK_SCALE_STEP = 0.05f

/** Décalage vers le bas par cran de profondeur : le bas des cartes du dessous dépasse. */
internal val STACK_OFFSET_STEP = 16.dp

/** Inclinaison des cartes selon leur cran, en degrés : droite au-dessus, puis alternées comme des tirages posés. */
@Suppress("MagicNumber") // Angles de la pile, sans autre sens que leur valeur.
private val STACK_ROTATIONS = listOf(0f, -3f, 3f, -2f)

/** Préfixe de l'étiquette de test d'une carte du dessous, suivi de sa profondeur (au-delà de la suivante). */
internal const val STACK_CARD_TAG = "stack_card_"
