package fr.babeuloula.photoswipe.ui.deck

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPresentationState
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.MediaType
import fr.babeuloula.photoswipe.ui.common.FilmEdgeChip

/**
 * Animation d'une photo animée sur la carte du dessus (PS-167) : jamais lancée seule, elle passe
 * par le lecteur de la pile quand l'utilisateur appuie sur le bouton, se joue une fois par-dessus
 * l'image, puis l'image fixe revient. Si elle ne se lit pas, le lecteur s'arrête et l'image reste,
 * sans message. La surface reste transparente jusqu'à la première image de l'animation.
 */
@OptIn(UnstableApi::class)
@Composable
internal fun BoxScope.motionPhotoPlayback(
    videoUri: String,
    videoPlayer: CardVideoPlayer,
): MotionPhotoPlayback {
    DisposableEffect(videoUri, videoPlayer) { onDispose { videoPlayer.stop(videoUri) } }
    val playing = videoPlayer.playingUri == videoUri
    val ended = videoPlayer.endedUri == videoUri
    // Une fois jouée, l'animation rend la place à l'image fixe, et le lecteur se libère.
    LaunchedEffect(ended) { if (ended) videoPlayer.stop(videoUri) }
    if (playing) {
        val presentation = rememberPresentationState(videoPlayer.player)
        Box(
            modifier = Modifier.matchParentSize().alpha(if (presentation.coverSurface) 0f else 1f),
            contentAlignment = Alignment.Center,
        ) {
            PlayerSurface(
                player = videoPlayer.player,
                surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
                // Comme l'image, l'animation remplit la carte.
                modifier = Modifier.resizeWithContentScale(ContentScale.Crop, presentation.videoSizeDp),
            )
        }
    }
    return MotionPhotoPlayback(playing) { if (playing) videoPlayer.stop(videoUri) else videoPlayer.play(videoUri) }
}

/**
 * État de l'animation de la carte du dessus.
 *
 * @property playing animation en cours.
 * @property toggle la lance, ou l'arrête pour revenir à l'image fixe.
 */
internal class MotionPhotoPlayback(
    val playing: Boolean,
    val toggle: () -> Unit,
)

/** Bouton qui joue l'animation, ou l'arrête pendant qu'elle se joue, dans le coin de la carte. */
@Composable
internal fun MotionPhotoButton(
    playback: MotionPhotoPlayback,
    modifier: Modifier = Modifier,
) {
    FilledIconButton(
        onClick = playback.toggle,
        colors =
            IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.onPrimaryFixed,
                contentColor = MaterialTheme.colorScheme.primaryFixed,
            ),
        // Cible de 48 dp, dans le coin de la carte : elle déborde de la marge du repère.
        modifier = modifier.offset(x = 8.dp, y = (-8).dp).size(48.dp),
    ) {
        Icon(
            painter =
                painterResource(
                    if (playback.playing) R.drawable.ic_stop_filled else R.drawable.ic_motion_photos_on,
                ),
            contentDescription =
                stringResource(
                    if (playback.playing) R.string.deck_motion_photo_stop else R.string.deck_motion_photo_play,
                ),
        )
    }
}

/** Pastille « Photo animée » (PS-167), au bas de la carte. */
@Composable
internal fun MotionPhotoChip(modifier: Modifier = Modifier) {
    val label = stringResource(R.string.deck_motion_photo)
    FilmEdgeChip(modifier = modifier.clearAndSetSemantics { contentDescription = label }) {
        Icon(
            painter = painterResource(R.drawable.ic_motion_photos_on),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Text(label)
    }
}

/**
 * Lecture de la carte du dessus ([active]) par le lecteur de la pile : sa vidéo, ou l'animation de
 * sa photo animée (PS-167), que seul le bouton lance.
 *
 * @property video vidéo de la carte, `null` pour une photo.
 * @property motionPhoto animation d'une photo animée, `null` sinon.
 */
internal class CardPlayback(
    val video: ActiveVideoState? = null,
    val motionPhoto: MotionPhotoPlayback? = null,
)

/**
 * Lecture de [card] : rien sans le lecteur de la pile (aperçus, tests) ni sous la carte du dessus.
 * [onRatio] reçoit les proportions d'une vidéo dès sa première image : la carte prend sa forme.
 */
@Composable
internal fun BoxScope.cardPlayback(
    card: MediaCardUiState,
    active: Boolean,
    onRatio: (Float) -> Unit = {},
): CardPlayback {
    val videoPlayer = LocalCardVideoPlayer.current
    return when {
        !active || videoPlayer == null -> {
            CardPlayback()
        }

        card.type == MediaType.VIDEO -> {
            CardPlayback(
                video = activeVideo(card = card, videoPlayer = videoPlayer, onRatio = onRatio),
            )
        }

        card.motionVideo != null -> {
            CardPlayback(motionPhoto = motionPhotoPlayback(card.motionVideo, videoPlayer))
        }

        else -> {
            CardPlayback()
        }
    }
}
