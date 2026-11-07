package fr.babeuloula.photoswipe.ui.snooze

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import java.time.LocalTime

/**
 * Sélecteur d'heure de la reprogrammation du rappel d'aujourd'hui (SPEC §3.6), ouvert par l'action
 * « Reprogrammer » de la notification. Sans état : l'heure refusée est signalée par [timePassed].
 *
 * @param initialTime heure proposée à l'ouverture.
 * @param is24Hour format du téléphone.
 * @param timePassed vrai après le choix d'une heure déjà passée : un message l'explique.
 * @param onConfirm heure choisie.
 * @param onDismiss annulation : rien ne change.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnoozeReminderDialog(
    initialTime: LocalTime,
    is24Hour: Boolean,
    timePassed: Boolean,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState =
        rememberTimePickerState(
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute,
            is24Hour = is24Hour,
        )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reminder_snooze_title)) },
        text = {
            Column {
                TimePicker(state = pickerState)
                if (timePassed) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.reminder_snooze_time_passed),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(pickerState.hour, pickerState.minute)) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}

@PreviewLightDark
@Composable
private fun SnoozeReminderDialogPreview() {
    PhotoSwipeTheme {
        SnoozeReminderDialog(
            initialTime = LocalTime.NOON,
            is24Hour = true,
            timePassed = true,
            onConfirm = {},
            onDismiss = {},
        )
    }
}
