package fr.babeuloula.photoswipe.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.ui.common.segmentShape

/**
 * Section « Gestes » : « Secouer pour annuler » (PS-175), désactivé par défaut pour ne rien annuler
 * sans le vouloir dans les transports.
 */
@Composable
internal fun ShakeSection(
    shakeToUndo: Boolean,
    onEvent: (SettingsEvent) -> Unit,
) {
    PreferenceSection(stringResource(R.string.settings_gestures_section)) {
        SwitchRow(
            shape = segmentShape(0, 1),
            title = stringResource(R.string.settings_shake_to_undo),
            summary = stringResource(R.string.settings_shake_to_undo_summary),
            checked = shakeToUndo,
            onCheckedChange = { onEvent(SettingsEvent.ShakeToUndoToggled(it)) },
        )
    }
}
