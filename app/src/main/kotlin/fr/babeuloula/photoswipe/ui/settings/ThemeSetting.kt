package fr.babeuloula.photoswipe.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.ThemeMode
import fr.babeuloula.photoswipe.ui.common.segmentShape

// Réglage du thème de l'application (SPEC §4.5, §8) : celui du téléphone, clair ou sombre.

/**
 * Section « Apparence » : le thème de l'application, en boutons radio dans la liste (ADR 0025) :
 * « Celui du téléphone », « Clair », « Sombre », appliqué dès qu'on touche un thème ; puis
 * l'interrupteur « Noir pur » (PS-176), qui ne change que le thème sombre, et l'interrupteur
 * « Animations » (PS-146), à couper sur un téléphone ancien.
 */
@Composable
internal fun AppearanceSection(
    themeMode: ThemeMode,
    onEvent: (SettingsEvent) -> Unit,
    animationsEnabled: Boolean = true,
    pureBlack: Boolean = false,
) {
    PreferenceSection(stringResource(R.string.settings_appearance_section)) {
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.settings_theme),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(segmentShape(0, ThemeMode.entries.size + 1))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
            )
            ThemeMode.entries.forEachIndexed { index, mode ->
                PreferenceRow(
                    shape = segmentShape(index + 1, ThemeMode.entries.size + 1),
                    modifier =
                        Modifier.selectable(
                            selected = mode == themeMode,
                            role = Role.RadioButton,
                            onClick = { onEvent(SettingsEvent.ThemePicked(mode)) },
                        ),
                    leading = { RadioButton(selected = mode == themeMode, onClick = null) },
                    title = stringResource(mode.label),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            SwitchRow(
                shape = segmentShape(0, 2),
                title = stringResource(R.string.settings_pure_black),
                summary = stringResource(R.string.settings_pure_black_summary),
                checked = pureBlack,
                onCheckedChange = { onEvent(SettingsEvent.PureBlackToggled(it)) },
            )
            SwitchRow(
                shape = segmentShape(1, 2),
                title = stringResource(R.string.settings_animations),
                summary = stringResource(R.string.settings_animations_summary),
                checked = animationsEnabled,
                onCheckedChange = { onEvent(SettingsEvent.AnimationsToggled(it)) },
            )
        }
    }
}

/** Ligne à interrupteur : toute la ligne le bascule, et TalkBack l'annonce comme un interrupteur. */
@Composable
private fun SwitchRow(
    shape: Shape,
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    PreferenceRow(
        shape = shape,
        title = title,
        summary = summary,
        trailing = { Switch(checked = checked, onCheckedChange = null) },
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    )
}

/** Nom du thème affiché à l'utilisateur. */
private val ThemeMode.label: Int
    get() =
        when (this) {
            ThemeMode.SYSTEM -> R.string.settings_theme_system
            ThemeMode.LIGHT -> R.string.settings_theme_light
            ThemeMode.DARK -> R.string.settings_theme_dark
        }
