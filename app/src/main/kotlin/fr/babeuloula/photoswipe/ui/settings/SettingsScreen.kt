package fr.babeuloula.photoswipe.ui.settings

import android.text.format.DateFormat
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.painterResource
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
import fr.babeuloula.photoswipe.ui.common.segmentShape
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date

/**
 * Réglages (SPEC §4.5), en liste de préférences Material 3 : le rappel quotidien (SPEC §3.6),
 * activé ou non, et son heure, choisie dans le sélecteur d'heure Material 3 ; la langue de
 * l'application ([SettingsEnvironment.languageLabel]), choisie dans le réglage de langue par application d'Android ;
 * le thème (celui du téléphone, clair ou sombre) ; les statistiques d'utilisation ; « Soutenir PhotoSwipe » (noter l'application, faire un don) ;
 * puis « À propos » : version de l'application et licences.
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
                            painterResource(R.drawable.ic_arrow_back),
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
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ReminderSection(reminder, environment.exactAlarmAllowed, onEvent)
                        LanguageSection(environment.languageLabel, onEvent)
                        state.themeMode?.let { AppearanceSection(it, onEvent, state.animationsEnabled) }
                        state.statistics?.let { StatisticsSection(it) }
                        SupportSection(onEvent)
                        AboutSection(onEvent, environment.showDeveloperTools)
                    }
                }
                if (state.timePickerOpen) ReminderTimeDialog(reminder.time, onEvent)
            }
        }
    }
}

/**
 * Section du rappel quotidien : interrupteur, puis heure. Rappel activé sans l'autorisation des
 * alarmes exactes : une ligne de plus explique qu'il peut arriver un peu en retard et ouvre
 * « Alarmes et rappels » d'Android (PS-125).
 */
@Composable
private fun ReminderSection(
    reminder: ReminderSettings,
    exactAlarmAllowed: Boolean,
    onEvent: (SettingsEvent) -> Unit,
) {
    val askExactAlarm = reminder.enabled && !exactAlarmAllowed
    val rows = if (askExactAlarm) REMINDER_ROWS + 1 else REMINDER_ROWS
    PreferenceSection(stringResource(R.string.settings_reminder_section)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            // Toute la ligne bascule l'interrupteur, et TalkBack l'annonce comme un interrupteur.
            PreferenceRow(
                shape = segmentShape(0, rows),
                title = stringResource(R.string.settings_reminder_enabled),
                summary = stringResource(R.string.settings_reminder_enabled_summary),
                trailing = { Switch(checked = reminder.enabled, onCheckedChange = null) },
                modifier =
                    Modifier.toggleable(
                        value = reminder.enabled,
                        role = Role.Switch,
                        onValueChange = { onEvent(SettingsEvent.ReminderToggled(it)) },
                    ),
            )
            PreferenceRow(
                shape = segmentShape(1, rows),
                title = stringResource(R.string.settings_reminder_time),
                // Avec les alarmes exactes, « À 18:00 pile » ; sinon « Vers 18:00 » (PS-125).
                summary =
                    stringResource(
                        if (exactAlarmAllowed) {
                            R.string.settings_reminder_time_exact
                        } else {
                            R.string.settings_reminder_time_summary
                        },
                        timeLabel(reminder.time),
                    ),
                modifier = Modifier.clickable(role = Role.Button) { onEvent(SettingsEvent.PickReminderTime) },
            )
            if (askExactAlarm) {
                PreferenceRow(
                    shape = segmentShape(REMINDER_ROWS, rows),
                    title = stringResource(R.string.settings_reminder_exact),
                    summary = stringResource(R.string.settings_reminder_exact_summary),
                    trailing = { TrailingIcon(R.drawable.ic_open_in_new) },
                    modifier = Modifier.clickable(role = Role.Button) { onEvent(SettingsEvent.OpenExactAlarmSettings) },
                )
            }
        }
    }
}

/** Lignes du rappel toujours affichées : interrupteur et heure. */
private const val REMINDER_ROWS = 2

