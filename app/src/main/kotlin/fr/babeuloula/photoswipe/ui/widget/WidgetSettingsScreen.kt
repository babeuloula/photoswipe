package fr.babeuloula.photoswipe.ui.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.ui.common.segmentShape
import fr.babeuloula.photoswipe.ui.settings.PreferenceRow
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme

/**
 * Réglage du widget « Ce jour-là » (PS-170), à sa pose et depuis « Réglages du widget » : montrer
 * une photo ou non, pour un écran d'accueil vu par d'autres. Sans état : l'activité de
 * configuration garde le choix et l'enregistre à « Terminé ».
 */
@Composable
fun WidgetSettingsScreen(
    showPhoto: Boolean,
    onShowPhotoChange: (Boolean) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier =
                Modifier
                    .safeDrawingPadding()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.widget_settings_title),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = 24.dp).semantics { heading() },
                )
                Text(
                    text = stringResource(R.string.widget_settings_text),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PreferenceRow(
                    shape = segmentShape(0, 1),
                    title = stringResource(R.string.widget_settings_show_photo),
                    summary = stringResource(R.string.widget_settings_show_photo_summary),
                    trailing = { Switch(checked = showPhoto, onCheckedChange = null) },
                    modifier =
                        Modifier.toggleable(
                            value = showPhoto,
                            role = Role.Switch,
                            onValueChange = onShowPhotoChange,
                        ),
                )
                Spacer(Modifier)
            }
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text(stringResource(R.string.widget_settings_done))
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun WidgetSettingsScreenPreview() {
    PhotoSwipeTheme {
        WidgetSettingsScreen(showPhoto = true, onShowPhotoChange = {}, onDone = {})
    }
}
