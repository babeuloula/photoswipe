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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import fr.babeuloula.photoswipe.domain.model.ThemeMode
import fr.babeuloula.photoswipe.ui.common.CenteredContent
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date

/**
 * Réglages (SPEC §4.5), en liste de préférences Material 3 : le rappel quotidien (SPEC §3.6),
 * activé ou non, et son heure, choisie dans le sélecteur d'heure Material 3 ; la langue de
 * l'application ([SettingsEnvironment.languageLabel]), choisie dans le réglage de langue par application d'Android ;
 * le thème (celui du téléphone, clair ou sombre) ; puis « À propos » : version de l'application et licences.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
    environment: SettingsEnvironment = SettingsEnvironment(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                CenteredContent {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        ReminderSection(reminder, onEvent)
                        LanguageSection(environment.languageLabel, onEvent)
                        state.themeMode?.let { AppearanceSection(it, onEvent) }
                        AboutSection(onEvent, environment.showDeveloperTools)
                    }
                }
                if (state.timePickerOpen) ReminderTimeDialog(reminder.time, onEvent)
                val themeMode = state.themeMode
                if (state.themeDialogOpen && themeMode != null) ThemeDialog(themeMode, onEvent)
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
    Column {
        SectionTitle(stringResource(R.string.settings_reminder_section))
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

/**
 * Ce que les réglages lisent hors de leur ViewModel.
 *
 * @property languageLabel langue de l'application, lue dans le système (SPEC §9).
 * @property showDeveloperTools vrai si la variante a des outils développeur (debug, SPEC §10).
 */
data class SettingsEnvironment(
    val languageLabel: String = "",
    val showDeveloperTools: Boolean = false,
)

/** Section « Langue » : la langue de l'application, choisie dans les réglages d'Android (SPEC §9). */
@Composable
private fun LanguageSection(
    languageLabel: String,
    onEvent: (SettingsEvent) -> Unit,
) {
    Column {
        SectionTitle(stringResource(R.string.settings_language_section))
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_language)) },
            supportingContent = { Text(languageLabel) },
            modifier = Modifier.clickable(role = Role.Button) { onEvent(SettingsEvent.OpenLanguageSettings) },
        )
    }
}

/** Section « À propos » : version de l'application et licences. */
@Composable
private fun AboutSection(
    onEvent: (SettingsEvent) -> Unit,
    showDeveloperTools: Boolean,
) {
    Column {
        SectionTitle(stringResource(R.string.settings_about_section))
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_about_version)) },
            supportingContent = { Text(appVersion()) },
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_about_licenses)) },
            supportingContent = { Text(stringResource(R.string.settings_about_licenses_summary)) },
            modifier = Modifier.clickable(role = Role.Button) { onEvent(SettingsEvent.OpenLicenses) },
        )
        // Variante debug seulement (SPEC §10).
        if (showDeveloperTools) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_developer_tools)) },
                modifier = Modifier.clickable(role = Role.Button) { onEvent(SettingsEvent.OpenDeveloperTools) },
            )
        }
    }
}

/** Version de l'application installée (`versionName`). */
@Composable
private fun appVersion(): String {
    val context = LocalContext.current
    return remember(context) {
        context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName
            .orEmpty()
    }
}

/** Titre de section, annoncé comme tel par TalkBack. */
@Composable
internal fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier =
            Modifier
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
                .semantics { heading() },
    )
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
            state =
                SettingsUiState(
                    reminder = ReminderSettings.DEFAULT.copy(enabled = true),
                    themeMode = ThemeMode.SYSTEM,
                ),
            environment = SettingsEnvironment(languageLabel = "Français"),
            onEvent = {},
        )
    }
}