/**
 * Ce que les réglages lisent hors de leur ViewModel.
 *
 * @property languageLabel langue de l'application, lue dans le système (SPEC §9).
 * @property showDeveloperTools vrai si la variante a des outils développeur (debug, SPEC §10).
 * @property exactAlarmAllowed vrai si les alarmes exactes sont autorisées : le rappel arrive à
 * l'heure pile (PS-125).
 */
data class SettingsEnvironment(
    val languageLabel: String = "",
    val showDeveloperTools: Boolean = false,
    val exactAlarmAllowed: Boolean = true,
)

/** Section « Langue » : la langue de l'application, choisie dans les réglages d'Android (SPEC §9). */
@Composable
private fun LanguageSection(
    languageLabel: String,
    onEvent: (SettingsEvent) -> Unit,
) {
    PreferenceSection(stringResource(R.string.settings_language_section)) {
        PreferenceRow(
            shape = segmentShape(0, 1),
            title = stringResource(R.string.settings_language),
            summary = languageLabel,
            // S'ouvre dans les réglages d'Android, hors de l'application.
            trailing = { TrailingIcon(R.drawable.ic_open_in_new) },
            modifier = Modifier.clickable(role = Role.Button) { onEvent(SettingsEvent.OpenLanguageSettings) },
        )
    }
}

/** Section « À propos » : version de l'application, tutoriel à revoir, licences et code source. */
@Composable
private fun AboutSection(
    onEvent: (SettingsEvent) -> Unit,
    showDeveloperTools: Boolean,
) {
    // Version, tutoriel, licences, code source et, en debug, les outils développeur.
    val count = if (showDeveloperTools) ABOUT_ROWS + 1 else ABOUT_ROWS
    PreferenceSection(stringResource(R.string.settings_about_section)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            PreferenceRow(
                shape = segmentShape(0, count),
                title = stringResource(R.string.settings_about_version),
                summary = appVersion(),
            )
            PreferenceRow(
                shape = segmentShape(1, count),
                title = stringResource(R.string.settings_about_tutorial),
                summary = stringResource(R.string.settings_about_tutorial_summary),
                trailing = { TrailingIcon(R.drawable.ic_chevron_right) },
                modifier = Modifier.clickable(role = Role.Button) { onEvent(SettingsEvent.OpenTutorial) },
            )
            PreferenceRow(
                shape = segmentShape(2, count),
                title = stringResource(R.string.settings_about_licenses),
                summary = stringResource(R.string.settings_about_licenses_summary),
                trailing = { TrailingIcon(R.drawable.ic_chevron_right) },
                modifier = Modifier.clickable(role = Role.Button) { onEvent(SettingsEvent.OpenLicenses) },
            )
            PreferenceRow(
                shape = segmentShape(ABOUT_ROWS - 1, count),
                title = stringResource(R.string.settings_about_source_code),
                summary = stringResource(R.string.settings_about_source_code_summary),
                trailing = { TrailingIcon(R.drawable.ic_open_in_new) },
                modifier = Modifier.clickable(role = Role.Button) { onEvent(SettingsEvent.OpenSourceCode) },
            )
            // Variante debug seulement (SPEC §10).
            if (showDeveloperTools) {
                PreferenceRow(
                    shape = segmentShape(ABOUT_ROWS, count),
                    title = stringResource(R.string.settings_developer_tools),
                    trailing = { TrailingIcon(R.drawable.ic_chevron_right) },
                    modifier = Modifier.clickable(role = Role.Button) { onEvent(SettingsEvent.OpenDeveloperTools) },
                )
            }
        }
    }
}

/** Lignes de « À propos » dans toutes les variantes : version et licences. */
private const val ABOUT_ROWS = 4

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
                .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp)
                .semantics { heading() },
    )
}

/** Icône de fin de ligne, décorative : chevron ou « ouvrir ailleurs ». */
@Composable
internal fun TrailingIcon(
    @DrawableRes icon: Int,
) {
    Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
