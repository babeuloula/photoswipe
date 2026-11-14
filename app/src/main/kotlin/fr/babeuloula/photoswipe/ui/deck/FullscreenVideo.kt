package fr.babeuloula.photoswipe.ui.deck

import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPresentationState
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.ui.common.AllowRotation
import kotlinx.coroutines.delay
import java.time.Duration

/**
 * Vidéo de la carte du dessus en plein écran (PS-119), ouverte par le bouton sous celui du son :
 * fond noir, barres système masquées, même lecteur que la carte (la lecture continue au même
 * endroit, avec le même son). C'est le seul écran qui suit l'orientation du téléphone ; sans zoom.
 *
 * Commandes : lecture / pause au centre, son et « Quitter le plein écran » en haut, barre de
 * progression en bas. Elles s'effacent quelques secondes après le début de la lecture et reviennent
 * au toucher ; avec TalkBack, elles restent affichées. Le retour ou « Quitter » ramène à la carte,
 * à la position atteinte, sans décision.
 */
@Composable
internal fun FullscreenVideo(
    card: MediaCardUiState,
    videoPlayer: CardVideoPlayer,
    onExit: () -> Unit,
) {
    // Lu ici : la fenêtre du dialogue fournit ses propres réglages de plateforme.
    val hideControlsAfter =
        LocalAccessibilityManager.current?.calculateRecommendedTimeoutMillis(
            CONTROLS_VISIBLE_MILLIS,
            containsIcons = true,
            containsControls = true,
        ) ?: CONTROLS_VISIBLE_MILLIS
    Dialog(
        onDismissRequest = onExit,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        ImmersiveWindow()
        AllowRotation()
        KeepScreenOnWhilePlaying(videoPlayer)
        FullscreenVideoContent(
            card = card,
            videoPlayer = videoPlayer,
            onExit = onExit,
            hideControlsAfter = hideControlsAfter,
        )
    }
}

/**
 * Contenu de [FullscreenVideo], sans sa fenêtre. Les commandes s'effacent [hideControlsAfter]
 * millisecondes après le début de la lecture ; jamais avec `Long.MAX_VALUE` (TalkBack).
 */
@OptIn(UnstableApi::class)
@Composable
internal fun FullscreenVideoContent(
    card: MediaCardUiState,
    videoPlayer: CardVideoPlayer,
    onExit: () -> Unit,
    hideControlsAfter: Long = CONTROLS_VISIBLE_MILLIS,
) {
    val playing = videoPlayer.playingUri == card.uri
    var position by remember(card.uri) { mutableStateOf(videoPlayer.position(card.uri) ?: Duration.ZERO) }
    LaunchedEffect(card.uri, playing) {
        position = videoPlayer.position(card.uri) ?: Duration.ZERO
        while (playing) {
            delay(PROGRESS_TICK_MILLIS)
            position = videoPlayer.position(card.uri) ?: Duration.ZERO
        }
    }
    var controlsVisible by remember { mutableStateOf(true) }
    LaunchedEffect(controlsVisible, playing, hideControlsAfter) {
        if (controlsVisible && playing && hideControlsAfter != Long.MAX_VALUE) {
            delay(hideControlsAfter)
            controlsVisible = false
        }
    }
    val presentation = rememberPresentationState(videoPlayer.player)
    val description = stringResource(R.string.deck_video_description, localizedDate(LocalContext.current, card.takenOn))
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .holdToFastForward(
                    fast = videoPlayer.fastForward,
                    onFast = { videoPlayer.setFastForward(card.uri, it) },
                    onTap = { controlsVisible = !controlsVisible },
                    onTapLabel = stringResource(R.string.deck_video_toggle_controls),
                ).semantics { contentDescription = description }
                .testTag(FULLSCREEN_VIDEO_TAG),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.matchParentSize().alpha(if (presentation.coverSurface) 0f else 1f),
            contentAlignment = Alignment.Center,
        ) {
            PlayerSurface(
                player = videoPlayer.player,
                surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
                modifier = Modifier.resizeWithContentScale(ContentScale.Fit, presentation.videoSizeDp),
            )
        }
        if (videoPlayer.fastForward) FastForwardChip(Modifier.align(Alignment.TopCenter).padding(top = 48.dp))
        AnimatedVisibility(visible = controlsVisible, enter = fadeIn(), exit = fadeOut()) {
            FullscreenControls(
                card = card,
                videoPlayer = videoPlayer,
                position = position,
                onSeek = { target ->
                    videoPlayer.seekTo(card.uri, target)
                    position = target
                },
                onExit = onExit,
            )
        }
    }
}

