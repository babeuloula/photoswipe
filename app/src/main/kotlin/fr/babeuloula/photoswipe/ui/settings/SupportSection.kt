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
 * Section « Soutenir PhotoSwipe » (PS-120) : noter l'application sur le Play Store, faire un don
 * avec PayPal. Les deux s'ouvrent hors de l'application.
 */
@Composable
internal fun SupportSection(onEvent: (SettingsEvent) -> Unit) {
    PreferenceSection(stringResource(R.string.settings_support_section)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            PreferenceRow(
                shape = segmentShape(0, 2),
                title = stringResource(R.string.settings_support_rate),
                summary = stringResource(R.string.settings_support_rate_summary),
                trailing = { TrailingIcon(R.drawable.ic_open_in_new) },
                modifier = Modifier.clickable(role = Role.Button) { onEvent(SettingsEvent.RateApp) },
            )
            PreferenceRow(
                shape = segmentShape(1, 2),
                title = stringResource(R.string.settings_support_donate),
                summary = stringResource(R.string.settings_support_donate_summary),
                trailing = { TrailingIcon(R.drawable.ic_open_in_new) },
                modifier = Modifier.clickable(role = Role.Button) { onEvent(SettingsEvent.Donate) },
            )
        }
    }
}
