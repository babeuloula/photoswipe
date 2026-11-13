package fr.babeuloula.photoswipe.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.CatchUpWindow
import fr.babeuloula.photoswipe.ui.common.segmentShape

/**
 * Section « Rattrapage » (PS-169) : « Jours à rattraper », 7 (par défaut), 14 ou 30, en boutons
 * radio dans la liste comme le thème, appliqué dès qu'on touche une durée. Une fenêtre plus longue
 * laisse rattraper ses souvenirs après une absence de plus d'une semaine.
 */
@Composable
internal fun CatchUpSection(
    window: CatchUpWindow,
    onEvent: (SettingsEvent) -> Unit,
) {
    val rows = CatchUpWindow.entries.size + 1
    PreferenceSection(stringResource(R.string.settings_catch_up_section)) {
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(segmentShape(0, rows))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_catch_up_days),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.settings_catch_up_days_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            CatchUpWindow.entries.forEachIndexed { index, option ->
                PreferenceRow(
                    shape = segmentShape(index + 1, rows),
                    modifier =
                        Modifier.selectable(
                            selected = option == window,
                            role = Role.RadioButton,
                            onClick = { onEvent(SettingsEvent.CatchUpWindowPicked(option)) },
                        ),
                    leading = { RadioButton(selected = option == window, onClick = null) },
                    title = pluralStringResource(R.plurals.settings_catch_up_days_option, option.days, option.days),
                )
            }
        }
    }
}

/**
 * Section « Aide au tri » (PS-164) : signaler ou non les photos peut-être floues ou très sombres,
 * d'une pastille « Floue ? » ou « Très sombre ? ». Actif par défaut.
 */
@Composable
internal fun SortingHelpSection(
    qualityHints: Boolean,
    onEvent: (SettingsEvent) -> Unit,
) {
    PreferenceSection(stringResource(R.string.settings_sorting_help_section)) {
        SwitchRow(
            shape = segmentShape(0, 1),
            title = stringResource(R.string.settings_quality_hints),
            summary = stringResource(R.string.settings_quality_hints_summary),
            checked = qualityHints,
            onCheckedChange = { onEvent(SettingsEvent.QualityHintsToggled(it)) },
        )
    }
}
