package fr.babeuloula.photoswipe.ui.summary

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import fr.babeuloula.photoswipe.R

/**
 * Confirmation de PhotoSwipe avant l'envoi à la corbeille, quand l'accès « Gestion des médias »
 * fait disparaître celle d'Android (ADR 0023) : rien ne part à la corbeille sans un dernier accord.
 */
@Composable
internal fun TrashConfirmationDialog(
    count: Int,
    onEvent: (SummaryEvent) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onEvent(SummaryEvent.TrashConfirmationDismissed) },
        title = { Text(pluralStringResource(R.plurals.summary_trash_confirm_title, count, count)) },
        text = { Text(stringResource(R.string.summary_trash_confirm_text)) },
        confirmButton = {
            TextButton(onClick = { onEvent(SummaryEvent.TrashConfirmationAccepted) }) {
                Text(stringResource(R.string.summary_trash_confirm_action))
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(SummaryEvent.TrashConfirmationDismissed) }) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}
