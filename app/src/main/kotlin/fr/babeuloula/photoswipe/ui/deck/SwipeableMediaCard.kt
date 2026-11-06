package fr.babeuloula.photoswipe.ui.deck

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.ui.theme.motion
import fr.babeuloula.photoswipe.ui.theme.swipeColors
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Côté par lequel une carte quitte la pile, ou y revient après un undo : droite (gardée), gauche
 * (marquée pour suppression), haut (favori, depuis le menu).
 */
enum class CardSide(
    internal val direction: Offset,
) {
    /** Gauche : marquée pour suppression. */
    LEFT(Offset(-1f, 0f)),

    /** Droite : gardée. */
    RIGHT(Offset(1f, 0f)),

    /** Haut : favori. */
    TOP(Offset(0f, -1f)),
}

/** Décision envoyée quand la carte sort par ce côté. */
private val CardSide.decision: DeckEvent
    get() =
        when (this) {
            CardSide.LEFT -> DeckEvent.MarkForDeletion
            CardSide.RIGHT -> DeckEvent.Keep
            CardSide.TOP -> DeckEvent.Favorite
        }

/** Où mène un geste : une décision par la gauche ou la droite, ou le menu par le haut. */
internal enum class SwipeTarget { LEFT, RIGHT, MENU }

/**
 * [MediaCard] qu'on glisse avec le doigt (SPEC §4.3). La carte suit le doigt en s'inclinant.
 * Relâchée au-delà du seuil à droite ou à gauche, elle sort de ce côté et [onEvent] reçoit
 * [DeckEvent.Keep] ou [DeckEvent.MarkForDeletion] ; glissée vers le haut, elle revient au centre et
 * [onEvent] reçoit [DeckEvent.OpenMenu] ; en deçà des seuils, elle revient au centre avec un ressort.
 *
 * Pendant le geste, un voile vert avec une coche (droite), rouge avec une corbeille (gauche) ou bleu
 * avec un cœur (haut) recouvre la carte, d'autant plus marqué que le doigt s'éloigne du centre ;
 * franchir un seuil déclenche un retour haptique. Relâchée avant le seuil, la carte revient sans aucun voile.
 *
 * [requestedExit] fait sortir la carte par ce côté comme un geste : c'est ainsi que les boutons et
 * le menu l'animent. [returningFrom] la fait entrer depuis ce côté jusqu'au centre : l'animation
 * inverse de sa sortie, quand sa décision est annulée.
 *
 * L'état du geste vit dans la composition : l'appelant entoure la carte d'un `key` sur le média,
 * pour que chaque nouvelle carte parte du centre.
 */
