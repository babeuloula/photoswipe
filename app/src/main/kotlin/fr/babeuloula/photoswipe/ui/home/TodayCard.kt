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
 * Carte « Aujourd'hui » (ADR 0025) : la date, le repère des années, une bande de vignettes rangées
 * par année et le nombre de médias à trier, avec « Commencer le tri ». Tout trié : la carte passe
 * en surface, une coche le dit et « Voir le résumé » mène au résumé. Sans souvenir ce jour-là, ou de
 * toute la semaine, elle le dit en texte.
 */
@Composable
internal fun TodayCard(
    state: HomeUiState.Loaded,
    onOpenDay: () -> Unit,
    onEvent: (HomeEvent) -> Unit,
) {
    val today = state.today
    when {
        !state.hasMemories -> {
            NoMemoriesCard(state)
        }

        today.memoryCount == 0 -> {
            TextCard(
                title = stringResource(R.string.home_today_no_memories),
                body = stringResource(R.string.home_today_no_memories_body, dayAndMonth(today.date)),
                date = today.date,
            )
        }

        else -> {
            MemoriesCard(today, onOpenDay = onOpenDay, onSummary = { onEvent(HomeEvent.OpenPendingDeletions) })
        }
    }
}

/** Aucun souvenir sur les 7 jours de la fenêtre : galerie récente, ou jours sans photo les années passées. */
@Composable
private fun NoMemoriesCard(state: HomeUiState.Loaded) {
    val oldest = state.previousDays.lastOrNull()?.date ?: state.today.date
    TextCard(
        title = stringResource(R.string.home_empty_title),
        body = stringResource(R.string.home_empty_body, dayAndMonth(oldest), dayAndMonth(state.today.date)),
        date = state.today.date,
    )
}

/** Carte de texte du jour, sans souvenir à montrer. */
@Composable
private fun TextCard(
    title: String,
    body: String,
    date: LocalDate,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.home_today_with_date, dayLabel(date)),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Carte du jour avec ses souvenirs : à trier sur `primaryContainer`, tout trié en surface. */
@Composable
private fun MemoriesCard(
    today: ReviewDay,
    onOpenDay: () -> Unit,
    onSummary: () -> Unit,
) {
    val toSort = today.remainingCount > 0
    val scheme = MaterialTheme.colorScheme
    val container = if (toSort) scheme.primaryContainer else scheme.surfaceContainerHigh
    val content = if (toSort) scheme.onPrimaryContainer else scheme.onSurface
    Card(
        onClick = onOpenDay,
        enabled = toSort,
        shape = MaterialTheme.shapes.extraLarge,
        colors =
            CardDefaults.cardColors(
                containerColor = container,
                contentColor = content,
                disabledContainerColor = container,
                disabledContentColor = content,
            ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_today),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(dayLabel(today.date), style = MaterialTheme.typography.headlineSmall)
                }
                today.years?.let { FilmEdgeLabel(yearsLabel(it)) }
            }
            PreviewStrip(
                today = today,
                tileColor = content,
                onTileColor = container,
                modifier = Modifier.alpha(if (toSort) 1f else SORTED_ALPHA),
            )
            if (toSort) {
                TodayCount(today.remainingCount, onStart = onOpenDay)
            } else {
                AllSorted(onSummary)
            }
        }
    }
}

/**
 * Bande de vignettes du jour, une par année, avec l'année dessous en chasse fixe ; la dernière case
 * donne le nombre des autres souvenirs (« +20 »).
 */
@Composable
private fun PreviewStrip(
    today: ReviewDay,
    tileColor: Color,
    onTileColor: Color,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        today.previews.forEach { media ->
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Thumbnail(media, Modifier.fillMaxWidth().height(PREVIEW_HEIGHT).clip(MaterialTheme.shapes.small))
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
        repeat(ReviewDay.PREVIEW_COUNT - today.previews.size) { Spacer(Modifier.weight(1f)) }
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .height(PREVIEW_HEIGHT)
                    .clip(MaterialTheme.shapes.small)
                    .background(if (today.hiddenCount > 0) tileColor else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            if (today.hiddenCount > 0) {
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

/** « 23 médias à trier », le nombre en grand et en chasse fixe, et « Commencer le tri ». */
@Composable
private fun TodayCount(
    count: Int,
    onStart: () -> Unit,
) {
    val text = pluralStringResource(R.plurals.home_remaining, count, count)
    val number = count.toString()
    val numberStyle = MaterialTheme.typography.displaySmall.toSpanStyle()
    val annotated =
        buildAnnotatedString {
            val start = text.indexOf(number)
            if (start < 0) {
                append(text)
            } else {
                append(text.substring(0, start))
                withStyle(numberStyle) { append(number) }
                append(text.substring(start + number.length))
            }
        }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(annotated, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Button(onClick = onStart) { Text(stringResource(R.string.home_start_sorting)) }
    }
}

/** Tout est trié aujourd'hui : coche verte, et « Voir le résumé ». */
@Composable
private fun AllSorted(onSummary: () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painterResource(R.drawable.ic_check_circle_filled),
                contentDescription = null,
                tint = MaterialTheme.swipeColors.keep,
            )
            Text(stringResource(R.string.home_today_done), style = MaterialTheme.typography.bodyLarge)
        }
        FilledTonalButton(onClick = onSummary) { Text(stringResource(R.string.home_view_summary)) }
    }
}

/** Années des souvenirs du jour : « De 2016 à 2025 », ou « En 2025 » pour une seule. */
@Composable
private fun yearsLabel(years: IntRange): String =
    if (years.first == years.last) {
        stringResource(R.string.home_year_single, years.first)
    } else {
        stringResource(R.string.home_years_range, years.first, years.last)
    }

/** Jour de la semaine, jour et mois, sans l'année : « Vendredi 6 novembre ». */
@Composable
internal fun dayLabel(date: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    val pattern = DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM")
    val text = date.format(DateTimeFormatter.ofPattern(pattern, locale))
    return text.replaceFirstChar { it.titlecase(locale) }
}

/** Jour et mois, sans l'année : « 6 novembre ». */
@Composable
private fun dayAndMonth(date: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    return date.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "dMMMM"), locale))
}

/** Hauteur des vignettes de la carte du jour. */
private val PREVIEW_HEIGHT = 104.dp

/** Opacité des vignettes d'un jour déjà trié : elles restent reconnaissables, mais en retrait. */
private const val SORTED_ALPHA = 0.72f
