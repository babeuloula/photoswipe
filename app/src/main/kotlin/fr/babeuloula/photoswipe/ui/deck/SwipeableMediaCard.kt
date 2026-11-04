package fr.babeuloula.photoswipe.ui.deck

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch

/**
 * [MediaCard] qu'on glisse horizontalement (SPEC §4.3). La carte suit le doigt en s'inclinant ;
 * relâchée au-delà du seuil vers la droite, elle sort de l'écran puis [onKeep] est appelé ; en deçà,
 * elle revient au centre avec un ressort. Le glissement à gauche arrive avec PS-43 : d'ici là, la
 * carte revient au centre.
 *
 * L'état du geste vit dans la composition : l'appelant entoure la carte d'un `key` sur le média,
 * pour que chaque nouvelle carte parte du centre.
 */
@Composable
fun SwipeableMediaCard(
    card: MediaCardUiState,
    onKeep: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val width = with(LocalDensity.current) { maxWidth.toPx() }
        val offsetX = remember { Animatable(0f) }
        val scope = rememberCoroutineScope()
        MediaCard(
            card = card,
            modifier =
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        translationX = offsetX.value
                        rotationZ = offsetX.value / width * MAX_ROTATION_DEGREES
                    }.pointerInput(card.id) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                scope.launch {
                                    if (offsetX.value > width * SWIPE_THRESHOLD) {
                                        offsetX.animateTo(width * EXIT_DISTANCE, exitSpring)
                                        onKeep()
                                    } else {
                                        offsetX.animateTo(0f, returnSpring)
                                    }
                                }
                            },
                            onDragCancel = { scope.launch { offsetX.animateTo(0f, returnSpring) } },
                        ) { change, dragAmount ->
                            change.consume()
                            scope.launch { offsetX.snapTo(offsetX.value + dragAmount) }
                        }
                    },
        )
    }
}

/** Part de la largeur de la carte à dépasser pour que le geste compte. */
internal const val SWIPE_THRESHOLD = 0.35f

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
