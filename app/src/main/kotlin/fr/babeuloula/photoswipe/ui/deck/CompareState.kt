package fr.babeuloula.photoswipe.ui.deck

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader

/**
 * Comparaison avec la photo précédente d'un groupe de photos proches (PS-160) : tant que le doigt
 * reste posé sur la carte du dessus, ou après l'action TalkBack « Comparer », la photo prise juste
 * avant la remplace.
 *
 * @property previousUri adresse de la photo précédente ; `null` hors groupe ou pour la première
 * photo du groupe : il n'y a rien à comparer.
 */
internal class CompareState(
    val previousUri: String?,
) {
    /** Vrai pendant la comparaison. */
    var showing by mutableStateOf(false)

    /** Vrai s'il y a une photo précédente à montrer. */
    val available: Boolean get() = previousUri != null
}

/** État de comparaison de [card], remis à zéro quand la carte change. */
@Composable
internal fun rememberCompare(card: MediaCardUiState): CompareState =
    remember(card.id, card.similar?.previousUri) { CompareState(card.similar?.previousUri) }

/**
 * Photo précédente du groupe, par-dessus la carte, avec un repère « Photo précédente · 1 sur 3 »
 * que TalkBack annonce.
 */
@Composable
internal fun BoxScope.ComparedPhoto(card: MediaCardUiState) {
    val similar = card.similar ?: return
    val uri = similar.previousUri ?: return
    AsyncImage(
        model = uri,
        contentDescription = null,
        imageLoader = LocalImageLoader.current,
        placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize().testTag(COMPARED_PHOTO_TAG),
    )
    val label = stringResource(R.string.deck_compare_previous, similar.position - 1, similar.size)
    Box(
        modifier =
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onPrimaryFixed)
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .clearAndSetSemantics {
                    contentDescription = label
                    liveRegion = LiveRegionMode.Polite
                },
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primaryFixed)
    }
}

/** Étiquette de test de la photo précédente affichée. */
internal const val COMPARED_PHOTO_TAG = "compared_photo"
