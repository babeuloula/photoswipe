package fr.babeuloula.photoswipe.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.SessionSummary
import fr.babeuloula.photoswipe.ui.deck.fileSizeLabel

/**
 * Espace libéré en tête du résumé : estimé s'il reste des médias marqués, sinon celui libéré
 * aujourd'hui ; sans [trash] (Android 10), une suppression définitive rend la place tout de suite (PS-185).
 */
@Composable
internal fun SummaryFreedSpace(
    summary: SessionSummary,
    trash: Boolean,
) {
    when {
        summary.markedForDeletionCount > 0 -> {
            // Sans corbeille (Android 10), la place est rendue dès la confirmation (PS-185).
            FreedSpace(
                title = stringResource(R.string.summary_freed_space),
                bytes = summary.estimatedFreedBytes,
                detail =
                    pluralStringResource(
                        if (trash) R.plurals.summary_if_confirmed else R.plurals.summary_if_confirmed_now,
                        summary.markedForDeletionCount,
                        summary.markedForDeletionCount,
                    ),
            )
        }

        // Tout est confirmé : la place libérée aujourd'hui plutôt qu'une estimation à zéro (ADR 0034).
        summary.trashedTodayCount > 0 -> {
            FreedSpace(
                title =
                    stringResource(
                        if (trash) R.string.summary_freed_today else R.string.summary_deleted_for_good_today,
                    ),
                bytes = summary.trashedTodayBytes,
                detail =
                    pluralStringResource(
                        if (trash) {
                            R.plurals.summary_trashed_today_detail
                        } else {
                            R.plurals.summary_deleted_for_good_detail
                        },
                        summary.trashedTodayCount,
                        summary.trashedTodayCount,
                    ),
            )
        }
    }
}

/**
 * Espace libéré, mis en avant sans carte (ADR 0025) : le nombre en grand, en chasse fixe, sous son
 * [title] (« Espace libéré estimé » ou « Espace libéré aujourd'hui »), et ce dont il dépend ([detail]).
 */
@Composable
private fun FreedSpace(
    title: String,
    bytes: Long,
    detail: String,
) {
    Column(
        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { heading() },
        )
        // Une seule ligne : la taille du texte descend du displayLarge jusqu'à une taille lisible.
        val style = MaterialTheme.typography.displayLarge
        Text(
            text = fileSizeLabel(LocalContext.current, bytes),
            style = style,
            maxLines = 1,
            softWrap = false,
            autoSize = TextAutoSize.StepBased(minFontSize = FREED_SPACE_MIN_FONT_SIZE, maxFontSize = style.fontSize),
            modifier = Modifier.testTag(FREED_SPACE_TAG),
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Taille la plus petite de l'espace libéré, encore lisible, quand le displayLarge ne tient pas. */
private val FREED_SPACE_MIN_FONT_SIZE = 24.sp
