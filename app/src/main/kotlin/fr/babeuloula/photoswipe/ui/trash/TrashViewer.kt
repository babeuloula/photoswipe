package fr.babeuloula.photoswipe.ui.trash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.MediaType
import fr.babeuloula.photoswipe.domain.model.TrashItem
import fr.babeuloula.photoswipe.ui.common.AllowRotation
import fr.babeuloula.photoswipe.ui.common.FilmEdgeLabel
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader
import fr.babeuloula.photoswipe.ui.deck.CardVideoPlayer
import fr.babeuloula.photoswipe.ui.deck.FullscreenVideo
import fr.babeuloula.photoswipe.ui.deck.MediaCardUiState
import fr.babeuloula.photoswipe.ui.deck.ZoomablePhoto
import fr.babeuloula.photoswipe.ui.deck.fixedButtonColors
import fr.babeuloula.photoswipe.ui.deck.localizedDate
import fr.babeuloula.photoswipe.ui.deck.rememberCardVideoPlayer
import java.time.LocalDate
import java.time.ZoneId

/**
 * Médias de la corbeille en grand (PS-188), dans une fenêtre plein écran sur fond noir qui suit
 * l'orientation du téléphone. On les fait défiler d'un glissement horizontal ; une photo se zoome
 * comme dans la pile (`ZoomablePhoto`), le défilement s'arrêtant tant qu'elle est zoomée ; une
 * vidéo montre sa première image et se lit en plein écran avec le lecteur de la pile.
 *
 * En haut, la place du média (« 2 sur 5 ») et « Fermer » ; en bas, « Supprimer définitivement »
 * (bouton à icône) et « Restaurer », qui envoient les mêmes événements que les boutons de la liste.
 * Le retour, « Fermer » ou un glissement vers le bas de la photo entière ramène à la liste. Les
 * annonces de la page s'affichent aussi par-dessus la vue.
 */
@Composable
internal fun TrashViewer(
    state: TrashUiState.Loaded,
    onEvent: (TrashEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val index = state.viewedIndex ?: return
    Dialog(
        onDismissRequest = { onEvent(TrashEvent.View(null)) },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        AllowRotation()
        TrashViewerContent(state = state, index = index, onEvent = onEvent, snackbarHostState = snackbarHostState)
    }
}

/** Contenu de [TrashViewer], sans sa fenêtre, ouvert sur le média à la place [index] de la corbeille. */
@Composable
internal fun TrashViewerContent(
    state: TrashUiState.Loaded,
    index: Int,
    onEvent: (TrashEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val items = state.trash.items
    val pager = rememberPagerState(initialPage = index) { items.size }
    // Le ViewModel choisit le média ouvert (par exemple le suivant après une restauration) : la vue
    // s'y place ; un défilement de l'utilisateur lui est renvoyé une fois la page posée.
    LaunchedEffect(index) { if (pager.currentPage != index) pager.scrollToPage(index) }
    val currentItems by rememberUpdatedState(items)
    val currentIndex by rememberUpdatedState(index)
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect { page ->
            if (page != currentIndex) currentItems.getOrNull(page)?.let { onEvent(TrashEvent.View(it.media.id)) }
        }
    }
    // Un seul lecteur pour les vidéos de la corbeille, créé seulement s'il y en a.
    val videoPlayer = if (items.any { it.media.type == MediaType.VIDEO }) rememberCardVideoPlayer() else null
    val close = { onEvent(TrashEvent.View(null)) }
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag(TRASH_VIEWER_TAG),
    ) {
        HorizontalPager(
            state = pager,
            key = { items[it].media.id.value },
            modifier = Modifier.fillMaxSize().testTag(TRASH_VIEWER_PAGER_TAG),
        ) { page ->
            val item = items[page]
            if (item.media.type == MediaType.VIDEO && videoPlayer != null) {
                TrashVideo(item, videoPlayer)
            } else {
                ZoomablePhoto(
                    uri = item.media.uri,
                    contentDescription =
                        stringResource(
                            R.string.deck_photo_description,
                            localizedDate(LocalContext.current, takenOn(item)),
                        ),
                    onClose = close,
                )
            }
        }
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
            ViewerTopBar(position = index + 1, total = items.size, onClose = close)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
            }
            ViewerActions(item = items[index], enabled = !state.busy, onEvent = onEvent)
        }
    }
}

