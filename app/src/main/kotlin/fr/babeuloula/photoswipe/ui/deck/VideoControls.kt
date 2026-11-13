package fr.babeuloula.photoswipe.ui.deck

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.ui.common.FilmEdgeChip
import fr.babeuloula.photoswipe.ui.theme.motion
import kotlinx.coroutines.delay
import java.time.Duration

/**
 * Barre de progression d'une vidéo (PS-118) : elle suit la lecture, et glisser le curseur ou
 * toucher la barre déplace la lecture à cet endroit. Pendant le déplacement, le temps visé et la
 * durée totale s'affichent au-dessus (« 0:12 / 0:45 »).
 *
 * Posée sur la vidéo, elle prend les rôles fixes du thème. Le curseur, qui consomme le glissement
 * horizontal, a la priorité sur le geste de tri de la carte. TalkBack l'annonce « Position dans la
 * vidéo, 0:12 sur 0:45 » et la règle par pas.
 *
 * @param position position de lecture.
 * @param duration durée totale, non nulle.
 * @param onSeek position choisie, à la fin du déplacement.
 */
@Composable
internal fun VideoProgressBar(
    position: Duration,
    duration: Duration,
    onSeek: (Duration) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Fraction visée pendant le déplacement du curseur, `null` sinon : la barre suit alors la lecture.
    var dragging by remember { mutableStateOf<Float?>(null) }
    val fraction = dragging ?: (position.toMillis().toFloat() / duration.toMillis()).coerceIn(0f, 1f)
    val shown = Duration.ofMillis((duration.toMillis() * fraction).toLong())
    val label = stringResource(R.string.deck_video_position)
    val state = stringResource(R.string.deck_video_position_state, durationLabel(shown), durationLabel(duration))
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (dragging != null) {
            FilmEdgeChip { Text("${durationLabel(shown)} / ${durationLabel(duration)}") }
        }
        Slider(
            value = fraction,
            onValueChange = { dragging = it },
            onValueChangeFinished = {
                dragging?.let { onSeek(Duration.ofMillis((duration.toMillis() * it).toLong())) }
                dragging = null
            },
            colors =
                SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primaryFixed,
                    activeTrackColor = MaterialTheme.colorScheme.primaryFixed,
                    inactiveTrackColor = MaterialTheme.colorScheme.onPrimaryFixed,
                ),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(VIDEO_PROGRESS_TAG)
                    .semantics {
                        contentDescription = label
                        stateDescription = state
                    },
        )
    }
}

/** Étiquette de test de la barre de progression d'une vidéo. */
internal const val VIDEO_PROGRESS_TAG = "video_progress"

/**
 * Zone de tap de la vidéo et son gros bouton central : ▶ tant qu'elle ne joue pas ; au démarrage,
 * ⏸ qui s'efface en fondu. Toute la carte réagit au tap, sans ondulation ; un glissement reste un
 * geste de tri. TalkBack lit l'action : « Lire la vidéo » ou « Mettre la vidéo en pause ». Le
 * maintien du doigt lit la vidéo en ×2 ([fast], [onFast], PS-161), avec une puce « ×2 ».
 */
@Composable
internal fun PlayPauseOverlay(
    playing: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    fast: Boolean = false,
    onFast: (Boolean) -> Unit = {},
) {
    val buttonAlpha = remember { Animatable(1f) }
    // Fondu du thème (SPEC §8) : effet lent, sans rebond.
    val fadeOut = MaterialTheme.motion.slowEffects<Float>()
    LaunchedEffect(playing) {
        if (playing) {
            delay(PAUSE_ICON_VISIBLE.toMillis())
            buttonAlpha.animateTo(0f, fadeOut)
        } else {
            buttonAlpha.snapTo(1f)
        }
    }
    val action = stringResource(if (playing) R.string.deck_video_pause else R.string.deck_video_play)
    Box(
        modifier =
            modifier
                .holdToFastForward(fast = fast, onFast = onFast, onTap = onToggle)
                .semantics {
                    contentDescription = action
                    role = Role.Button
                },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.onPrimaryFixed,
            contentColor = MaterialTheme.colorScheme.primaryFixed,
            shape = CircleShape,
            modifier =
                Modifier
                    .size(BIG_BUTTON_SIZE)
                    .graphicsLayer { alpha = buttonAlpha.value }
                    .testTag(PLAY_PAUSE_BUTTON_TAG),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter =
                        if (playing) {
                            painterResource(
                                R.drawable.ic_pause_filled,
                            )
                        } else {
                            painterResource(R.drawable.ic_play_arrow_filled)
                        },
                    contentDescription = null,
                    modifier = Modifier.size(BIG_ICON_SIZE),
                )
            }
        }
        if (fast) FastForwardChip(Modifier.align(Alignment.TopCenter).padding(top = 64.dp))
    }
}

/**
 * Bouton du son de la vidéo (SPEC §4.3) : coupé au départ, un tap l'active ou le coupe. Le réglage
 * suit l'utilisateur sur les vidéos suivantes de la pile.
 */
@Composable
internal fun SoundButton(
    muted: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledIconButton(
        onClick = onToggle,
        colors =
            IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.onPrimaryFixed,
                contentColor = MaterialTheme.colorScheme.primaryFixed,
            ),
        modifier = modifier.size(48.dp),
    ) {
        Icon(
            painter = painterResource(if (muted) R.drawable.ic_volume_off else R.drawable.ic_volume_up),
            contentDescription =
                stringResource(
                    if (muted) R.string.deck_video_sound_on else R.string.deck_video_sound_off,
                ),
        )
    }
}

/** Taille du gros bouton central d'une vidéo. */
private val BIG_BUTTON_SIZE = 64.dp

/** Taille de son icône. */
private val BIG_ICON_SIZE = 32.dp

/** Temps pendant lequel ⏸ reste visible au démarrage, avant de s'effacer. */
private val PAUSE_ICON_VISIBLE: Duration = Duration.ofMillis(PAUSE_ICON_VISIBLE_MILLIS)

/** Étiquette de test du gros bouton central d'une vidéo. */
internal const val PLAY_PAUSE_BUTTON_TAG = "play_pause_button"

/** Temps pendant lequel ⏸ reste visible au démarrage, en millisecondes. */
private const val PAUSE_ICON_VISIBLE_MILLIS = 600L
