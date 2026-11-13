package fr.babeuloula.photoswipe.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.ui.common.segmentShape

/**
 * Section « Activité » : une ligne qui ouvre la page « Progression » (PS-153), une autre la page
 * « Corbeille » (PS-168).
 */
@Composable
internal fun ProgressSection(onEvent: (SettingsEvent) -> Unit) {
    PreferenceSection(stringResource(R.string.settings_statistics_section)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            PreferenceRow(
                shape = segmentShape(0, 2),
                title = stringResource(R.string.settings_progress),
                summary = stringResource(R.string.settings_progress_summary),
                trailing = { TrailingIcon(R.drawable.ic_chevron_right) },
                modifier = Modifier.clickable(role = Role.Button) { onEvent(SettingsEvent.OpenProgress) },
            )
            PreferenceRow(
                shape = segmentShape(1, 2),
                title = stringResource(R.string.trash_title),
                summary = stringResource(R.string.settings_trash_summary),
                trailing = { TrailingIcon(R.drawable.ic_chevron_right) },
                modifier = Modifier.clickable(role = Role.Button) { onEvent(SettingsEvent.OpenTrash) },
            )
        }
    }
}