/** Haut de la vue : la place du média, « 2 sur 5 », et « Fermer ». */
@Composable
private fun ViewerTopBar(
    position: Int,
    total: Int,
    onClose: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) {
            FilmEdgeLabel(text = stringResource(R.string.deck_progress, position, total))
        }
        FilledIconButton(onClick = onClose, colors = fixedButtonColors(), modifier = Modifier.size(48.dp)) {
            Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.photo_viewer_close))
        }
    }
}

/** Bas de la vue : « Supprimer définitivement » (icône) et « Restaurer ». */
@Composable
private fun ViewerActions(
    item: TrashItem,
    enabled: Boolean,
    onEvent: (TrashEvent) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledIconButton(
            onClick = { onEvent(TrashEvent.DeleteForever(item.media.id)) },
            enabled = enabled,
            colors =
                IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            modifier = Modifier.size(ACTION_HEIGHT),
        ) {
            Icon(
                painterResource(R.drawable.ic_delete_forever),
                contentDescription = stringResource(R.string.trash_delete_forever),
            )
        }
        Button(
            onClick = { onEvent(TrashEvent.Restore(item.media.id)) },
            enabled = enabled,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryFixed,
                    contentColor = MaterialTheme.colorScheme.onPrimaryFixed,
                ),
            modifier = Modifier.weight(1f).height(ACTION_HEIGHT),
        ) {
            Text(stringResource(R.string.trash_restore))
        }
    }
}

/**
 * Vidéo de la corbeille : sa première image en grand et ▶ au centre, qui la lit en plein écran
 * avec [videoPlayer], le lecteur de la vue.
 */
@Composable
private fun TrashVideo(
    item: TrashItem,
    videoPlayer: CardVideoPlayer,
) {
    val context = LocalContext.current
    var fullscreen by rememberSaveable(item.media.uri) { mutableStateOf(false) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AsyncImage(
            model = item.media.uri,
            contentDescription = stringResource(R.string.deck_video_description, localizedDate(context, takenOn(item))),
            imageLoader = LocalImageLoader.current,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
        FilledIconButton(
            onClick = {
                videoPlayer.playByUser(item.media.uri)
                fullscreen = true
            },
            colors = fixedButtonColors(),
            modifier = Modifier.size(PLAY_BUTTON_SIZE).testTag(TRASH_VIEWER_PLAY_TAG),
        ) {
            Icon(
                painterResource(R.drawable.ic_play_arrow_filled),
                contentDescription = stringResource(R.string.deck_video_play),
                modifier = Modifier.size(32.dp),
            )
        }
    }
    if (fullscreen) {
        FullscreenVideo(
            card = MediaCardUiState.of(item.media, LocalDate.now(), ZoneId.systemDefault()),
            videoPlayer = videoPlayer,
            onExit = {
                videoPlayer.pauseByUser(item.media.uri)
                fullscreen = false
            },
        )
    }
}

/** Jour local de la prise de vue du média. */
private fun takenOn(item: TrashItem): LocalDate =
    item.media.takenAt
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

/** Hauteur des boutons du bas, et côté du bouton à icône. */
private val ACTION_HEIGHT = 56.dp

/** Taille du bouton ▶ d'une vidéo. */
private val PLAY_BUTTON_SIZE = 72.dp

/** Étiquette de test de la vue en grand. */
internal const val TRASH_VIEWER_TAG = "trash_viewer"

/** Étiquette de test du défilement des médias. */
internal const val TRASH_VIEWER_PAGER_TAG = "trash_viewer_pager"

/** Étiquette de test du bouton ▶ d'une vidéo. */
internal const val TRASH_VIEWER_PLAY_TAG = "trash_viewer_play"