/** Commandes du plein écran : lecture / pause au centre, son et sortie en haut, progression en bas. */
@Composable
private fun FullscreenControls(
    card: MediaCardUiState,
    videoPlayer: CardVideoPlayer,
    position: Duration,
    onSeek: (Duration) -> Unit,
    onExit: () -> Unit,
) {
    val playing = videoPlayer.playingUri == card.uri
    Box(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
        FilledIconButton(
            onClick = { if (playing) videoPlayer.pauseByUser(card.uri) else videoPlayer.playByUser(card.uri) },
            colors = fixedButtonColors(),
            modifier = Modifier.align(Alignment.Center).size(BIG_CONTROL_SIZE),
        ) {
            Icon(
                painter = painterResource(if (playing) R.drawable.ic_pause_filled else R.drawable.ic_play_arrow_filled),
                contentDescription =
                    stringResource(
                        if (playing) R.string.deck_video_pause else R.string.deck_video_play,
                    ),
                modifier = Modifier.size(32.dp),
            )
        }
        Row(
            modifier = Modifier.align(Alignment.TopEnd),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SoundButton(muted = videoPlayer.muted, onToggle = videoPlayer::toggleSound)
            FilledIconButton(
                onClick = onExit,
                colors = fixedButtonColors(),
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_fullscreen_exit),
                    contentDescription = stringResource(R.string.deck_video_exit_fullscreen),
                )
            }
        }
        card.duration?.takeIf { !it.isZero && !it.isNegative }?.let { total ->
            VideoProgressBar(
                position = position,
                duration = total,
                onSeek = onSeek,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            )
        }
    }
}

/** Couleurs des boutons posés sur la vidéo : rôles fixes du thème. */
@Composable
internal fun fixedButtonColors() =
    IconButtonDefaults.filledIconButtonColors(
        containerColor = MaterialTheme.colorScheme.onPrimaryFixed,
        contentColor = MaterialTheme.colorScheme.primaryFixed,
    )

/** Masque les barres système de la fenêtre du plein écran ; un glissement depuis le bord les rappelle. */
@Composable
private fun ImmersiveWindow() {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window ?: return
    DisposableEffect(window) {
        window.insetsController?.let { controller ->
            controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsets.Type.systemBars())
        }
        onDispose {}
    }
}

/**
 * Vidéo ouverte en plein écran (son URI, `null` sinon), tenue par la pile (`LoadedDeck`) et
 * enregistrée avec l'état de l'écran : une rotation recrée l'activité, le plein écran reste ouvert
 * (PS-126). `null` hors d'une pile : la carte tient alors elle-même l'état.
 */
val LocalFullscreenVideo = staticCompositionLocalOf<MutableState<String?>?> { null }

/** Bouton « Plein écran » de la carte vidéo, sous celui du son. */
@Composable
internal fun FullscreenButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledIconButton(
        onClick = onClick,
        colors = fixedButtonColors(),
        modifier = modifier.size(48.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_fullscreen),
            contentDescription = stringResource(R.string.deck_video_fullscreen),
        )
    }
}

/** Taille du bouton de lecture du plein écran. */
private val BIG_CONTROL_SIZE = 72.dp

/** Temps d'affichage des commandes pendant la lecture, en millisecondes. */
internal const val CONTROLS_VISIBLE_MILLIS = 3_000L

/** Rafraîchissement de la position pendant la lecture, en millisecondes. */
private const val PROGRESS_TICK_MILLIS = 250L

/** Étiquette de test du plein écran vidéo. */
internal const val FULLSCREEN_VIDEO_TAG = "fullscreen_video"
