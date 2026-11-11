package fr.babeuloula.photoswipe.ui.deck

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.ui.theme.monoLabel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Progression dans la pile du jour, sous la barre du haut : une barre et « 4 sur 23 » en chiffres
 * tabulaires. TalkBack la lit comme une barre de progression. Avec plusieurs années ([segments]),
 * la barre est coupée en un tronçon par année, à la taille de son nombre de souvenirs (PS-144) :
 * on voit combien d'années il reste à trier.
 */
@Composable
internal fun DeckProgress(
    position: Int,
    total: Int,
    yearProgress: YearProgress? = null,
    segments: List<YearSegment> = emptyList(),
) {
    val progressLabel = stringResource(R.string.deck_progress, position, total)
    val label =
        if (segments.size > 1) {
            stringResource(
                R.string.deck_progress_with_years,
                progressLabel,
                pluralStringResource(R.plurals.deck_progress_years, segments.size, segments.size),
            )
        } else {
            progressLabel
        }
    Column {
        ProgressRow(position, total, progressLabel, label, segments)
        yearProgress?.let { YearLine(it) }
    }
}

/**
 * Tronçon d'une année dans la barre de progression de la pile (PS-144).
 *
 * @property year année de prise de vue.
 * @property sorted souvenirs de cette année déjà triés pendant la session.
 * @property total souvenirs de cette année dans la pile, triés ou non.
 */
data class YearSegment(
    val year: Int,
    val sorted: Int,
    val total: Int,
)

/**
 * Où en est la pile dans l'année de la carte du dessus (PS-138).
 *
 * @property year année de prise de vue.
 * @property position rang de la carte parmi les souvenirs de cette année, à partir de 1.
 * @property total souvenirs de cette année dans la pile.
 */
internal data class YearProgress(
    val year: Int,
    val position: Int,
    val total: Int,
)

/**
 * « 2023 · souvenir 2 sur 5 », ou « 2025 · seul souvenir de cette année » : la pile va d'une année à l'autre, de la plus récente à la plus
 * ancienne ; la ligne glisse à chaque changement d'année pour le rendre visible.
 */
@Composable
private fun YearLine(progress: YearProgress) {
    AnimatedContent(
        targetState = progress,
        contentKey = { it.year },
        transitionSpec = {
            (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
        },
        label = "deckYear",
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
    ) { shown ->
        Text(
            text =
                if (shown.total == 1) {
                    stringResource(R.string.deck_year_single, shown.year)
                } else {
                    stringResource(R.string.deck_year_progress, shown.year, shown.position, shown.total)
                },
            style = MaterialTheme.typography.monoLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Barre de progression et « 4 sur 23 ». */
@Composable
private fun ProgressRow(
    position: Int,
    total: Int,
    shown: String,
    label: String,
    segments: List<YearSegment>,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val barModifier = Modifier.weight(1f).semantics { contentDescription = label }
        if (segments.size > 1) {
            Row(
                modifier = barModifier.testTag(YEAR_SEGMENTS_TAG),
                horizontalArrangement = Arrangement.spacedBy(SEGMENT_GAP),
            ) {
                segments.forEach { segment ->
                    LinearProgressIndicator(
                        progress = { segment.sorted.toFloat() / segment.total },
                        modifier = Modifier.weight(segment.total.toFloat()),
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                    )
                }
            }
        } else {
            LinearProgressIndicator(
                progress = { if (total == 0) 0f else (position - 1).toFloat() / total },
                modifier = barModifier,
            )
        }
        Text(
            text = shown,
            style = MaterialTheme.typography.monoLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

/** Jour et mois de [date] dans la langue de [context], sans l'année : « 6 novembre », « November 6 ». */
internal fun dayAndMonth(
    context: Context,
    date: LocalDate,
): String {
    val locale = context.resources.configuration.locales[0]
    return date.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "dMMMM"), locale))
}

/** Repère de test de la barre coupée par année. */
internal const val YEAR_SEGMENTS_TAG = "deck_year_segments"

/** Espace entre les tronçons de deux années. */
private val SEGMENT_GAP = 4.dp
