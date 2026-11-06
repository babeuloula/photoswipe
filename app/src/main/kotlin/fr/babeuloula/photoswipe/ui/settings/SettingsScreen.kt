package fr.babeuloula.photoswipe.ui.settings

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.ReminderSettings
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date

/**
 * Réglages (SPEC §4) : pour l'instant le rappel quotidien (SPEC §3.6), activé ou non, et son
 * heure, choisie dans le sélecteur d'heure Material 3. La langue et « À propos » arrivent avec
 * l'écran complet des réglages (PS-63).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = { onEvent(SettingsEvent.Back) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            val reminder = state.reminder
            if (reminder == null) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                ReminderSection(reminder, onEvent)
                if (state.timePickerOpen) ReminderTimeDialog(reminder.time, onEvent)
            }
        }
    }
}

/** Section du rappel quotidien : interrupteur, puis heure. */
@Composable
private fun ReminderSection(
    reminder: ReminderSettings,
    onEvent: (SettingsEvent) -> Unit,
) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Text(
            text = stringResource(R.string.settings_reminder_section),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier =
                Modifier
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
                    .semantics { heading() },
        )
        // Toute la ligne bascule l'interrupteur, et TalkBack l'annonce comme un interrupteur.
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_reminder_enabled)) },
            supportingContent = { Text(stringResource(R.string.settings_reminder_enabled_summary)) },
            trailingContent = { Switch(checked = reminder.enabled, onCheckedChange = null) },
            modifier =
                Modifier.toggleable(
                    value = reminder.enabled,
                    role = Role.Switch,
                    onValueChange = { onEvent(SettingsEvent.ReminderToggled(it)) },
                ),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_reminder_time)) },
            supportingContent = {
                Text(stringResource(R.string.settings_reminder_time_summary, timeLabel(reminder.time)))
            },
            modifier = Modifier.clickable(role = Role.Button) { onEvent(SettingsEvent.PickReminderTime) },
        )
    }
}

/** Sélecteur d'heure Material 3, au format 12 ou 24 heures du téléphone. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(
    time: LocalTime,
    onEvent: (SettingsEvent) -> Unit,
) {
    val pickerState =
        rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = DateFormat.is24HourFormat(LocalContext.current),
        )
    AlertDialog(
        onDismissRequest = { onEvent(SettingsEvent.TimePickerDismissed) },
        title = { Text(stringResource(R.string.settings_reminder_time_picker)) },
        text = { TimePicker(state = pickerState) },
        confirmButton = {
            TextButton(onClick = {
                onEvent(SettingsEvent.ReminderTimePicked(LocalTime.of(pickerState.hour, pickerState.minute)))
            }) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(SettingsEvent.TimePickerDismissed) }) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

/** Heure au format du téléphone (12 ou 24 heures, langue de l'application). */
@Composable
private fun timeLabel(time: LocalTime): String {
    val instant =
        LocalDate
            .now()
            .atTime(time)
            .atZone(ZoneId.systemDefault())
            .toInstant()
    return DateFormat.getTimeFormat(LocalContext.current).format(Date.from(instant))
}

@PreviewLightDark
@Composable
private fun SettingsScreenPreview() {
    PhotoSwipeTheme {
        SettingsScreen(
            state = SettingsUiState(reminder = ReminderSettings.DEFAULT.copy(enabled = true)),
            onEvent = {},
        )
    }
}
