package fr.babeuloula.photoswipe.ui.deck

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
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
 * [onOpen] est appelé par un appui sur une photo, pour l'ouvrir en grand (PS-117).
 *
 * [onIndicationChange] reçoit l'indicateur du geste à chaque image : la pile en tire le bouton à
 * mettre en avant et la remontée de la carte suivante.
 *
 * L'état du geste vit dans la composition : l'appelant entoure la carte d'un `key` sur le média,
 * pour que chaque nouvelle carte parte du centre.
 *
 * Sans [enabled], la carte attend dans la pile, sous celle du dessus (PS-147) : ni geste, ni lecture
 * vidéo, ni ouverture en grand. Elle garde ainsi la même composition, et son image chargée, du fond
 * de la pile jusqu'au dessus.
 */
@Composable
fun SwipeableMediaCard(
    card: MediaCardUiState,
    onEvent: (DeckEvent) -> Unit,
    modifier: Modifier = Modifier,
    requestedExit: CardSide? = null,
    returningFrom: CardSide? = null,
    onIndicationChange: (SwipeIndication?) -> Unit = {},
    onOpen: (() -> Unit)? = null,
    enabled: Boolean = true,
    frameRatio: Float? = null,
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
        // Ressorts du thème (SPEC §8) : retour au centre avec un léger rebond, sortie rapide sans rebond.
        val motion = MaterialTheme.motion
        val returnSpring = remember(motion) { motion.defaultSpatial<Offset>() }
        val exitSpring = remember(motion) { motion.fastExit<Offset>() }

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
                    }.swipeGestures(
                        enabled = enabled,
                        key = card.id,
                        onDragStart = { drag.value = offset.value },
                        onRelease = { cancelled -> scope.launch { if (cancelled) backToCenter() else release() } },
                    ) { dragAmount ->
                        val before = swipeTarget(drag.value, size)
                        drag.value += dragAmount
                        val after = swipeTarget(drag.value, size)
                        if (crossesThreshold(before, after)) {
                            haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                        }
                        scope.launch { offset.snapTo(drag.value) }
                    },
        ) {
            val indication = swipeIndication(drag.value, size)
            SideEffect { onIndicationChange(indication) }
            // Le voile suit la carte, à la forme de la photo (PS-142).
            MediaCard(
                card = card,
                active = enabled,
                onOpen = onOpen,
                modifier = Modifier.matchParentSize(),
                overlay = { SwipeIndicator(indication = indication, modifier = Modifier.matchParentSize()) },
                frameRatio = frameRatio,
                details = enabled,
            )
        }
    }
}

/**
 * Voile coloré du geste en cours (ADR 0025) : vert vers la droite, rouge vers la gauche, bleu vers
 * le haut, et au centre une pastille ronde avec la coche, la corbeille ou le cœur, puis le libellé
 * (« Garder », « Supprimer », « Favori ou partage »), sur les conteneurs de ces couleurs. L'opacité
 * suit l'intensité de [indication]. Décoratif : les boutons de la pile portent les actions pour
 * TalkBack.
 */
@Composable
internal fun SwipeIndicator(
    indication: SwipeIndication?,
    modifier: Modifier = Modifier,
) {
    if (indication == null) return
    val colors = MaterialTheme.swipeColors
    val (veil, container, onContainer) =
        when (indication.side) {
            CardSide.RIGHT -> Triple(colors.keep, colors.keepContainer, colors.onKeepContainer)
            CardSide.LEFT -> Triple(colors.delete, colors.deleteContainer, colors.onDeleteContainer)
            CardSide.TOP -> Triple(colors.favorite, colors.favoriteContainer, colors.onFavoriteContainer)
        }
    Column(
        modifier =
            modifier
                .testTag(indicatorTag(indication.side))
                .graphicsLayer { alpha = indication.progress }
                .clip(MaterialTheme.shapes.extraLarge)
                .background(veil.copy(alpha = MAX_VEIL_ALPHA)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Box(
            modifier = Modifier.size(64.dp).background(container, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter =
                    painterResource(
                        when (indication.side) {
                            CardSide.RIGHT -> R.drawable.ic_check
                            CardSide.LEFT -> R.drawable.ic_delete
                            CardSide.TOP -> R.drawable.ic_favorite_filled
                        },
                    ),
                contentDescription = null,
                tint = onContainer,
            )
        }
        Text(
            text =
                stringResource(
                    when (indication.side) {
                        CardSide.RIGHT -> R.string.deck_keep
                        CardSide.LEFT -> R.string.deck_delete
                        CardSide.TOP -> R.string.deck_indicator_menu
                    },
                ),
            style = MaterialTheme.typography.labelLarge,
            color = onContainer,
            modifier =
                Modifier
                    .clearAndSetSemantics {}
                    .background(container, CircleShape)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
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
data class SwipeIndication(
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
private const val MAX_VEIL_ALPHA = 0.42f

/** Distance de sortie, en tailles de carte : assez pour quitter l'écran malgré l'inclinaison. */
private const val EXIT_DISTANCE = 1.5f

/** Inclinaison quand la carte a parcouru toute sa largeur. */
private const val MAX_ROTATION_DEGREES = 12f

/**
 * Geste de glissement de la carte, seulement si [enabled] (la carte du dessus, PS-147) : [onDrag]
 * reçoit chaque déplacement du doigt, [onRelease] le doigt levé, ou le geste annulé (vrai).
 */
private fun Modifier.swipeGestures(
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
private fun crossesThreshold(
    before: SwipeTarget?,
    after: SwipeTarget?,
): Boolean = after != null && after != before
