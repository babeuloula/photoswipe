package fr.babeuloula.photoswipe.ui.tutorial

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.ui.common.FilmEdgeLabel
import fr.babeuloula.photoswipe.ui.deck.CardSide
import fr.babeuloula.photoswipe.ui.deck.SwipeIndication
import fr.babeuloula.photoswipe.ui.deck.SwipeIndicator
import fr.babeuloula.photoswipe.ui.theme.motion
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Page « garder ou supprimer » : le tirage de la première page glisse à droite sous le voile vert
 * et la coche « Garder », revient, puis glisse à gauche sous le voile rouge et la corbeille
 * « Supprimer ». Le voile est celui de la vraie pile ([SwipeIndicator]) : mêmes couleurs, icônes et
 * libellés. Statique ([animate] faux) : le tirage penché à droite, « Garder » affiché.
 */
@Composable
internal fun KeepOrDeleteIllustration(
    animate: Boolean,
    modifier: Modifier = Modifier,
) {
    // Position du tirage : 0 au centre, 1 glissé à droite, -1 glissé à gauche.
    val swipe = remember { Animatable(if (animate) 0f else STATIC_SWIPE) }
    val motion = MaterialTheme.motion
    if (animate) {
        LaunchedEffect(Unit) {
            while (true) {
                for (target in listOf(1f, -1f)) {
                    delay(KEEP_OR_DELETE_PAUSE_MILLIS)
                    swipe.animateTo(target, tween(KEEP_OR_DELETE_SWIPE_MILLIS))
                    delay(KEEP_OR_DELETE_HOLD_MILLIS)
                    swipe.animateTo(0f, motion.defaultSpatial())
                }
            }
        }
    }
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    // Le tirage grandit avec la police, pour que le libellé du voile y tienne.
    val grow = max(1f, density.fontScale * FONT_GROWTH)
    val shiftPx = with(density) { SWIPE_DISTANCE.toPx() }
    Box(modifier, contentAlignment = Alignment.Center) {
        SamplePrint(
            scheme.surfaceContainerHighest,
            Modifier.offset(x = (-44).dp, y = (-12).dp).rotate(-BACK_TILT),
            showIcon = false,
        )
        SamplePrint(
            scheme.surfaceContainerHigh,
            Modifier.offset(x = 44.dp, y = (-16).dp).rotate(BACK_TILT),
            showIcon = false,
        )
        SwipedPrint(swipe = { swipe.value }, shiftPx = shiftPx, grow = grow)
    }
}

/**
 * Tirage d'exemple glissé de [swipe] (-1 à gauche, 1 à droite) fois [shiftPx], penché comme la
 * carte de la pile, avec le voile du côté où il va ; agrandi de [grow] pour une grande police.
 */
@Composable
private fun SwipedPrint(
    swipe: () -> Float,
    shiftPx: Float,
    grow: Float,
) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    Column(
        modifier =
            Modifier.graphicsLayer {
                translationX = swipe() * shiftPx
                rotationZ = swipe() * MAX_TILT
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box {
            SamplePrint(
                scheme.primaryContainer,
                width = PRINT_WIDTH * grow,
                height = PRINT_HEIGHT * grow,
                iconTint = scheme.onPrimaryContainer,
                // Forme de la carte de la pile, celle du voile.
                shape = MaterialTheme.shapes.extraLarge,
            )
            val value = swipe()
            if (abs(value) > MIN_VISIBLE) {
                SwipeIndicator(
                    indication = SwipeIndication(if (value > 0) CardSide.RIGHT else CardSide.LEFT, min(1f, abs(value))),
                    modifier = Modifier.matchParentSize(),
                )
            }
        }
        FilmEdgeLabel(context.resources.getQuantityString(R.plurals.deck_years_ago, YEARS_AGO, YEARS_AGO))
    }
}

/** Ancienneté du souvenir d'exemple. */
private const val YEARS_AGO = 3

/** Position de la version statique : penché à droite, « Garder ». */
private const val STATIC_SWIPE = 0.8f

/** En deçà, le voile n'est pas composé. */
private const val MIN_VISIBLE = 0.02f

/** Croissance du tirage par point d'échelle de police. */
private const val FONT_GROWTH = 0.75f

/** Inclinaison maximale du tirage glissé, en degrés, comme la carte de la pile qui suit le doigt. */
private const val MAX_TILT = 12f

/** Inclinaison des tirages de derrière, en degrés. */
private const val BACK_TILT = 8f

/** Distance parcourue par le tirage glissé. */
private val SWIPE_DISTANCE = 48.dp

/** Pause avant chaque glissement. */
internal const val KEEP_OR_DELETE_PAUSE_MILLIS = 700L

/** Durée d'un glissement. */
internal const val KEEP_OR_DELETE_SWIPE_MILLIS = 650

/** Temps pendant lequel le tirage reste glissé. */
internal const val KEEP_OR_DELETE_HOLD_MILLIS = 700L
