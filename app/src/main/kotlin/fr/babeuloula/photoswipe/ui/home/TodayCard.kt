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
import fr.babeuloula.photoswipe.ui.common.SharedScreenKey
import fr.babeuloula.photoswipe.ui.common.Thumbnail
import fr.babeuloula.photoswipe.ui.common.currentWindowLayout
import fr.babeuloula.photoswipe.ui.common.sharedScreen
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
 * en surface, une coche le dit, « Voir le résumé » mène au résumé, et « Retrier » ou un appui sur
 * la carte rouvre le jour (ADR 0026, PS-111). Sans souvenir ce jour-là, ou de
 * toute la semaine, elle le dit en texte.
 */
@Composable
internal fun TodayCard(
    state: HomeUiState.Loaded,
    onOpenDay: () -> Unit,
    onEvent: (HomeEvent) -> Unit,
    compact: Boolean = false,
) {
    val today = state.today
    when {
        !state.hasMemories -> {
            NoMemoriesCard(state)
        }

        // Vidé par ses suppressions : la carte dit ce qui a été fait, et mène au résumé (ADR 0034).
        today.isEmptied -> {
            TextCard(
                title = stringResource(R.string.home_today_done),
                body = null,
                date = today.date,
                day = today,
                onSummary = { onEvent(HomeEvent.OpenPendingDeletions) },
            )
        }

        today.memoryCount == 0 -> {
            TextCard(
                title = stringResource(R.string.home_today_no_memories),
                body = stringResource(R.string.home_today_no_memories_body, dayAndMonth(today.date)),
                date = today.date,
            )
        }

        else -> {
            MemoriesCard(today, onOpenDay = onOpenDay, onEvent = onEvent, compact = compact)
        }
    }
}

/** Aucun souvenir sur toute la fenêtre de rattrapage : galerie récente, ou jours sans photo les années passées. */
@Composable
private fun NoMemoriesCard(state: HomeUiState.Loaded) {
    val oldest = state.previousDays.lastOrNull()?.date ?: state.today.date
    TextCard(
        title = stringResource(R.string.home_empty_title),
        body = stringResource(R.string.home_empty_body, dayAndMonth(oldest), dayAndMonth(state.today.date)),
        date = state.today.date,
    )
}

/**
 * Carte de texte du jour, sans souvenir à montrer ; pour un jour vidé par ses suppressions ([day]),
 * ce qui a été supprimé et « Voir le résumé » ([onSummary]).
 */
@Composable
private fun TextCard(
    title: String,
    body: String?,
    date: LocalDate,
    day: ReviewDay? = null,
    onSummary: (() -> Unit)? = null,
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
        body?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        day?.let { DeletedLabel(it) }
        onSummary?.let {
            FilledTonalButton(onClick = it, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.home_view_summary))
            }
        }
    }
}

/**
 * Carte du jour avec ses souvenirs : à trier sur `primaryContainer`, tout trié en surface. En
 * deux volets ([compact]), la bande de vignettes et les boutons passent dans le volet de détail.
 */
@Composable
private fun MemoriesCard(
    today: ReviewDay,
    onOpenDay: () -> Unit,
    onEvent: (HomeEvent) -> Unit,
    compact: Boolean,
) {
    val onResort = { onEvent(HomeEvent.ResortDay(today.date)) }
    val toSort = today.remainingCount > 0
    val scheme = MaterialTheme.colorScheme
    val container = if (toSort) scheme.primaryContainer else scheme.surfaceContainerHigh
    val content = if (toSort) scheme.onPrimaryContainer else scheme.onSurface
    // Tout trié : toucher la carte, photos comprises, retrie le jour comme « Retrier » (PS-111).
    val resortLabel = stringResource(R.string.home_resort)
    Card(
        onClick = if (toSort) onOpenDay else onResort,
        shape = MaterialTheme.shapes.extraLarge,
        colors =
            CardDefaults.cardColors(
                containerColor = container,
                contentColor = content,
            ),
        modifier =
            Modifier
                // La carte s'agrandit jusqu'à devenir la pile du jour (PS-141).
                .sharedScreen(SharedScreenKey.Deck(today.date), MaterialTheme.shapes.extraLarge)
                .fillMaxWidth()
                .semantics {
                    if (!toSort) {
                        onClick(label = resortLabel) {
                            onResort()
                            true
                        }
                    }
                },
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CardHeader(today, compact)
            if (!compact) {
                // Toucher les vignettes montre tous les souvenirs du jour, avant ou après le tri (PS-139).
                PreviewStrip(
                    today = today,
                    tileColor = content,
                    onTileColor = container,
                    modifier =
                        Modifier
                            // Les vignettes s'agrandissent jusqu'à la vue d'ensemble du jour (PS-141).
                            .sharedScreen(SharedScreenKey.Overview(today.date), MaterialTheme.shapes.small)
                            .alpha(if (toSort) 1f else SORTED_ALPHA)
                            .clip(MaterialTheme.shapes.small)
                            .clickable(onClickLabel = stringResource(R.string.day_overview_open)) {
                                onEvent(HomeEvent.OpenOverview(today.date))
                            }.testTag(PREVIEW_STRIP_TAG),
                )
            }
            when {
                toSort -> TodayCount(today.remainingCount, onStart = onOpenDay.takeUnless { compact })
                compact -> Text(stringResource(R.string.home_today_done), style = MaterialTheme.typography.bodyLarge)
                else -> AllSorted({ onEvent(HomeEvent.OpenPendingDeletions) }, onResort)
            }
            if (today.deletedCount > 0) DeletedLabel(today)
        }
    }
}

/** « Aujourd'hui », la date, « Ce jour-là, toutes les années passées » et, hors volets, le repère des années. */
@Composable
private fun CardHeader(
    today: ReviewDay,
    compact: Boolean,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.home_today),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.semantics { heading() },
            )
            Text(dayLabel(today.date), style = MaterialTheme.typography.headlineSmall)
            // Les souvenirs ne viennent pas que de l'an dernier (PS-138).
            Text(stringResource(R.string.home_today_every_year), style = MaterialTheme.typography.bodyMedium)
        }
        if (!compact) today.years?.let { FilmEdgeLabel(yearsLabel(it)) }
    }
}

/** « 23 médias à trier », le nombre en grand et en chasse fixe, et « Commencer le tri ». */
@Composable
private fun TodayCount(
    count: Int,
    onStart: (() -> Unit)?,
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
        if (onStart != null) Button(onClick = onStart) { Text(stringResource(R.string.home_start_sorting)) }
    }
}

/** Tout est trié aujourd'hui : coche verte, « Retrier » (ADR 0026) et « Voir le résumé ». */
@Composable
private fun AllSorted(
    onSummary: () -> Unit,
    onResort: () -> Unit,
) {
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onResort) { Text(stringResource(R.string.home_resort)) }
            FilledTonalButton(onClick = onSummary) { Text(stringResource(R.string.home_view_summary)) }
        }
    }
}

/** Années des souvenirs du jour : « De 2016 à 2025 », ou « En 2025 » pour une seule. */
@Composable
internal fun yearsLabel(years: IntRange): String =
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

/** Opacité des vignettes d'un jour déjà trié : elles restent reconnaissables, mais en retrait. */
private const val SORTED_ALPHA = 0.72f

/** Repère de test de la bande de vignettes de la carte du jour. */
internal const val PREVIEW_STRIP_TAG = "today_preview_strip"
