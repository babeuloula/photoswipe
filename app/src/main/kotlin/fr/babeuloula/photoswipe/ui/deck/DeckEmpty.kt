package fr.babeuloula.photoswipe.ui.deck

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R

/**
 * Pile vidée (SPEC §4.3) : rien à trier pour ce jour, les choix sont enregistrés et rien n'est
 * supprimé sans confirmation ; « Voir le résumé », « Retour à l'accueil » et, s'il y a une
 * décision à annuler, « Annuler ».
 */
@Composable
internal fun EmptyDeck(
    canUndo: Boolean,
    onEvent: (DeckEvent) -> Unit,
) {
    // Défilable pour une grande police ; sinon le message au milieu et les boutons en bas.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(Modifier)
            EmptyDeckMessage(canUndo = canUndo, onEvent = onEvent)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onEvent(DeckEvent.Finish) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) {
                    Text(stringResource(R.string.deck_view_summary))
                }
                TextButton(
                    onClick = { onEvent(DeckEvent.Back) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) {
                    Text(stringResource(R.string.deck_back_home))
                }
            }
        }
    }
}

/** Message de la pile vidée, et « Annuler » s'il y a une décision à annuler. */
@Composable
private fun EmptyDeckMessage(
    canUndo: Boolean,
    onEvent: (DeckEvent) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier.size(64.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_done_all),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Text(
            text = stringResource(R.string.deck_empty),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.deck_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (canUndo) UndoButton(enabled = true, onClick = { onEvent(DeckEvent.Undo) }, labeled = true)
    }
}
