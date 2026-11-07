package fr.babeuloula.photoswipe.ui.deck

import androidx.annotation.DrawableRes
import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
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
import fr.babeuloula.photoswipe.domain.model.ReviewDecision
import fr.babeuloula.photoswipe.ui.common.FilmEdgeChip
import fr.babeuloula.photoswipe.ui.common.FilmEdgeLabel
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import fr.babeuloula.photoswipe.ui.theme.motion
import fr.babeuloula.photoswipe.ui.theme.swipeColors
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate

/**
 * Carte plein écran d'un souvenir (SPEC §4.3), arrondie à 28 dp : l'image, chargée par Coil à la
 * taille de la carte ; en haut le repère de bord de film « Il y a 3 ans · 6 nov. 2023 » et, s'il y a
 * lieu, le cœur des favoris ; en bas le poids du fichier et, pour une vidéo, sa durée (ADR 0025).
 * Sans état : elle affiche [card] et rien d'autre.
 *
 * Si l'image ne s'ouvre pas (fichier disparu, données de démo sans fichier, vidéo dont la
 * miniature n'est pas encore gérée), une icône et un message remplacent l'image.
 *
 * [active] marque la carte du dessus : si c'est une vidéo, elle est lue en muet par le lecteur de
 * la pile ([LocalCardVideoPlayer]) ; la carte suivante, préparée dessous, reste sur sa miniature.
 *
 * [onOpen], s'il est donné, est appelé par un appui sur une photo : la pile l'ouvre en grand
 * (PS-117). Sur une vidéo, l'appui garde son rôle de lecture et de pause.
 */
@Composable
fun MediaCard(
    card: MediaCardUiState,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    onOpen: (() -> Unit)? = null,
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
        shape = MaterialTheme.shapes.extraLarge,
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
                modifier = Modifier.fillMaxSize().openOnTap(card.type, onOpen),
            )
            if (failed) {
                Unavailable(type = card.type, modifier = Modifier.align(Alignment.Center))
            }
            val videoPlayer = LocalCardVideoPlayer.current
            val video =
                if (active && card.type == MediaType.VIDEO && videoPlayer != null) {
                    activeVideo(card = card, videoPlayer = videoPlayer)
                } else {
                    null
                }
            TopRow(
                card = card,
                modifier = Modifier.align(Alignment.TopStart).fillMaxWidth().padding(CARD_PADDING),
            ) {
                if (video != null && !video.ended) {
                    SoundButton(muted = video.muted, onToggle = video.toggleSound)
                }
            }
            BottomChips(
                sizeBytes = card.sizeBytes,
                duration = shownDuration(card, video),
                isVideo = card.type == MediaType.VIDEO && video?.ended != true,
                playing = video?.playing == true,
                modifier = Modifier.align(Alignment.BottomStart).padding(CARD_PADDING),
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
 * Un tap sur la vidéo la met en pause (le ▶ revient et reste) ou la relance avec le son. La pastille du haut
 * donne le temps restant ; à la fin de la vidéo, elle disparaît et seul le ▶ reste. La surface
 * reste transparente jusqu'à la première image, pour que la miniature ne laisse pas place à un
 * rectangle noir.
 */
@OptIn(UnstableApi::class)
@Composable
private fun BoxScope.activeVideo(
    card: MediaCardUiState,
    videoPlayer: CardVideoPlayer,
): ActiveVideoState {
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
        onToggle = { if (playing) videoPlayer.pauseByUser(card.uri) else videoPlayer.playByUser(card.uri) },
        modifier = Modifier.matchParentSize(),
    )
    return ActiveVideoState(
        playing = playing,
        ended = ended,
        remaining = card.duration?.let { (it - position).coerceAtLeast(Duration.ZERO) },
        muted = videoPlayer.muted,
        toggleSound = videoPlayer::toggleSound,
    )
}

/** Durée affichée d'une vidéo : totale au repos, restante pendant la lecture, rien à la fin. */
private fun shownDuration(
    card: MediaCardUiState,
    video: ActiveVideoState?,
): Duration? =
    when {
        card.type != MediaType.VIDEO -> null
        video == null -> card.duration
        video.ended -> null
        else -> video.remaining
    }

/**
 * État de la vidéo de la carte du dessus, lu par les pastilles et le bouton du son.
 *
 * @property playing vidéo en cours de lecture.
 * @property ended vidéo lue jusqu'au bout.
 * @property remaining temps restant, si la durée est connue.
 * @property muted son coupé.
 * @property toggleSound active ou coupe le son.
 */
