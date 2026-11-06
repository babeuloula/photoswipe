package fr.babeuloula.photoswipe.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.ThemeMode

// Réglage du thème de l'application (SPEC §4.5, §8) : celui du téléphone, clair ou sombre.

/** Section « Apparence » : le thème de l'application. */
@Composable
internal fun AppearanceSection(
    themeMode: ThemeMode,
    onEvent: (SettingsEvent) -> Unit,
) {
    Column {
        SectionTitle(stringResource(R.string.settings_appearance_section))
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_theme)) },
            supportingContent = { Text(stringResource(themeMode.label)) },
            modifier = Modifier.clickable(role = Role.Button) { onEvent(SettingsEvent.PickTheme) },
        )
    }
}

/** Nom du thème affiché à l'utilisateur. */
private val ThemeMode.label: Int
    get() =
        when (this) {
            ThemeMode.SYSTEM -> R.string.settings_theme_system
            ThemeMode.LIGHT -> R.string.settings_theme_light
            ThemeMode.DARK -> R.string.settings_theme_dark
        }

/** Choix du thème : une liste de boutons radio, appliquée dès qu'on touche un thème. */
@Composable
internal fun ThemeDialog(
    selected: ThemeMode,
    onEvent: (SettingsEvent) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onEvent(SettingsEvent.ThemeDialogDismissed) },
        title = { Text(stringResource(R.string.settings_theme)) },
        text = {
            Column(Modifier.selectableGroup()) {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .selectable(
                                    selected = mode == selected,
                                    role = Role.RadioButton,
                                    onClick = { onEvent(SettingsEvent.ThemePicked(mode)) },
                                ),
                    ) {
                        RadioButton(selected = mode == selected, onClick = null)
                        Text(
                            text = stringResource(mode.label),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onEvent(SettingsEvent.ThemeDialogDismissed) }) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}
