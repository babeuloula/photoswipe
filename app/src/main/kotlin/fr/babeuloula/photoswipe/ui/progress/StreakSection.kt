package fr.babeuloula.photoswipe.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.Streak
import fr.babeuloula.photoswipe.ui.common.segmentShape
import fr.babeuloula.photoswipe.ui.settings.PreferenceRow
import fr.babeuloula.photoswipe.ui.settings.PreferenceSection

/**
 * Section « Série » de la page « Progression » (PS-155) : la série de jours en cours, avec ce qui
 * la fait tenir, et la meilleure série. Sans série, la première ligne dit comment en commencer une.
 * Chaque ligne est lue d'un bloc par TalkBack.
 */
@Composable
internal fun StreakSection(streak: Streak) {
    val block = Modifier.semantics(mergeDescendants = true) {}
    PreferenceSection(stringResource(R.string.progress_streak_section)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            PreferenceRow(
                shape = segmentShape(0, 2),
                title = stringResource(R.string.progress_streak_current),
                summary =
                    if (streak.current == 0) {
                        stringResource(R.string.progress_streak_none)
                    } else {
                        pluralStringResource(R.plurals.progress_streak_days, streak.current, streak.current) +
                            "\n" + stringResource(R.string.progress_streak_rule)
                    },
                modifier = block,
            )
            PreferenceRow(
                shape = segmentShape(1, 2),
                title = stringResource(R.string.progress_streak_best),
                summary =
                    if (streak.best == 0) {
                        stringResource(R.string.progress_streak_best_none)
                    } else {
                        pluralStringResource(R.plurals.progress_streak_days, streak.best, streak.best)
                    },
                modifier = block,
            )
        }
    }
}
