package fr.babeuloula.photoswipe.ui.deck

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
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
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.MediaType
import fr.babeuloula.photoswipe.ui.common.AllowRotation
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
 * Comme le plein écran vidéo, elle suit l'orientation du téléphone (PS-123, ADR 0028) ; le reste
 * de l'application reste en portrait. Une rotation repart de la photo entière.
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
        AllowRotation()
        PhotoViewerContent(card = card, onClose = onClose)
    }
}

/**
 * Photo ouverte en grand (identifiant du média, `null` sinon), tenue par la pile (`LoadedDeck`)
 * et enregistrée avec l'état de l'écran : une rotation recrée l'activité, la photo reste ouverte
 * (PS-123). `null` hors d'une pile.
 */
val LocalOpenPhoto = staticCompositionLocalOf<MutableState<Long?>?> { null }

/** Contenu de [PhotoViewer], sans sa fenêtre. */
@Composable
internal fun PhotoViewerContent(
    card: MediaCardUiState,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag(PHOTO_VIEWER_TAG),
    ) {
        ZoomablePhoto(
            uri = card.uri,
            contentDescription = stringResource(R.string.deck_photo_description, localizedDate(context, card.takenOn)),
            onClose = onClose,
        )
        ViewerTopBar(card = card, onClose = onClose, modifier = Modifier.align(Alignment.TopStart))
    }
}

/**
 * Photo [uri] entière, zoomable : pincement (jusqu'à ×5), glissement de la photo zoomée, double
 * appui pour zoomer ou revenir à la photo entière ; à la taille d'origine, un glissement vers le
 * bas appelle [onClose]. Partagée par la photo en grand de la pile et celle de la corbeille (PS-188).
 */
@Composable
internal fun ZoomablePhoto(
    uri: String,
    contentDescription: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val zoom = remember(uri) { ZoomState() }
    // Glissement vers le bas de la photo entière, qui la ferme au-delà du seuil.
    val dismiss = remember(uri) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    AsyncImage(
        model = uri,
        contentDescription = contentDescription,
        imageLoader = LocalImageLoader.current,
        contentScale = ContentScale.Fit,
        modifier =
            modifier
                .fillMaxSize()
                .onSizeChanged { zoom.size = Size(it.width.toFloat(), it.height.toFloat()) }
                .zoomOrDismiss(uri, zoom, dismiss, scope, onClose)
                .graphicsLayer {
                    scaleX = zoom.scale
                    scaleY = zoom.scale
                    translationX = zoom.offset.x
                    translationY = zoom.offset.y + dismiss.value
                    alpha = 1f - (abs(dismiss.value) / size.height).coerceIn(0f, MAX_DISMISS_FADE)
                },
    )
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
 * Sur la photo entière, un glissement plutôt horizontal n'est pas consommé : il reste à ce qui
 * entoure la photo, comme le défilement de la corbeille (PS-188).
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
            // Direction choisie au-delà du seuil de glissement : fermeture (vers le bas) ou passage.
            var dismissing: Boolean? = null
            var travel = Offset.Zero
            do {
                val event = awaitPointerEvent()
                val pan = event.calculatePan()
                val factor = event.calculateZoom()
                when {
                    pan == Offset.Zero && factor == 1f -> {
                        Unit
                    }

                    zoom.zoomed || factor != 1f || event.changes.size > 1 -> {
                        zoom.transform(event.calculateCentroid(useCurrent = true), pan, factor)
                        event.changes.forEach { it.consume() }
                    }

                    dismissing == null -> {
                        travel += pan
                        dismissing = dismissDirection(travel, viewConfiguration.touchSlop)
                        if (dismissing == true) event.changes.forEach { it.consume() }
                    }

                    dismissing == true -> {
                        scope.launch { dismiss.snapTo((dismiss.value + pan.y).coerceAtLeast(0f)) }
                        event.changes.forEach { it.consume() }
                    }
                }
            } while (event.changes.any { it.pressed })
            settleDismiss(dismiss, size.height, scope, onClose)
        }
    }

/**
 * Direction d'un glissement de la photo entière après un déplacement total [travel] : vrai s'il
 * est plutôt vertical (fermeture), faux s'il est plutôt horizontal (laissé à ce qui l'entoure),
 * `null` tant qu'il n'a pas dépassé le seuil [touchSlop].
 */
private fun dismissDirection(
    travel: Offset,
    touchSlop: Float,
): Boolean? = if (travel.getDistance() > touchSlop) abs(travel.y) > abs(travel.x) else null

/** Fin du glissement vers le bas : au-delà du seuil de [height], [onClose] ; sinon la photo revient. */
private fun settleDismiss(
    dismiss: Animatable<Float, *>,
    height: Int,
    scope: CoroutineScope,
    onClose: () -> Unit,
) {
    if (dismiss.value > height * DISMISS_FRACTION) {
        onClose()
    } else if (dismiss.value != 0f) {
        scope.launch { dismiss.animateTo(0f) }
    }
}

/**
 * Appui sur une photo de la carte : [onOpen] l'ouvre en grand (PS-117). Sur la carte du dessus
 * d'un groupe de photos proches ([compare], PS-160), le maintien du doigt montre la photo
 * précédente jusqu'au relâchement, sans ouvrir ni décider ; TalkBack propose la même chose par une
 * action, qui bascule entre les deux photos.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun Modifier.openOnTap(
    type: MediaType,
    onOpen: (() -> Unit)?,
    compare: CompareState? = null,
): Modifier {
    if (type != MediaType.IMAGE || (onOpen == null && compare?.available != true)) return this
    val comparing = compare?.takeIf { it.available }
    val compareLabel =
        stringResource(if (comparing?.showing == true) R.string.deck_compare_back else R.string.deck_compare_action)
    return this
        .then(
            if (comparing == null) {
                Modifier
            } else {
                // Relâcher le doigt, ou le glisser assez pour lancer un geste, termine la comparaison.
                Modifier.pointerInput(comparing) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        waitForUpOrCancellation()
                        comparing.showing = false
                    }
                }
            },
        ).combinedClickable(
            interactionSource = null,
            indication = null,
            onClickLabel = stringResource(R.string.photo_viewer_open).takeIf { onOpen != null },
            role = Role.Image,
            onLongClick = comparing?.let { { it.showing = true } },
            onClick = { onOpen?.invoke() },
        ).then(
            if (comparing == null) {
                Modifier
            } else {
                Modifier.semantics {
                    customActions =
                        listOf(
                            CustomAccessibilityAction(compareLabel) {
                                comparing.showing = !comparing.showing
                                true
                            },
                        )
                }
            },
        )
}

/** Part de la hauteur à glisser vers le bas pour fermer la vue. */
private const val DISMISS_FRACTION = 0.2f

/** Fondu maximal de la photo pendant ce glissement. */
private const val MAX_DISMISS_FADE = 0.6f

/** Étiquette de test de la vue en grand. */
internal const val PHOTO_VIEWER_TAG = "photo_viewer"
