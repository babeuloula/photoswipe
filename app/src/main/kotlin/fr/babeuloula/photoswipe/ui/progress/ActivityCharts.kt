package fr.babeuloula.photoswipe.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.ActivityReport
import fr.babeuloula.photoswipe.domain.model.MonthlyActivity
import fr.babeuloula.photoswipe.ui.common.segmentShape
import fr.babeuloula.photoswipe.ui.deck.fileSizeLabel
import fr.babeuloula.photoswipe.ui.settings.PreferenceSection
import fr.babeuloula.photoswipe.ui.theme.swipeColors
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/**
 * Section « Les 6 derniers mois » de la page « Progression » (PS-154) : médias triés par mois
 * (gardés et supprimés empilés) et espace libéré par mois, en barres dessinées aux couleurs du
 * thème. Chaque graphique est lu d'un bloc par TalkBack, avec ses chiffres. Sans aucun tri sur la
 * période, un texte l'explique ; l'historique ne commence qu'à la version qui l'a introduit, et
 * la section dit depuis quand.
 */
@Composable
internal fun ActivitySection(report: ActivityReport) {
    PreferenceSection(stringResource(R.string.progress_activity_section, report.months.size)) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(segmentShape(0, 1))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (report.isEmpty) {
                Text(stringResource(R.string.progress_activity_empty), style = MaterialTheme.typography.bodyLarge)
            } else {
                SortedChart(report)
                FreedChart(report)
            }
            report.since?.let { since ->
                val locale = LocalConfiguration.current.locales[0]
                val date = since.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
                Text(
                    stringResource(R.string.progress_activity_since, date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Médias triés par mois : gardés et supprimés empilés, leur légende et leur part sur la période. */
@Composable
private fun SortedChart(report: ActivityReport) {
    val locale = LocalConfiguration.current.locales[0]
    val kept = MaterialTheme.swipeColors.keep
    val trashed = MaterialTheme.swipeColors.delete
    val months =
        report.months.map { month ->
            stringResource(
                R.string.progress_activity_sorted_month,
                month.fullName(locale),
                month.keptCount,
                month.trashedCount,
            )
        }
    Text(stringResource(R.string.progress_activity_sorted), style = MaterialTheme.typography.titleMedium)
    BarChart(
        bars =
            report.months.map { month ->
                Bar(
                    month.shortName(locale),
                    listOf(
                        month.keptCount.toFloat() to kept,
                        month.trashedCount.toFloat() to trashed,
                    ),
                )
            },
        description = stringResource(R.string.progress_activity_sorted_description, months.joinToString(" ")),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Legend(kept, stringResource(R.string.progress_activity_kept))
        Legend(trashed, stringResource(R.string.progress_activity_trashed))
    }
    report.keptPercent?.let { percent ->
        Text(
            stringResource(R.string.progress_activity_ratio, percent, PERCENT - percent),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Espace libéré par mois, et son total sur la période. */
@Composable
private fun FreedChart(report: ActivityReport) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val color = MaterialTheme.colorScheme.primary
    val months =
        report.months.map { month ->
            stringResource(
                R.string.progress_activity_freed_month,
                month.fullName(locale),
                fileSizeLabel(context, month.freedBytes),
            )
        }
    Text(
        stringResource(R.string.progress_activity_freed),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 8.dp),
    )
    BarChart(
        bars = report.months.map { month -> Bar(month.shortName(locale), listOf(month.freedBytes.toFloat() to color)) },
        description = stringResource(R.string.progress_activity_freed_description, months.joinToString(" ")),
    )
    Text(
        stringResource(
            R.string.progress_activity_freed_total,
            fileSizeLabel(context, report.months.sumOf { it.freedBytes }),
            report.months.size,
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Barre d'un graphique.
 *
 * @property label libellé sous la barre.
 * @property parts valeurs empilées de bas en haut, chacune avec sa couleur.
 */
private class Bar(
    val label: String,
    val parts: List<Pair<Float, Color>>,
)

/**
 * Graphique en barres, à l'échelle de la plus haute. Les barres sont dessinées, leurs libellés sont
 * du texte (ils suivent la taille de police) ; l'ensemble est une image décrite par [description].
 */
@Composable
private fun BarChart(
    bars: List<Bar>,
    description: String,
) {
    val highest = bars.maxOf { bar -> bar.parts.sumOf { it.first.toDouble() } }.toFloat()
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clearAndSetSemantics {
                    contentDescription = description
                    role = Role.Image
                },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        bars.forEach { bar ->
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(CHART_HEIGHT)
                        .drawBehind {
                            val radius = CornerRadius(BAR_RADIUS.toPx())
                            // Ligne de base : un mois sans tri garde sa place.
                            drawRoundRect(
                                track,
                                Offset(0f, size.height - BASELINE.toPx()),
                                Size(size.width, BASELINE.toPx()),
                            )
                            var top = size.height
                            bar.parts.filter { it.first > 0f }.forEach { (value, color) ->
                                val height = (value / highest * size.height).coerceAtLeast(MIN_BAR.toPx())
                                top = (top - height).coerceAtLeast(0f)
                                drawRoundRect(color, Offset(0f, top), Size(size.width, height), radius)
                            }
                        },
                )
                Text(
                    text = bar.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/** Pastille de couleur et son libellé. */
@Composable
private fun Legend(
    color: Color,
    label: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

/** Nom abrégé du mois, sous sa barre (« sept. »). */
private fun MonthlyActivity.shortName(locale: Locale): String =
    month.month.getDisplayName(TextStyle.SHORT_STANDALONE, locale)

/** Nom complet du mois, pour la description lue par TalkBack. */
private fun MonthlyActivity.fullName(locale: Locale): String =
    month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)

private const val PERCENT = 100
private val CHART_HEIGHT = 120.dp
private val BAR_RADIUS = 4.dp
private val BASELINE = 2.dp
private val MIN_BAR = 3.dp