private class ActiveVideoState(
    val playing: Boolean,
    val ended: Boolean,
    val remaining: Duration?,
    val muted: Boolean,
    val toggleSound: () -> Unit,
)

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
                .clickable(interactionSource = null, indication = null, onClick = onToggle)
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
    FilledIconButton(
        onClick = onToggle,
        colors =
            IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.onPrimaryFixed,
                contentColor = MaterialTheme.colorScheme.primaryFixed,
            ),
        // Cible de 48 dp, dans le coin de la carte : déborde de la marge du repère.
        modifier = modifier.offset(x = 8.dp, y = (-8).dp).size(48.dp),
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

/**
 * Haut de la carte : le repère de bord de film « Il y a 3 ans · 6 nov. 2023 », le cœur d'un média
 * déjà en favori (PS-86) et, pour la vidéo du dessus, [trailing] (le bouton du son).
 */
@Composable
private fun TopRow(
    card: MediaCardUiState,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit,
) {
    val context = LocalContext.current
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Le repère prend toute la place laissée par le cœur et le bouton du son, sans être coupé.
        Box(modifier = Modifier.weight(1f)) {
            FilmEdgeLabel(text = memoryLabel(context, card.yearsAgo, card.takenOn))
        }
        // Décision d'avant, sur un jour rouvert pour être retrié (ADR 0026) ; le favori a son cœur.
        card.previousDecision?.let { PreviousDecisionBadge(it) }
        // Déjà en favori dans la galerie, ou en attente dans l'application (PS-86).
        if (card.favorite) {
            Surface(
                color = MaterialTheme.colorScheme.onPrimaryFixed,
                contentColor = MaterialTheme.swipeColors.favoriteOnFixed,
                shape = CircleShape,
                modifier = Modifier.size(FAVORITE_BADGE_SIZE),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_favorite_filled),
                        contentDescription = stringResource(R.string.deck_already_favorite),
                    )
                }
            }
        }
        trailing()
    }
}

/**
 * Pastille de la décision prise la dernière fois sur ce média, quand son jour est retrié : coche
 * verte pour un média gardé, corbeille rouge pour un média marqué pour suppression. TalkBack la lit.
 */
@Composable
private fun PreviousDecisionBadge(decision: ReviewDecision) {
    val colors = MaterialTheme.swipeColors
    val style =
        when (decision) {
            ReviewDecision.MARKED_FOR_DELETION -> {
                PreviousDecisionStyle(
                    R.drawable.ic_delete,
                    colors.deleteContainer,
                    colors.onDeleteContainer,
                    R.string.deck_previously_marked,
                )
            }

            else -> {
                PreviousDecisionStyle(
                    R.drawable.ic_check,
                    colors.keepContainer,
                    colors.onKeepContainer,
                    R.string.deck_previously_kept,
                )
            }
        }
    Surface(
        color = style.container,
        contentColor = style.content,
        shape = CircleShape,
        modifier = Modifier.size(FAVORITE_BADGE_SIZE),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painter = painterResource(style.icon), contentDescription = stringResource(style.description))
        }
    }
}

/** Icône, couleurs et description de la pastille d'une décision précédente. */
private data class PreviousDecisionStyle(
    @param:DrawableRes val icon: Int,
    val container: Color,
    val content: Color,
    @param:StringRes val description: Int,
)

/**
 * Bas de la carte : pour une vidéo ([isVideo]), ▶ et la durée au repos, ⏸ et le temps restant
 * pendant la lecture ; puis le poids du fichier. TalkBack lit « Durée : 00:32 ».
 */
@Composable
private fun BottomChips(
    sizeBytes: Long,
    duration: Duration?,
    isVideo: Boolean,
    playing: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (isVideo) {
            val label = duration?.let { durationLabel(it) }
            val description = label?.let { stringResource(R.string.deck_video_duration, it) }
            FilmEdgeChip(modifier = Modifier.clearAndSetSemantics { description?.let { contentDescription = it } }) {
                Icon(
                    painter =
                        painterResource(
                            if (playing) R.drawable.ic_pause_filled else R.drawable.ic_play_arrow_filled,
                        ),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                if (label != null) Text(label)
            }
        }
        FilmEdgeChip { Text(fileSizeLabel(context, sizeBytes)) }
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
            painter =
                painterResource(
                    if (type ==
                        MediaType.VIDEO
                    ) {
                        R.drawable.ic_play_arrow_filled
                    } else {
                        R.drawable.ic_warning
                    },
                ),
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

/** Marge du repère et des pastilles dans la carte. */
private val CARD_PADDING = 16.dp

/** Taille de la pastille du cœur des favoris. */
private val FAVORITE_BADGE_SIZE = 40.dp

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
        PhotoSwipeTheme {
            MediaCard(card = PREVIEW_CARD, modifier = Modifier.fillMaxSize())
        }
    }
}
