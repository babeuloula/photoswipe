package fr.babeuloula.photoswipe.ui.deck

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.MediaType
import fr.babeuloula.photoswipe.ui.common.FilmEdgeLabel
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Photo de la carte du dessus en grand (PS-117), dans une fenêtre plein écran sur fond noir :
 * photo entière, pincement pour zoomer (jusqu'à ×5), glissement pour se déplacer dans la photo
 * zoomée, double appui pour zoomer ou revenir à la photo entière. À la taille d'origine, un
 * glissement vers le bas, le retour ou « Fermer » referme la vue, sans décision sur la carte.
 *
 * L'écran reste en portrait : seul le plein écran vidéo suit l'orientation du téléphone.
 */
@Composable
fun PhotoViewer(
    card: MediaCardUiState,
    onClose: () -> Unit,
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        PhotoViewerContent(card = card, onClose = onClose)
    }
}

/** Contenu de [PhotoViewer], sans sa fenêtre. */
@Composable
internal fun PhotoViewerContent(
    card: MediaCardUiState,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val zoom = remember(card.uri) { ZoomState() }
    // Glissement vers le bas de la photo entière, qui la ferme au-delà du seuil.
    val dismiss = remember(card.uri) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag(PHOTO_VIEWER_TAG),
    ) {
        AsyncImage(
            model = card.uri,
            contentDescription = stringResource(R.string.deck_photo_description, localizedDate(context, card.takenOn)),
            imageLoader = LocalImageLoader.current,
            contentScale = ContentScale.Fit,
            modifier =
                Modifier
                    .fillMaxSize()
                    .onSizeChanged { zoom.size = Size(it.width.toFloat(), it.height.toFloat()) }
                    .zoomOrDismiss(card.uri, zoom, dismiss, scope, onClose)
                    .graphicsLayer {
                        scaleX = zoom.scale
                        scaleY = zoom.scale
                        translationX = zoom.offset.x
                        translationY = zoom.offset.y + dismiss.value
                        alpha = 1f - (abs(dismiss.value) / size.height).coerceIn(0f, MAX_DISMISS_FADE)
                    },
        )
        ViewerTopBar(card = card, onClose = onClose, modifier = Modifier.align(Alignment.TopStart))
    }
}

/** Haut de la vue : le repère « Il y a 3 ans · … » et « Fermer ». */
@Composable
private fun ViewerTopBar(
    card: MediaCardUiState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .safeDrawingPadding()
                .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            FilmEdgeLabel(text = memoryLabel(LocalContext.current, card.yearsAgo, card.takenOn))
        }
        FilledIconButton(
            onClick = onClose,
            colors =
                IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimaryFixed,
                    contentColor = MaterialTheme.colorScheme.primaryFixed,
                ),
            modifier = Modifier.size(48.dp),
        ) {
            Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.photo_viewer_close))
        }
    }
}

/**
 * Gestes de la vue : double appui pour zoomer ; pincement ou glissement de la photo zoomée pour
 * zoomer et se déplacer ; glissement vers le bas de la photo entière, qui la ferme au-delà du seuil.
 */
private fun Modifier.zoomOrDismiss(
    key: Any,
    zoom: ZoomState,
    dismiss: Animatable<Float, *>,
    scope: CoroutineScope,
    onClose: () -> Unit,
): Modifier =
    pointerInput(key) {
        detectTapGestures(onDoubleTap = { zoom.toggle(it) })
    }.pointerInput(key) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            do {
                val event = awaitPointerEvent()
                val pan = event.calculatePan()
                val factor = event.calculateZoom()
                if (pan != Offset.Zero || factor != 1f) {
                    if (zoom.zoomed || factor != 1f || event.changes.size > 1) {
                        zoom.transform(event.calculateCentroid(useCurrent = true), pan, factor)
                    } else {
                        scope.launch { dismiss.snapTo((dismiss.value + pan.y).coerceAtLeast(0f)) }
                    }
                    event.changes.forEach { it.consume() }
                }
            } while (event.changes.any { it.pressed })
            if (dismiss.value > size.height * DISMISS_FRACTION) {
                onClose()
            } else if (dismiss.value != 0f) {
                scope.launch { dismiss.animateTo(0f) }
            }
        }
    }

/** Appui sur une photo : l'ouvre en grand (PS-117), annoncé « Voir en grand » par TalkBack. */
@Composable
internal fun Modifier.openOnTap(
    type: MediaType,
    onOpen: (() -> Unit)?,
): Modifier =
    if (onOpen != null && type == MediaType.IMAGE) {
        clickable(
            interactionSource = null,
            indication = null,
            onClickLabel = stringResource(R.string.photo_viewer_open),
            role = Role.Image,
            onClick = onOpen,
        )
    } else {
        this
    }

/** Part de la hauteur à glisser vers le bas pour fermer la vue. */
private const val DISMISS_FRACTION = 0.2f

/** Fondu maximal de la photo pendant ce glissement. */
private const val MAX_DISMISS_FADE = 0.6f

/** Étiquette de test de la vue en grand. */
internal const val PHOTO_VIEWER_TAG = "photo_viewer"
