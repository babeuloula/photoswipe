package fr.babeuloula.photoswipe.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import fr.babeuloula.photoswipe.domain.model.MediaItem

/** Vignette d'un média, recadrée, chargée par Coil à sa taille ; décorative. */
@Composable
fun Thumbnail(
    media: MediaItem,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = media.uri,
        contentDescription = null,
        imageLoader = LocalImageLoader.current,
        placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHighest),
        error = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}
