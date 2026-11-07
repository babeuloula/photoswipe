package fr.babeuloula.photoswipe.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.UsageStatistics
import fr.babeuloula.photoswipe.ui.common.segmentShape
import fr.babeuloula.photoswipe.ui.deck.fileSizeLabel
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Section « Statistiques » des réglages (PS-122) : depuis quand l'application sert, combien de
 * médias ont été envoyés à la corbeille et la place libérée. Avant toute suppression, une seule
 * ligne le dit. Chaque ligne est lue d'un bloc par TalkBack.
 */
@Composable
internal fun StatisticsSection(statistics: UsageStatistics) {
    val locale = LocalConfiguration.current.locales[0]
    val since = statistics.since.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale))
    val rows = if (statistics.deletedCount == 0) 2 else STATISTICS_ROWS
    val block = Modifier.semantics(mergeDescendants = true) {}
    PreferenceSection(stringResource(R.string.settings_statistics_section)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            PreferenceRow(
                shape = segmentShape(0, rows),
                title = stringResource(R.string.settings_statistics_since),
                summary = since,
                modifier = block,
            )
            if (statistics.deletedCount == 0) {
                PreferenceRow(
                    shape = segmentShape(1, rows),
                    title = stringResource(R.string.settings_statistics_nothing_deleted),
                    modifier = block,
                )
            } else {
                PreferenceRow(
                    shape = segmentShape(1, rows),
                    title = stringResource(R.string.settings_statistics_deleted),
                    summary =
                        pluralStringResource(
                            R.plurals.settings_statistics_deleted_count,
                            statistics.deletedCount,
                            statistics.deletedCount,
                        ),
                    modifier = block,
                )
                PreferenceRow(
                    shape = segmentShape(2, rows),
                    title = stringResource(R.string.settings_statistics_freed),
                    summary = fileSizeLabel(LocalContext.current, statistics.freedBytes),
                    modifier = block,
                )
            }
        }
    }
}

/** Lignes de la section une fois des médias supprimés : depuis quand, combien, quelle place. */
private const val STATISTICS_ROWS = 3
