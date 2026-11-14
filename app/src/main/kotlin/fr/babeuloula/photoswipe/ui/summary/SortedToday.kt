package fr.babeuloula.photoswipe.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.SessionSummary
import fr.babeuloula.photoswipe.ui.common.LocalPlatformFeatures
import fr.babeuloula.photoswipe.ui.common.segmentShape
import fr.babeuloula.photoswipe.ui.deck.fileSizeLabel
import fr.babeuloula.photoswipe.ui.theme.swipeColors

/**
 * Gardés, favoris et, s'il y en a, supprimés aujourd'hui (ADR 0024, 0034). Sans favoris de la
 * galerie (Android 10), pas de ligne des favoris (PS-183).
 */
@Composable
internal fun SortedToday(summary: SessionSummary) {
    val colors = MaterialTheme.swipeColors
    val favorites = LocalPlatformFeatures.current.galleryFavorites
    val lines = 1 + (if (favorites) 1 else 0) + if (summary.trashedTodayCount > 0) 1 else 0
    Column {
        SummaryHeading(stringResource(R.string.summary_sorted_today))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            SummaryLine(
                icon = R.drawable.ic_check,
                tint = colors.keep,
                text = pluralStringResource(R.plurals.summary_kept, summary.keptCount, summary.keptCount),
                shape = segmentShape(0, lines),
            )
            if (favorites) {
                SummaryLine(
                    icon = R.drawable.ic_favorite_filled,
                    tint = colors.favorite,
                    text =
                        pluralStringResource(
                            R.plurals.summary_favorites,
                            summary.favoriteCount,
                            summary.favoriteCount,
                        ),
                    shape = segmentShape(1, lines),
                )
            }
            if (summary.trashedTodayCount > 0) {
                SummaryLine(
                    icon = R.drawable.ic_delete,
                    tint = colors.delete,
                    text =
                        pluralStringResource(
                            // Sans corbeille (Android 10), la suppression est définitive (PS-185).
                            if (LocalPlatformFeatures.current.systemTrash) {
                                R.plurals.summary_deleted_today
                            } else {
                                R.plurals.summary_deleted_for_good_today
                            },
                            summary.trashedTodayCount,
                            summary.trashedTodayCount,
                            fileSizeLabel(LocalContext.current, summary.trashedTodayBytes),
                        ),
                    shape = segmentShape(lines - 1, lines),
                )
            }
        }
    }
}
