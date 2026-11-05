package fr.babeuloula.photoswipe.ui.deck

import androidx.annotation.OptIn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPresentationState
import coil3.ImageLoader
import coil3.compose.AsyncImage
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaType
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate

/**
 * Carte plein écran d'un souvenir (SPEC §4.3) : l'image, chargée par Coil à la taille de la carte,
 * et en bas l'étiquette « Il y a 3 ans · 14 mai 2023 » avec le poids du fichier. Sans état : elle
 * affiche [card] et rien d'autre.
 *
 * Si l'image ne s'ouvre pas (fichier disparu, données de démo sans fichier, vidéo dont la
 * miniature n'est pas encore gérée), une icône et un message remplacent l'image.
 *
 * [active] marque la carte du dessus : si c'est une vidéo, elle est lue en muet par le lecteur de
 * la pile ([LocalCardVideoPlayer]) ; la carte suivante, préparée dessous, reste sur sa miniature.
 */
@Composable
fun MediaCard(
    card: MediaCardUiState,
    modifier: Modifier = Modifier,
    active: Boolean = false,
) {
    val context = LocalContext.current
    val date = localizedDate(context, card.takenOn)
    var failed by remember(card.uri) { mutableStateOf(false) }
    val descriptionRes =
        if (card.type ==
            MediaType.VIDEO
        ) {
            R.string.deck_video_description
        } else {
            R.string.deck_photo_description
        }
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = card.uri,
                contentDescription = stringResource(descriptionRes, date),
                imageLoader = LocalImageLoader.current,
                placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentScale = ContentScale.Fit,
                onSuccess = { failed = false },
                onError = { failed = true },
                modifier = Modifier.fillMaxSize(),
            )
            if (failed) {
                Unavailable(type = card.type, modifier = Modifier.align(Alignment.Center))
            }
            val videoPlayer = LocalCardVideoPlayer.current
            if (active && card.type == MediaType.VIDEO && videoPlayer != null) {
                ActiveVideo(card = card, videoPlayer = videoPlayer)
            } else if (card.type == MediaType.VIDEO) {
                VideoBadge(
                    remaining = card.duration,
                    playing = false,
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                )
            }
            MemoryLabel(
                card = card,
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(12.dp),
            )
        }
    }
}

/**
 * Vidéo de la carte du dessus (SPEC §4.3) : elle démarre seule, en muet, [AUTOPLAY_DELAY] après
 * l'arrivée de la carte, est lue une fois, et s'arrête quand la carte part.
 *
 * Le son est coupé au départ ; le bouton en haut à droite l'active ou le coupe. Un gros bouton ▶
 * au centre attend la lecture ; au démarrage il devient ⏸ puis s'efface en fondu.
 * Un tap sur la vidéo la met en pause (le ▶ revient et reste) ou la relance. La pastille du haut
 * donne le temps restant ; à la fin de la vidéo, elle disparaît et seul le ▶ reste. La surface
 * reste transparente jusqu'à la première image, pour que la miniature ne laisse pas place à un
 * rectangle noir.
 */
@OptIn(UnstableApi::class)
@Composable
private fun BoxScope.ActiveVideo(
    card: MediaCardUiState,
    videoPlayer: CardVideoPlayer,
) {
    LaunchedEffect(card.uri, videoPlayer) {
        if (!videoPlayer.resumesImmediately(card.uri)) delay(AUTOPLAY_DELAY.toMillis())
        videoPlayer.play(card.uri)
    }
    DisposableEffect(card.uri, videoPlayer) { onDispose { videoPlayer.stop(card.uri) } }
    val playing = videoPlayer.playingUri == card.uri
    val ended = videoPlayer.endedUri == card.uri
    var position by remember(card.uri) { mutableStateOf(Duration.ZERO) }
    // Le temps restant n'avance que pendant la lecture ; en pause ou à la fin, il est lu une fois.
    LaunchedEffect(card.uri, playing) {
        position = videoPlayer.position(card.uri) ?: Duration.ZERO
        while (playing) {
            delay(PROGRESS_TICK.toMillis())
            position = videoPlayer.position(card.uri) ?: Duration.ZERO
        }
    }
    val presentation = rememberPresentationState(videoPlayer.player)
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
    PlayPauseOverlay(
        playing = playing,
        onToggle = { if (playing) videoPlayer.pauseByUser(card.uri) else videoPlayer.play(card.uri) },
        modifier = Modifier.matchParentSize(),
    )
    if (!ended) {
        VideoBadge(
            remaining = card.duration?.let { (it - position).coerceAtLeast(Duration.ZERO) },
            playing = playing,
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
        )
        SoundButton(
            muted = videoPlayer.muted,
            onToggle = videoPlayer::toggleSound,
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
        )
    }
}

/**
 * Zone de tap de la vidéo et son gros bouton central : ▶ tant qu'elle ne joue pas ; au démarrage,
 * ⏸ qui s'efface en fondu. Toute la carte réagit au tap, sans ondulation ; un glissement reste un
 * geste de tri. TalkBack lit l'action : « Lire la vidéo » ou « Mettre la vidéo en pause ».
 */
