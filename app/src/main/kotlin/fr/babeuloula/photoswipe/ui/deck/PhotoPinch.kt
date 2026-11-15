package fr.babeuloula.photoswipe.ui.deck

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalWindowInfo

/**
 * Pincement sur la photo de la carte du dessus (PS-190) : dès qu'il agrandit la photo, [onStart]
 * l'ouvre en grand, et le zoom continue sur [zoom], celui de la vue en grand, tant que les doigts
 * restent posés. [onEnd] est appelé quand ils se lèvent.
 *
 * @property zoom zoom de la vue en grand, que le pincement fait avancer.
 * @property onStart ouvre la photo en grand.
 * @property onEnd doigts levés.
 */
class PhotoPinch(
    val zoom: ZoomState,
    val onStart: () -> Unit,
    val onEnd: () -> Unit,
)

/**
 * Relaie un pincement de la carte à [pinch] (PS-190). Avec deux doigts posés, le geste est
 * consommé : la carte ne glisse pas, aucune décision n'est prise. Au-delà d'un léger agrandissement,
 * la photo s'ouvre en grand et chaque mouvement des doigts, ramené aux coordonnées de la fenêtre
 * (celles de la vue en grand, plein écran), zoome et déplace sa photo, jusqu'au lever des doigts :
 * la fenêtre qui a reçu le premier doigt garde le geste, même sous la vue en grand.
 */
@Composable
internal fun Modifier.pinchToOpen(pinch: PhotoPinch?): Modifier {
    if (pinch == null) return this
    val coordinates = remember { arrayOfNulls<LayoutCoordinates>(1) }
    val window = LocalWindowInfo.current.containerSize
    return this
        .onGloballyPositioned { coordinates[0] = it }
        .pointerInput(pinch, window) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                var pinching = false
                var growth = 1f
                do {
                    val event = awaitPointerEvent()
                    val pressed = event.changes.count { it.pressed }
                    // Doigts levés : plus de centre entre eux, rien à zoomer.
                    if (pressed > 0 && (pinching || pressed > 1)) {
                        val factor = event.calculateZoom()
                        val centroid =
                            event.calculateCentroid(useCurrent = true).let { local ->
                                coordinates[0]?.takeIf { it.isAttached }?.localToWindow(local) ?: local
                            }
                        if (pinching) {
                            pinch.zoom.transform(centroid, event.calculatePan(), factor)
                        } else {
                            growth *= factor
                            if (growth > PINCH_OPEN_SCALE) {
                                pinching = true
                                pinch.zoom.reset()
                                pinch.zoom.size = Size(window.width.toFloat(), window.height.toFloat())
                                pinch.onStart()
                                pinch.zoom.transform(centroid, Offset.Zero, growth)
                            }
                        }
                        event.changes.forEach { it.consume() }
                    }
                } while (event.changes.any { it.pressed })
                if (pinching) pinch.onEnd()
            }
        }
}

/** Agrandissement du pincement à partir duquel la photo s'ouvre en grand. */
private const val PINCH_OPEN_SCALE = 1.05f
