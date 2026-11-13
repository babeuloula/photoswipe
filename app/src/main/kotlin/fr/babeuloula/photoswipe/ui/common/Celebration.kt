package fr.babeuloula.photoswipe.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.ui.theme.swipeColors
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Célébration de fin de pile (PS-174) : la pile d'un jour l'appelle quand sa dernière carte est
 * triée. Elle n'est fournie par [LocalCelebration] que si l'utilisateur l'a activée dans les
 * réglages et que les animations ne sont pas réduites : sinon, la pile ne trouve rien à appeler.
 */
@Stable
class Celebration internal constructor() {
    /** Nombre de célébrations lancées : chacune redémarre les confettis. */
    internal var launches by mutableIntStateOf(0)
        private set

    /** Lance des confettis par-dessus l'écran, sans rien bloquer ni retarder. */
    fun celebrate() {
        launches++
    }
}

/** Célébration de fin de pile, `null` quand elle est désactivée ou que les animations sont réduites. */
val LocalCelebration = staticCompositionLocalOf<Celebration?> { null }

/** Étiquette de test des confettis, qui n'ont aucune sémantique pour TalkBack. */
const val CONFETTI_TAG = "celebration_confetti"

/**
 * Entoure l'application d'une couche de confettis (PS-174), au-dessus de [content] : ainsi la pile
 * peut ouvrir le résumé aussitôt, les confettis continuent de tomber par-dessus. La couche ne reçoit
 * aucun toucher et reste muette pour TalkBack (décorative). Une vibration courte accompagne leur départ.
 *
 * @param enabled réglage « Célébrer la fin d'une pile », désactivé par défaut.
 */
@Composable
fun CelebrationHost(
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    val reducedMotion = rememberReducedMotion()
    val celebration = remember { Celebration() }
    val active = enabled && !reducedMotion
    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalCelebration provides celebration.takeIf { active }) { content() }
        val launches = celebration.launches
        if (active && launches > 0) {
            key(launches) { Confetti() }
        }
    }
}

/** Confettis qui tombent du haut de l'écran en tournoyant, aux couleurs du thème, puis disparaissent. */
@Composable
private fun Confetti() {
    val haptics = LocalHapticFeedback.current
    var finished by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    val colors =
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.swipeColors.keep,
            MaterialTheme.swipeColors.favorite,
        )
    val pieces = remember { confettiPieces(Random.Default) }
    LaunchedEffect(Unit) {
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        progress.animateTo(1f, tween(CONFETTI_DURATION_MILLIS, easing = LinearEasing))
        finished = true
    }
    if (!finished) {
        val pieceSize = with(LocalDensity.current) { PIECE_SIZE.toPx() }
        Canvas(
            Modifier
                .fillMaxSize()
                // Rien à annoncer : la couche est décorative, TalkBack la passe (seule l'étiquette de test la nomme).
                .testTag(CONFETTI_TAG),
        ) {
            val t = progress.value
            pieces.forEach { piece -> drawPiece(piece, t, colors, pieceSize) }
        }
    }
}

/** Dessine un confetti à l'instant [t] (de 0 à 1) de la célébration. */
private fun DrawScope.drawPiece(
    piece: ConfettiPiece,
    t: Float,
    colors: List<Color>,
    pieceSize: Float,
) {
    val local = ((t - piece.delay) / (1f - piece.delay)).coerceIn(0f, 1f)
    if (local <= 0f) return
    val x = size.width * piece.x + sin(local * piece.swayTurns * 2 * PI).toFloat() * piece.sway * size.width
    val y = -pieceSize + local * (size.height + 2 * pieceSize) * piece.speed
    val alpha = if (local > FADE_START) (1f - local) / (1f - FADE_START) else 1f
    val width = pieceSize * piece.scale
    val height = width * PIECE_RATIO
    rotate(degrees = piece.spin * local * FULL_TURN, pivot = Offset(x, y)) {
        drawRect(
            color = colors[piece.color % colors.size].copy(alpha = alpha.coerceIn(0f, 1f)),
            topLeft = Offset(x - width / 2, y - height / 2),
            size = Size(width, height),
        )
    }
}

/**
 * Un confetti : départ horizontal [x] (fraction de la largeur), [delay] avant son départ (fraction
 * de la durée), vitesse de chute, balancement, rotation, taille et couleur (indice dans la palette).
 */
private data class ConfettiPiece(
    val x: Float,
    val delay: Float,
    val speed: Float,
    val sway: Float,
    val swayTurns: Float,
    val spin: Float,
    val scale: Float,
    val color: Int,
)

/** Tire au hasard les confettis d'une célébration. */
private fun confettiPieces(random: Random): List<ConfettiPiece> =
    List(CONFETTI_COUNT) {
        ConfettiPiece(
            x = random.nextFloat(),
            delay = random.nextFloat() * MAX_DELAY,
            speed = 1f + random.nextFloat() * SPEED_SPREAD,
            sway = random.nextFloat() * MAX_SWAY,
            swayTurns = 1f + random.nextFloat() * 2f,
            spin = (random.nextFloat() - 0.5f) * 2 * MAX_SPIN_TURNS,
            scale = MIN_SCALE + random.nextFloat() * (1f - MIN_SCALE),
            color = random.nextInt(PALETTE_SIZE),
        )
    }

/** Durée totale des confettis : assez pour les voir tomber, sans s'attarder sur le résumé. */
private const val CONFETTI_DURATION_MILLIS = 2600

/** Nombre de confettis : une pluie bien visible, légère à dessiner même sur un téléphone ancien. */
private const val CONFETTI_COUNT = 90

/** Couleurs de la palette des confettis. */
private const val PALETTE_SIZE = 5

/** Départ le plus tardif d'un confetti, en fraction de la durée : la pluie s'étale un peu. */
private const val MAX_DELAY = 0.35f

/** Écart de vitesse de chute entre confettis. */
private const val SPEED_SPREAD = 0.4f

/** Balancement horizontal maximal, en fraction de la largeur. */
private const val MAX_SWAY = 0.05f

/** Tours complets au plus sur toute la chute, dans un sens ou dans l'autre. */
private const val MAX_SPIN_TURNS = 3f

/** Taille du plus petit confetti, relative au plus grand. */
private const val MIN_SCALE = 0.6f

/** Moment de la chute où le confetti commence à s'effacer. */
private const val FADE_START = 0.75f

/** Proportions d'un confetti : un petit rectangle. */
private const val PIECE_RATIO = 0.5f

private const val FULL_TURN = 360f

/** Largeur du plus grand confetti. */
private val PIECE_SIZE = 10.dp