@Composable
private fun PlayPauseOverlay(
    playing: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonAlpha = remember { Animatable(1f) }
    LaunchedEffect(playing) {
        if (playing) {
            delay(PAUSE_ICON_VISIBLE.toMillis())
            buttonAlpha.animateTo(0f, tween(FADE_OUT_MILLIS))
        } else {
            buttonAlpha.snapTo(1f)
        }
    }
    val action = stringResource(if (playing) R.string.deck_video_pause else R.string.deck_video_play)
    Box(
        modifier =
            modifier
                .clickable(interactionSource = null, indication = null, onClick = onToggle)
                .semantics {
                    contentDescription = action
                    role = Role.Button
                },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = LABEL_ALPHA),
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
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
                                R.drawable.ic_pause,
                            )
                        } else {
                            rememberVectorPainter(Icons.Filled.PlayArrow)
                        },
                    contentDescription = null,
                    modifier = Modifier.size(BIG_ICON_SIZE),
                )
            }
        }
    }
}

/**
 * Bouton du son de la vidéo (SPEC §4.3) : coupé au départ, un tap l'active ou le coupe. Le réglage
 * suit l'utilisateur sur les vidéos suivantes de la pile.
 */
@Composable
private fun SoundButton(
    muted: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalIconButton(
        onClick = onToggle,
        colors =
            IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.inverseSurface.copy(alpha = LABEL_ALPHA),
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            ),
        modifier = modifier,
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
private val BIG_BUTTON_SIZE = 72.dp

/** Taille de son icône. */
private val BIG_ICON_SIZE = 44.dp

/** Temps pendant lequel ⏸ reste visible au démarrage, avant de s'effacer. */
private val PAUSE_ICON_VISIBLE: Duration = Duration.ofMillis(PAUSE_ICON_VISIBLE_MILLIS)

/** Durée du fondu de ⏸, en millisecondes. */
private const val FADE_OUT_MILLIS = 400

/** Étiquette de test du gros bouton central d'une vidéo. */
internal const val PLAY_PAUSE_BUTTON_TAG = "play_pause_button"

/** Attente avant la lecture automatique d'une vidéo, en millisecondes. */
private const val AUTOPLAY_DELAY_MILLIS = 2_500L

/** Temps pendant lequel ⏸ reste visible au démarrage, en millisecondes. */
private const val PAUSE_ICON_VISIBLE_MILLIS = 600L

/** Rafraîchissement du temps restant pendant la lecture, en millisecondes. */
private const val PROGRESS_TICK_MILLIS = 250L

/** Attente avant la lecture automatique d'une vidéo : le temps de voir sa miniature et de décider. */
internal val AUTOPLAY_DELAY: Duration = Duration.ofMillis(AUTOPLAY_DELAY_MILLIS)

/** Rafraîchissement du temps restant pendant la lecture. */
private val PROGRESS_TICK: Duration = Duration.ofMillis(PROGRESS_TICK_MILLIS)

/** Étiquette du bas de la carte : « Il y a 3 ans · 14 mai 2023 » et le poids du fichier. */
@Composable
private fun MemoryLabel(
    card: MediaCardUiState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = LABEL_ALPHA),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = memoryLabel(context, card.yearsAgo, card.takenOn),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = fileSizeLabel(context, card.sizeBytes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Pastille d'une vidéo (SPEC §4.3) : ▶ et la durée tant qu'elle ne joue pas, ⏸ et le temps restant
 * pendant la lecture. Simple information : TalkBack lit « Durée : 00:32 ».
 */
@Composable
private fun VideoBadge(
    remaining: Duration?,
    playing: Boolean,
    modifier: Modifier = Modifier,
) {
    val label = remaining?.let { durationLabel(it) }
    val description = label?.let { stringResource(R.string.deck_video_duration, it) }
    Surface(
        color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = LABEL_ALPHA),
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.clearAndSetSemantics { description?.let { contentDescription = it } },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                painter =
                    if (playing) {
                        painterResource(
                            R.drawable.ic_pause,
                        )
                    } else {
                        rememberVectorPainter(Icons.Filled.PlayArrow)
                    },
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            if (label != null) Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Image impossible à afficher : une icône du type de média et un court message. */
@Composable
private fun Unavailable(
    type: MediaType,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = if (type == MediaType.VIDEO) Icons.Filled.PlayArrow else Icons.Filled.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = stringResource(R.string.deck_media_unavailable),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Opacité du fond de l'étiquette : lisible sur n'importe quelle photo sans la masquer. */
private const val LABEL_ALPHA = 0.85f

/** Souvenir fixe des aperçus : une photo du 14 mai 2023, montrée le 14 mai 2026. */
internal val PREVIEW_CARD =
    MediaCardUiState(
        id = MediaId(1),
        uri = "content://media/external/images/media/1",
        type = MediaType.IMAGE,
        takenOn = LocalDate.parse("2023-05-14"),
        yearsAgo = 3,
        sizeBytes = 3_500_000,
    )

@PreviewLightDark
@Composable
private fun MediaCardPreview() {
    CompositionLocalProvider(LocalImageLoader provides ImageLoader(LocalContext.current)) {
        PhotoSwipeTheme(dynamicColor = false) {
            MediaCard(card = PREVIEW_CARD, modifier = Modifier.fillMaxSize())
        }
    }
}