@Composable
fun SwipeableMediaCard(
    card: MediaCardUiState,
    onEvent: (DeckEvent) -> Unit,
    modifier: Modifier = Modifier,
    requestedExit: CardSide? = null,
    returningFrom: CardSide? = null,
) {
    BoxWithConstraints(modifier = modifier) {
        val size = with(LocalDensity.current) { Offset(maxWidth.toPx(), maxHeight.toPx()) }
        // Une carte annulée part de l'endroit où elle était sortie, puis revient au centre.
        val offset = remember { Animatable(exitOffset(returningFrom, size), Offset.VectorConverter) }
        // Position du doigt, tenue à jour de façon synchrone : l'Animatable la suit dans une coroutine.
        // L'indicateur la lit, et non la position animée : il ne suit pas le rebond du retour au centre.
        val drag = remember { mutableStateOf(Offset.Zero) }
        val scope = rememberCoroutineScope()
        val haptics = LocalHapticFeedback.current
        // Ressorts du thème (SPEC §8) : retour au centre avec un léger rebond, sortie rapide.
        val motion = MaterialTheme.motion
        val returnSpring = remember(motion) { motion.defaultSpatial<Offset>() }
        val exitSpring = remember(motion) { motion.fastSpatial<Offset>() }

        /** Sortie de la carte par [side], voile plein de ce côté, puis décision. */
        suspend fun exit(side: CardSide) {
            drag.value = side.direction * maxOf(size.x, size.y)
            offset.animateTo(exitOffset(side, size), exitSpring)
            onEvent(side.decision)
        }

        suspend fun backToCenter() {
            drag.value = Offset.Zero
            offset.animateTo(Offset.Zero, returnSpring)
        }

        /** Doigt levé : décision, menu ou retour au centre selon la cible atteinte. */
        suspend fun release() {
            when (swipeTarget(drag.value, size)) {
                SwipeTarget.RIGHT -> exit(CardSide.RIGHT)
                SwipeTarget.LEFT -> exit(CardSide.LEFT)
                SwipeTarget.MENU -> onEvent(DeckEvent.OpenMenu).also { backToCenter() }
                null -> backToCenter()
            }
        }

        LaunchedEffect(Unit) {
            if (offset.value != Offset.Zero) offset.animateTo(Offset.Zero, returnSpring)
        }

        // Bouton ou menu : même sortie que si la carte avait été glissée.
        LaunchedEffect(requestedExit) {
            requestedExit?.let { exit(it) }
        }
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        translationX = offset.value.x
                        translationY = offset.value.y
                        rotationZ = offset.value.x / size.x * MAX_ROTATION_DEGREES
                    }.pointerInput(card.id) {
                        detectDragGestures(
                            onDragStart = { drag.value = offset.value },
                            onDragEnd = { scope.launch { release() } },
                            onDragCancel = { scope.launch { backToCenter() } },
                        ) { change, dragAmount ->
                            change.consume()
                            val before = swipeTarget(drag.value, size)
                            drag.value += dragAmount
                            val after = swipeTarget(drag.value, size)
                            if (after != null && after != before) {
                                haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                            }
                            scope.launch { offset.snapTo(drag.value) }
                        }
                    },
        ) {
            MediaCard(card = card, active = true, modifier = Modifier.matchParentSize())
            SwipeIndicator(indication = swipeIndication(drag.value, size), modifier = Modifier.matchParentSize())
        }
    }
}

/**
 * Voile coloré du geste en cours : vert et coche vers la droite, rouge et corbeille vers la gauche,
 * bleu et cœur vers le haut, avec une opacité qui suit l'intensité de [indication]. Décoratif : les
 * boutons de la pile portent les actions pour TalkBack.
 */
@Composable
private fun SwipeIndicator(
    indication: SwipeIndication?,
    modifier: Modifier = Modifier,
) {
    if (indication == null) return
    val colors = MaterialTheme.swipeColors
    val (color, onColor) =
        when (indication.side) {
            CardSide.RIGHT -> colors.keep to colors.onKeep
            CardSide.LEFT -> colors.delete to colors.onDelete
            CardSide.TOP -> colors.favorite to colors.onFavorite
        }
    Box(
        modifier =
            modifier
                .testTag(indicatorTag(indication.side))
                .graphicsLayer { alpha = indication.progress }
                .clip(MaterialTheme.shapes.medium)
                .background(color.copy(alpha = MAX_VEIL_ALPHA)),
    ) {
        Icon(
            imageVector =
                when (indication.side) {
                    CardSide.RIGHT -> Icons.Filled.Check
                    CardSide.LEFT -> Icons.Filled.Delete
                    CardSide.TOP -> Icons.Filled.Favorite
                },
            contentDescription = null,
            tint = onColor,
            modifier =
                Modifier
                    .align(
                        when (indication.side) {
                            CardSide.RIGHT -> Alignment.TopStart

                            CardSide.LEFT -> Alignment.TopEnd

                            // La carte monte : l'icône se place en bas, la partie qui reste visible.
                            CardSide.TOP -> Alignment.BottomCenter
                        },
                    ).padding(24.dp)
                    .background(color, MaterialTheme.shapes.extraLarge)
                    .padding(12.dp)
                    .size(48.dp),
        )
    }
}

/** Étiquette de test de l'indicateur du côté [side]. */
internal fun indicatorTag(side: CardSide): String =
    when (side) {
        CardSide.RIGHT -> KEEP_INDICATOR_TAG
        CardSide.LEFT -> DELETE_INDICATOR_TAG
        CardSide.TOP -> FAVORITE_INDICATOR_TAG
    }

