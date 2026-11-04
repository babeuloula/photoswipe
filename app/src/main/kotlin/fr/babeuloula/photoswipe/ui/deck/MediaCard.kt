package fr.babeuloula.photoswipe.ui.deck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaType
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import java.time.LocalDate

/**
 * Carte plein écran d'un souvenir (SPEC §4.3) : l'image, chargée par Coil à la taille de la carte,
 * et en bas l'étiquette « Il y a 3 ans · 14 mai 2023 » avec le poids du fichier. Sans état : elle
 * affiche [card] et rien d'autre.
 *
 * Si l'image ne s'ouvre pas (fichier disparu, données de démo sans fichier, vidéo dont la
 * miniature n'est pas encore gérée), une icône et un message remplacent l'image.
 */
@Composable
fun MediaCard(
    card: MediaCardUiState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val date = localizedDate(context, card.takenOn)
    var failed by remember(card.uri) { mutableStateOf(false) }
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = card.uri,
                contentDescription =
                    stringResource(
                        if (card.type ==
                            MediaType.VIDEO
                        ) {
                            R.string.deck_video_description
                        } else {
                            R.string.deck_photo_description
                        },
                        date,
                    ),
                imageLoader = LocalImageLoader.current,
                placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentScale = ContentScale.Fit,
                onSuccess = { failed = false },
                onError = { failed = true },
                modifier = Modifier.fillMaxSize(),
            )
            if (failed) {
                Unavailable(type = card.type, modifier = Modifier.align(Alignment.Center))
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = LABEL_ALPHA),
                shape = MaterialTheme.shapes.medium,
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(12.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = memoryLabel(context, card.yearsAgo, card.takenOn),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = fileSizeLabel(context, card.sizeBytes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
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
            imageVector = if (type == MediaType.VIDEO) Icons.Filled.PlayArrow else Icons.Filled.Warning,
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

/** Opacité du fond de l'étiquette : lisible sur n'importe quelle photo sans la masquer. */
private const val LABEL_ALPHA = 0.85f

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
        PhotoSwipeTheme(dynamicColor = false) {
            MediaCard(card = PREVIEW_CARD, modifier = Modifier.fillMaxSize())
        }
    }
}
