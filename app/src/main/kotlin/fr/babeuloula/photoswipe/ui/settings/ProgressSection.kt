package fr.babeuloula.photoswipe.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.ui.common.segmentShape

/** Section « Statistiques » : une ligne qui ouvre la page « Progression » (PS-153). */
@Composable
internal fun ProgressSection(onEvent: (SettingsEvent) -> Unit) {
    PreferenceSection(stringResource(R.string.settings_statistics_section)) {
        PreferenceRow(
            shape = segmentShape(0, 1),
            title = stringResource(R.string.settings_progress),
            summary = stringResource(R.string.settings_progress_summary),
            trailing = { TrailingIcon(R.drawable.ic_chevron_right) },
            modifier = Modifier.clickable(role = Role.Button) { onEvent(SettingsEvent.OpenProgress) },
        )
    }
}