/**
 * Indicateur à montrer pour un geste en cours : son côté et son intensité, de 0 à 1.
 *
 * @property side côté vers lequel va la carte.
 * @property progress intensité, proportionnelle à la distance jusqu'au seuil de ce côté.
 */
internal data class SwipeIndication(
    val side: CardSide,
    val progress: Float,
)

/**
 * Indicateur du geste [drag] sur une carte de taille [size] : horizontal (gauche ou droite) si le
 * geste est surtout horizontal, bleu s'il monte surtout ; `null` au repos ou vers le bas.
 */
internal fun swipeIndication(
    drag: Offset,
    size: Offset,
): SwipeIndication? {
    val vertical = abs(drag.y) > abs(drag.x)
    val indication =
        when {
            vertical && drag.y < 0f -> SwipeIndication(CardSide.TOP, verticalProgress(drag.y, size.y))
            vertical -> null
            drag.x > 0f -> SwipeIndication(CardSide.RIGHT, swipeProgress(drag.x, size.x))
            drag.x < 0f -> SwipeIndication(CardSide.LEFT, swipeProgress(drag.x, size.x))
            else -> null
        }
    return indication?.takeIf { it.progress > 0f }
}

/** Intensité de l'indicateur vers le haut, pleine au seuil d'ouverture du menu. */
private fun verticalProgress(
    offset: Float,
    height: Float,
): Float = if (height <= 0f) 0f else (abs(offset) / (height * MENU_THRESHOLD)).coerceIn(0f, 1f)

/** Position de la carte sortie par [side] (centre si `null`), pour une carte de taille [size]. */
private fun exitOffset(
    side: CardSide?,
    size: Offset,
): Offset =
    when (side) {
        null -> Offset.Zero
        CardSide.TOP -> Offset(0f, -size.y * EXIT_DISTANCE)
        else -> side.direction * (size.x * EXIT_DISTANCE)
    }

/**
 * Cible d'un geste relâché à [drag] sur une carte de taille [size] : le menu si le doigt est surtout
 * monté au-delà du seuil vertical, sinon la droite ou la gauche au-delà du seuil horizontal ;
 * `null` si aucun seuil n'est atteint.
 */
internal fun swipeTarget(
    drag: Offset,
    size: Offset,
): SwipeTarget? =
    when {
        abs(drag.y) > abs(drag.x) -> if (drag.y < -size.y * MENU_THRESHOLD) SwipeTarget.MENU else null
        drag.x > size.x * SWIPE_THRESHOLD -> SwipeTarget.RIGHT
        drag.x < -size.x * SWIPE_THRESHOLD -> SwipeTarget.LEFT
        else -> null
    }

/**
 * Intensité de l'indicateur, de 0 (carte au centre) à 1 (seuil atteint ou dépassé), proportionnelle
 * à la distance parcourue, dans un sens comme dans l'autre.
 */
internal fun swipeProgress(
    offset: Float,
    width: Float,
): Float = if (width <= 0f) 0f else (abs(offset) / (width * SWIPE_THRESHOLD)).coerceIn(0f, 1f)

/** Part de la largeur de la carte à dépasser pour qu'un geste horizontal compte. */
internal const val SWIPE_THRESHOLD = 0.35f

/** Part de la hauteur de la carte à remonter pour ouvrir le menu. */
internal const val MENU_THRESHOLD = 0.2f

/** Étiquette de test de l'indicateur « garder ». */
internal const val KEEP_INDICATOR_TAG = "keep_indicator"

/** Étiquette de test de l'indicateur « supprimer ». */
internal const val DELETE_INDICATOR_TAG = "delete_indicator"

/** Étiquette de test de l'indicateur « favori ». */
internal const val FAVORITE_INDICATOR_TAG = "favorite_indicator"

/** Opacité maximale du voile coloré : la photo reste visible dessous. */
private const val MAX_VEIL_ALPHA = 0.35f

/** Distance de sortie, en tailles de carte : assez pour quitter l'écran malgré l'inclinaison. */
private const val EXIT_DISTANCE = 1.5f

/** Inclinaison quand la carte a parcouru toute sa largeur. */
private const val MAX_ROTATION_DEGREES = 12f
