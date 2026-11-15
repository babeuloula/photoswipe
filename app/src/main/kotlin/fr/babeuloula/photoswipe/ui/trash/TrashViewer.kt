package fr.babeuloula.photoswipe.ui.trash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import fr.babeuloula.photoswipe.ui.deck.FullscreenVideo
import fr.babeuloula.photoswipe.ui.deck.MediaCardUiState
import fr.babeuloula.photoswipe.ui.deck.ZoomablePhoto
import fr.babeuloula.photoswipe.ui.deck.fixedButtonColors
import fr.babeuloula.photoswipe.ui.deck.localizedDate
import fr.babeuloula.photoswipe.ui.deck.rememberCardVideoPlayer
import java.time.LocalDate
import java.time.ZoneId

/**
 * Média de la corbeille ouvert en grand (PS-188), dans une fenêtre plein écran sur fond noir qui
 * suit l'orientation du téléphone : une photo se zoome comme dans la pile (`ZoomablePhoto`), une
 * vidéo montre sa première image et se lit en plein écran avec le lecteur de la pile.
 *
 * En haut, la place du média (« 2 sur 5 ») et « Fermer » ; en bas, les flèches vers le média
 * précédent et suivant, « Supprimer définitivement » et « Restaurer », qui envoient les mêmes
 * événements que les boutons de la liste. Le retour, « Fermer » ou un glissement vers le bas de la
 * photo entière ramène à la liste. Les annonces de la page s'affichent aussi par-dessus la vue.
 */
@Composable
internal fun TrashViewer(
    state: TrashUiState.Loaded,
    onEvent: (TrashEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val index = state.viewedIndex ?: return
    val close = { onEvent(TrashEvent.View(null)) }
    Dialog(
        onDismissRequest = close,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        AllowRotation()
        TrashViewerContent(state = state, index = index, onEvent = onEvent, snackbarHostState = snackbarHostState)
    }
}

/** Contenu de [TrashViewer], sans sa fenêtre : le média à la place [index] de la corbeille. */
@Composable
internal fun TrashViewerContent(
    state: TrashUiState.Loaded,
    index: Int,
    onEvent: (TrashEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val items = state.trash.items
    val item = items[index]
    val close = { onEvent(TrashEvent.View(null)) }
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag(TRASH_VIEWER_TAG),
    ) {
        if (item.media.type == MediaType.VIDEO) {
            TrashVideo(item)
        } else {
            ZoomablePhoto(
                uri = item.media.uri,
                contentDescription =
                    stringResource(R.string.deck_photo_description, localizedDate(LocalContext.current, takenOn(item))),
                onClose = close,
            )
        }
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) {
                    FilmEdgeLabel(text = stringResource(R.string.deck_progress, index + 1, items.size))
                }
                FilledIconButton(onClick = close, colors = fixedButtonColors(), modifier = Modifier.size(48.dp)) {
                    Icon(
                        painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.photo_viewer_close),
                    )
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
            }
            ViewerActions(state = state, index = index, onEvent = onEvent)
        }
    }
}

/** Bas de la vue : média précédent, « Supprimer définitivement », « Restaurer », média suivant. */
@Composable
private fun ViewerActions(
    state: TrashUiState.Loaded,
    index: Int,
    onEvent: (TrashEvent) -> Unit,
) {
    val items = state.trash.items
    val id = items[index].media.id
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavigationButton(
            icon = R.drawable.ic_chevron_left,
            label = R.string.trash_viewer_previous,
            target = items.getOrNull(index - 1),
            onEvent = onEvent,
        )
        Button(
            onClick = { onEvent(TrashEvent.DeleteForever(id)) },
            enabled = !state.busy,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.trash_delete_forever), maxLines = 2)
        }
        Button(
            onClick = { onEvent(TrashEvent.Restore(id)) },
            enabled = !state.busy,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryFixed,
                    contentColor = MaterialTheme.colorScheme.onPrimaryFixed,
                ),
            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.trash_restore), maxLines = 2)
        }
        NavigationButton(
            icon = R.drawable.ic_chevron_right,
            label = R.string.trash_viewer_next,
            target = items.getOrNull(index + 1),
            onEvent = onEvent,
        )
    }
}

/** Flèche vers le média [target], désactivée au bout de la corbeille. */
@Composable
private fun NavigationButton(
    icon: Int,
    label: Int,
    target: TrashItem?,
    onEvent: (TrashEvent) -> Unit,
) {
    FilledIconButton(
        onClick = { target?.let { onEvent(TrashEvent.View(it.media.id)) } },
        enabled = target != null,
        colors = fixedButtonColors(),
        modifier = Modifier.size(48.dp),
    ) {
        Icon(painterResource(icon), contentDescription = stringResource(label))
    }
}

/**
 * Vidéo de la corbeille : sa première image en grand et ▶ au centre, qui la lit en plein écran
 * avec le lecteur de la pile (créé seulement pour une vidéo, libéré en la quittant).
 */
@Composable
private fun TrashVideo(item: TrashItem) {
    val context = LocalContext.current
    val videoPlayer = rememberCardVideoPlayer()
    var fullscreen by rememberSaveable(item.media.uri) { mutableStateOf(false) }
    val description = stringResource(R.string.deck_video_description, localizedDate(context, takenOn(item)))
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AsyncImage(
            model = item.media.uri,
            contentDescription = description,
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

/** Taille du bouton ▶ d'une vidéo. */
private val PLAY_BUTTON_SIZE = 72.dp

/** Étiquette de test de la vue en grand. */
internal const val TRASH_VIEWER_TAG = "trash_viewer"

/** Étiquette de test du bouton ▶ d'une vidéo. */
internal const val TRASH_VIEWER_PLAY_TAG = "trash_viewer_play"
