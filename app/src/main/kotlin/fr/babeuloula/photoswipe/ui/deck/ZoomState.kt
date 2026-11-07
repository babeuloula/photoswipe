package fr.babeuloula.photoswipe.ui.deck

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

/**
 * Zoom d'une photo en grand (PS-117) : facteur [scale] (1 = photo entière) et décalage [offset]
 * de son centre, en pixels. La photo zoomée ne quitte jamais le cadre : le décalage est borné pour
 * que ses bords restent hors de la vue.
 *
 * Les points sont exprimés depuis le coin haut gauche de la vue de taille [size] ; le zoom se fait
 * autour du centre de la vue (origine de `graphicsLayer`).
 */
class ZoomState {
    /** Facteur de zoom, de 1 à [MAX_SCALE]. */
    var scale: Float by mutableFloatStateOf(1f)
        private set

    /** Décalage du centre de la photo, en pixels. */
    var offset: Offset by mutableStateOf(Offset.Zero)
        private set

    /** Taille de la vue, à donner dès qu'elle est mesurée. */
    var size: Size = Size.Zero

    /** Vrai quand la photo est zoomée : un glissement la déplace au lieu de fermer la vue. */
    val zoomed: Boolean get() = scale > 1f

    /**
     * Pincement ou glissement : zoom de [zoom] autour du point [centroid] (qui reste sous les
     * doigts), puis déplacement de [pan].
     */
    fun transform(
        centroid: Offset,
        pan: Offset,
        zoom: Float,
    ) {
        val newScale = (scale * zoom).coerceIn(1f, MAX_SCALE)
        val fromCenter = centroid - center()
        val newOffset = fromCenter - (fromCenter - offset) * (newScale / scale) + pan
        scale = newScale
        offset = clamp(newOffset)
    }

    /** Double appui : zoom sur le point touché [tap], ou retour à la photo entière si elle était zoomée. */
    fun toggle(tap: Offset) {
        if (zoomed) {
            reset()
        } else {
            val fromCenter = tap - center()
            scale = DOUBLE_TAP_SCALE
            offset = clamp(fromCenter - fromCenter * DOUBLE_TAP_SCALE)
        }
    }

    /** Photo entière, centrée. */
    fun reset() {
        scale = 1f
        offset = Offset.Zero
    }

    private fun center(): Offset = Offset(size.width / 2, size.height / 2)

    /** Décalage borné : la photo zoomée couvre toujours la vue. */
    private fun clamp(value: Offset): Offset {
        val maxX = size.width * (scale - 1) / 2
        val maxY = size.height * (scale - 1) / 2
        return Offset(value.x.coerceIn(-maxX, maxX), value.y.coerceIn(-maxY, maxY))
    }

    companion object {
        /** Zoom maximal. */
        const val MAX_SCALE: Float = 5f

        /** Zoom d'un double appui. */
        const val DOUBLE_TAP_SCALE: Float = 2.5f
    }
}
