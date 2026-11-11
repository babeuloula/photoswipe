package fr.babeuloula.photoswipe.ui.deck

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import kotlin.math.abs

/**
 * Taille de la carte d'un média dans l'espace [maxWidth] × [maxHeight] de la pile (PS-142) : aux
 * proportions de l'image ([aspectRatio], largeur sur hauteur), la plus grande qui y tienne, pour
 * qu'aucune bande grise n'entoure la photo. Une photo en paysage donne une carte large et basse,
 * centrée dans la pile.
 *
 * La carte garde au moins [MIN_CARD_WIDTH] de large et [MIN_CARD_HEIGHT] de haut (sans dépasser
 * l'espace) pour que le repère, les pastilles et les boutons de la vidéo y tiennent ; une image
 * plus étroite ou plus basse (panorama très allongé) la remplit alors en étant rognée
 * ([fillsByCropping]), entière une fois ouverte en grand. Sans proportions connues
 * (image pas encore chargée, illisible), la carte prend tout l'espace.
 */
internal fun fittedCardSize(
    maxWidth: Dp,
    maxHeight: Dp,
    aspectRatio: Float?,
): DpSize {
    val ratio = aspectRatio?.takeIf { it > 0f && maxWidth > 0.dp && maxHeight > 0.dp }
    if (ratio == null) return DpSize(maxWidth, maxHeight)
    var width = maxWidth
    var height = maxWidth / ratio
    if (height > maxHeight) {
        height = maxHeight
        width = maxHeight * ratio
    }
    return DpSize(
        width = max(width, min(MIN_CARD_WIDTH, maxWidth)),
        height = max(height, min(MIN_CARD_HEIGHT, maxHeight)),
    )
}

/**
 * Place la carte du média [uri] dans tout l'espace donné, à la forme de son image (PS-142) :
 * [content] reçoit la taille de la carte, s'il faut rogner l'image ([fillsByCropping]) et de quoi
 * retenir les proportions de l'image une fois chargée.
 */
@Composable
internal fun FittedCardBox(
    uri: String,
    modifier: Modifier = Modifier,
    content: @Composable (size: DpSize, crop: Boolean, onRatio: (Float) -> Unit) -> Unit,
) {
    val ownRatio = remember(uri) { mutableStateOf<Float?>(null) }
    val ratios = LocalCardAspectRatios.current
    val ratio = ratios?.get(uri) ?: ownRatio.value
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val size = fittedCardSize(maxWidth, maxHeight, ratio)
        content(size, fillsByCropping(size, ratio)) { loaded ->
            if (ratios != null) ratios[uri] = loaded else ownRatio.value = loaded
        }
    }
}

/**
 * Proportions des images déjà chargées, par URI, tenues par la pile : la carte du dessus garde la
 * forme qu'avait la carte suivante, préparée dessous, sans repasser par toute la hauteur (PS-142).
 * `null` hors d'une pile : chaque carte retient alors les siennes.
 */
internal val LocalCardAspectRatios = staticCompositionLocalOf<SnapshotStateMap<String, Float>?> { null }

/** Largeur minimale d'une carte : le repère « Il y a 3 ans · 14 mai 2023 » y tient. */
internal val MIN_CARD_WIDTH = 280.dp

/** Hauteur minimale d'une carte : le repère en haut et les pastilles en bas y tiennent, un 2:1 aussi. */
internal val MIN_CARD_HEIGHT = 180.dp

/**
 * Vrai si l'image, aux proportions [aspectRatio], ne remplit pas la carte de taille [size] : elle y
 * est rognée plutôt que bordée de gris (PS-142).
 */
internal fun fillsByCropping(
    size: DpSize,
    aspectRatio: Float?,
): Boolean {
    if (aspectRatio == null || aspectRatio <= 0f || size.height <= 0.dp) return false
    val cardRatio = size.width / size.height
    return abs(cardRatio - aspectRatio) / aspectRatio > RATIO_TOLERANCE
}

/** Écart de proportions toléré avant de rogner : un arrondi au pixel ne compte pas. */
private const val RATIO_TOLERANCE = 0.01f
