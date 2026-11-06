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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
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
import fr.babeuloula.photoswipe.ui.common.segmentShape
import fr.babeuloula.photoswipe.ui.theme.IbmPlexMono
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import fr.babeuloula.photoswipe.ui.theme.monoLabel
import fr.babeuloula.photoswipe.ui.theme.swipeColors
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Les jours à rattraper, en liste segmentée : coins larges en haut et en bas, serrés entre deux jours. */
internal fun LazyListScope.previousDays(
    days: List<ReviewDay>,
    onOpenDay: (LocalDate) -> Unit,
    onResortDay: (LocalDate) -> Unit,
) {
    itemsIndexed(days, key = { _, day -> day.date.toEpochDay() }) { index, day ->
        PreviousDay(
            day,
            shape = segmentShape(index, days.size),
            onClick = { if (day.remainingCount > 0) onOpenDay(day.date) else onResortDay(day.date) },
        )
    }
}

/** Titre « À rattraper », annoncé comme tel par TalkBack. */
@Composable
internal fun PreviousDaysTitle() {
    Text(
        text = stringResource(R.string.home_previous_days),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 10.dp).semantics { heading() },
    )
}

/**
 * Un jour à rattraper : vignette, date et état (« 8 médias à trier », « Tout est trié »,
 * « Aucun souvenir »), chevron s'il reste à trier. Un jour tout trié s'ouvre pour être retrié
 * (ADR 0026) ; un jour sans souvenir est grisé et inactif.
 */
@Composable
private fun PreviousDay(
    day: ReviewDay,
    shape: Shape,
    onClick: () -> Unit,
) {
    val hasMedia = day.remainingCount > 0
    val hasMemories = day.memoryCount > 0
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable(
                    enabled = hasMemories,
                    onClickLabel = if (hasMedia) null else stringResource(R.string.home_resort),
                    onClick = onClick,
                ).padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val alpha = if (hasMemories) 1f else DISABLED_ALPHA
        DayThumbnail(day.previews.firstOrNull(), modifier = Modifier.alpha(alpha))
        Column(modifier = Modifier.weight(1f).alpha(alpha)) {
            Text(dayLabel(day.date), style = MaterialTheme.typography.bodyLarge)
            Text(
                text =
                    when {
                        hasMedia -> {
                            pluralStringResource(
                                R.plurals.home_remaining,
                                day.remainingCount,
                                day.remainingCount,
                            )
                        }

                        hasMemories -> {
                            stringResource(R.string.home_day_done)
                        }

                        else -> {
                            stringResource(R.string.home_day_no_memories)
                        }
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when {
            hasMedia -> TrailingIcon(R.drawable.ic_chevron_right)
            hasMemories -> TrailingIcon(R.drawable.ic_check)
        }
    }
}

/** Icône de fin de ligne, décorative. */
@Composable
private fun TrailingIcon(
    @DrawableRes icon: Int,
) {
    Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Vignette de 48 dp d'un jour, ou une image barrée s'il n'a aucun souvenir. */
@Composable
private fun DayThumbnail(
    media: MediaItem?,
    modifier: Modifier = Modifier,
) {
    val thumbnailModifier = modifier.size(48.dp).clip(MaterialTheme.shapes.small)
    if (media == null) {
        Box(
            modifier = thumbnailModifier.background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_hide_image), contentDescription = null)
        }
    } else {
        Thumbnail(media, thumbnailModifier)
    }
}

/** Opacité d'un jour sans média, celle des contenus désactivés de Material 3. */
internal const val DISABLED_ALPHA = 0.38f
