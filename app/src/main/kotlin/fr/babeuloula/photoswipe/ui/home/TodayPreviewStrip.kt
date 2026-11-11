package fr.babeuloula.photoswipe.ui.home

import android.text.format.DateFormat
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.ReviewDay
import fr.babeuloula.photoswipe.ui.common.CenteredContent
import fr.babeuloula.photoswipe.ui.common.FilmEdgeLabel
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader
import fr.babeuloula.photoswipe.ui.common.Thumbnail
import fr.babeuloula.photoswipe.ui.common.currentWindowLayout
import fr.babeuloula.photoswipe.ui.theme.IbmPlexMono
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import fr.babeuloula.photoswipe.ui.theme.monoLabel
import fr.babeuloula.photoswipe.ui.theme.swipeColors
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Bande de vignettes du jour en [ReviewDay.PREVIEW_SLOTS] cases, l'année sous chaque vignette en
 * chasse fixe ; s'il reste des souvenirs hors de la bande, la dernière case en donne le nombre (« +20 »).
 */
@Composable
internal fun PreviewStrip(
    today: ReviewDay,
    tileColor: Color,
    onTileColor: Color,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        today.previews.forEach { media ->
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Thumbnail(
                    media,
                    Modifier
                        .fillMaxWidth()
                        .height(PREVIEW_HEIGHT)
                        .clip(MaterialTheme.shapes.small),
                )
                Text(
                    text =
                        media.takenAt
                            .atZone(zone)
                            .year
                            .toString(),
                    style = MaterialTheme.typography.monoLabel,
                )
            }
        }
        val moreTile = today.hiddenCount > 0
        val emptySlots = ReviewDay.PREVIEW_SLOTS - today.previews.size - if (moreTile) 1 else 0
        repeat(emptySlots.coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
        if (moreTile) {
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(PREVIEW_HEIGHT)
                        .clip(MaterialTheme.shapes.small)
                        .background(tileColor),
                contentAlignment = Alignment.Center,
            ) {
                val description = stringResource(R.string.home_more_memories_description, today.hiddenCount)
                Text(
                    text = stringResource(R.string.home_more_memories, today.hiddenCount),
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = IbmPlexMono),
                    color = onTileColor,
                    modifier = Modifier.clearAndSetSemantics { contentDescription = description },
                )
            }
        }
    }
}

/** Hauteur des vignettes de la carte du jour. */
private val PREVIEW_HEIGHT = 104.dp
