package fr.babeuloula.photoswipe.ui.deck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.ui.theme.swipeColors

/**
 * Boutons équivalents aux gestes, sous la carte (SPEC §4.3, §8) : « Supprimer » à gauche comme le
 * glissement à gauche, « Garder » à droite comme le glissement à droite, et « Annuler » entre les deux. Ils déclenchent les mêmes
 * événements que les gestes, pour TalkBack, la navigation au clavier ou par préférence ; l'appelant
 * fait sortir la carte comme un geste avant d'enregistrer la décision.
 */
@Composable
fun DecisionButtons(
    canUndo: Boolean,
    onMarkForDeletion: () -> Unit,
    onUndo: () -> Unit,
    onKeep: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.swipeColors
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DecisionButton(
            icon = Icons.Filled.Delete,
            label = stringResource(R.string.deck_mark_for_deletion),
            container = colors.delete,
            content = colors.onDelete,
            onClick = onMarkForDeletion,
        )
        UndoButton(enabled = canUndo, onClick = onUndo)
        DecisionButton(
            icon = Icons.Filled.Check,
            label = stringResource(R.string.deck_keep),
            container = colors.keep,
            content = colors.onKeep,
            onClick = onKeep,
        )
    }
}

/**
 * « Annuler » (SPEC §3.4), entre les deux décisions et plus discret qu'elles ; inactif tant que la
 * session n'a rien à annuler. Cible tactile de 48 dp.
 */
@Composable
fun UndoButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedIconButton(onClick = onClick, enabled = enabled, modifier = modifier.size(48.dp)) {
        Icon(painter = painterResource(R.drawable.ic_undo), contentDescription = stringResource(R.string.deck_undo))
    }
}

/** Bouton rond de 64 dp, au-delà des 48 dp de cible tactile minimale (SPEC §8). */
@Composable
private fun DecisionButton(
    icon: ImageVector,
    label: String,
    container: Color,
    content: Color,
    onClick: () -> Unit,
) {
    FilledIconButton(
        onClick = onClick,
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = container, contentColor = content),
        modifier = Modifier.size(64.dp),
    ) {
        Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(32.dp))
    }
}
