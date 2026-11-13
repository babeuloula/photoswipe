package fr.babeuloula.photoswipe.ui.deck

import androidx.annotation.DrawableRes
import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.isSpecified
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
import fr.babeuloula.photoswipe.domain.model.SimilarGroup
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
 * [overlay] se dessine par-dessus, sur toute la carte (voile du geste). Sans [details], ni repère ni
 * pastilles : c'est une carte du dessous de la pile (PS-147).
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
    overlay: @Composable BoxScope.() -> Unit = {},
    details: Boolean = true,
) {
    FittedCardBox(uri = card.uri, modifier = modifier) { size, onRatio ->
        Card(
            modifier = Modifier.size(size),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
            elevation = CardDefaults.cardElevation(defaultElevation = CARD_ELEVATION),
        ) {
            val date = localizedDate(LocalContext.current, card.takenOn)
            val compare = rememberCompare(card)
            var failed by remember(card.uri) { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = card.uri,
                    contentDescription = stringResource(cardDescription(card.type), date),
                    imageLoader = LocalImageLoader.current,
                    placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHighest),
                    // L'image remplit toujours sa carte : à sa forme, rien n'est rogné ; ailleurs, pas de bande grise.
                    contentScale = ContentScale.Crop,
                    onSuccess = { success ->
                        failed = false
                        val intrinsic = success.painter.intrinsicSize
                        if (intrinsic.isSpecified &&
                            intrinsic.height > 0f
                        ) {
                            onRatio(intrinsic.width / intrinsic.height)
                        }
                    },
                    onError = { failed = true },
                    modifier = Modifier.fillMaxSize().openOnTap(card.type, onOpen, compare.takeIf { active }),
                )
                if (compare.showing && active) ComparedPhoto(card)
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
                // Repère et pastilles : pas sur les cartes du dessous de la pile, dont seuls les bords dépassent.
                if (details) {
                    TopRow(
                        card = card,
                        modifier = Modifier.align(Alignment.TopStart).fillMaxWidth().padding(CARD_PADDING),
                    ) {
                        if (video != null) VideoCornerButtons(video)
                    }
                    CardBottom(
                        card = card,
                        video = video,
                        modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(CARD_PADDING),
                    )
                }
                overlay()
            }
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
    val positionState = remember(card.uri) { mutableStateOf(Duration.ZERO) }
    var position by positionState
    // Plein écran (PS-119) : vidéo ouverte en plein écran, tenue par la pile pour survivre à la
    // recréation de l'activité par une rotation (PS-126) ; à défaut, par la carte.
    val ownFullscreen = rememberSaveable { mutableStateOf<String?>(null) }
    val fullscreenUri = LocalFullscreenVideo.current ?: ownFullscreen
    val fullscreen = fullscreenUri.value == card.uri
    if (fullscreen) FullscreenVideo(card = card, videoPlayer = videoPlayer, onExit = { fullscreenUri.value = null })
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
        // En plein écran, la vidéo est affichée par sa fenêtre : un lecteur n'a qu'une surface à la fois.
        if (!fullscreen) {
            PlayerSurface(
                player = videoPlayer.player,
                surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
                modifier = Modifier.resizeWithContentScale(ContentScale.Fit, presentation.videoSizeDp),
            )
        }
    }
    PlayPauseOverlay(
        playing = playing,
        onToggle = { if (playing) videoPlayer.pauseByUser(card.uri) else videoPlayer.playByUser(card.uri) },
        modifier = Modifier.matchParentSize(),
    )
    return ActiveVideoState(
        uri = card.uri,
        positionState = positionState,
        duration = card.duration,
        player = videoPlayer,
        openFullscreen = { fullscreenUri.value = card.uri },
    )
}

/** Boutons du son et du plein écran (PS-119), l'un sous l'autre dans le coin de la vidéo du dessus. */
@Composable
private fun VideoCornerButtons(video: ActiveVideoState) {
    // Cibles de 48 dp, dans le coin de la carte : elles débordent de la marge du repère.
    Column(
        modifier = Modifier.offset(x = 8.dp, y = (-8).dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!video.ended) SoundButton(muted = video.muted, onToggle = video::toggleSound)
        FullscreenButton(onClick = video.openFullscreen)
    }
}

/** Bas de la carte : la barre de progression de la vidéo du dessus (PS-118), puis les pastilles. */
@Composable
private fun CardBottom(
    card: MediaCardUiState,
    video: ActiveVideoState?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        val total = card.duration?.takeIf { !it.isZero && !it.isNegative }
        if (video != null && total != null) {
            VideoProgressBar(position = video.position, duration = total, onSeek = video::seek)
        }
        BottomChips(
            sizeBytes = card.sizeBytes,
            duration = shownDuration(card, video),
            isVideo = card.type == MediaType.VIDEO && video?.ended != true,
            playing = video?.playing == true,
            similar = card.similar,
        )
    }
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
 * État de la vidéo de la carte du dessus, lu par les pastilles, la barre de progression et les
 * boutons du son et du plein écran.
 *
 * @property uri vidéo de la carte.
 * @property duration durée totale, si elle est connue.
 * @property openFullscreen ouvre la vidéo en plein écran (PS-119).
 */
private class ActiveVideoState(
    val uri: String,
    private val positionState: MutableState<Duration>,
    val duration: Duration?,
    private val player: CardVideoPlayer,
    val openFullscreen: () -> Unit,
) {
    /** Vidéo en cours de lecture. */
    val playing: Boolean get() = player.playingUri == uri

    /** Vidéo lue jusqu'au bout. */
    val ended: Boolean get() = player.endedUri == uri

    /** Position de lecture. */
    val position: Duration get() = positionState.value

    /** Temps restant, si la durée est connue. */
    val remaining: Duration? get() = duration?.let { (it - position).coerceAtLeast(Duration.ZERO) }

    /** Son coupé. */
    val muted: Boolean get() = player.muted

    /** Active ou coupe le son. */
    fun toggleSound() = player.toggleSound()

    /** Déplace la lecture (barre de progression, PS-118). */
    fun seek(target: Duration) {
        player.seekTo(uri, target)
        positionState.value = target
    }
}

/** Attente avant la lecture automatique d'une vidéo, en millisecondes. */
private const val AUTOPLAY_DELAY_MILLIS = 2_500L

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
 * pendant la lecture ; puis le poids du fichier, et pour une photo prise à la suite d'autres
 * ([similar], PS-140) sa place parmi elles. TalkBack lit « Durée : 00:32 », « Photo 1 sur 3 prises à la suite ».
 */
@Composable
private fun BottomChips(
    sizeBytes: Long,
    duration: Duration?,
    isVideo: Boolean,
    playing: Boolean,
    modifier: Modifier = Modifier,
    similar: SimilarGroup? = null,
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
        similar?.let { SimilarChip(it) }
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

/** Élévation de la carte : une légère ombre portée, plus marquée en bas, qui la détache de celle d'en dessous (PS-142). */
private val CARD_ELEVATION = 6.dp

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
