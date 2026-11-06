package fr.babeuloula.photoswipe.ui.deck

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.ui.theme.swipeColors

/**
 * Boutons équivalents aux gestes, sous la carte (SPEC §4.3, §8) : « Supprimer » à gauche comme le
 * glissement à gauche, « Garder » à droite comme le glissement à droite, et entre les deux
 * « Annuler » et « Menu » (comme le glissement vers le haut). Ils déclenchent les mêmes actions que
 * les gestes, pour TalkBack, la navigation au clavier ou par préférence ; l'appelant fait sortir la
 * carte comme un geste avant d'enregistrer la décision. Sur grand écran ou en paysage, ils se
 * placent en colonne à côté de la carte ([vertical]).
 */
@Composable
fun DecisionButtons(
    canUndo: Boolean,
    onClick: (DeckButton) -> Unit,
    modifier: Modifier = Modifier,
    vertical: Boolean = false,
) {
    if (vertical) {
        // À côté de la carte, sur grand écran ou en paysage : même ordre de haut en bas.
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { Buttons(canUndo, onClick) }
    } else {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) { Buttons(canUndo, onClick) }
    }
}

/** Les quatre boutons, dans l'ordre de lecture : « Supprimer », « Annuler », « Menu », « Garder ». */
@Composable
private fun Buttons(
    canUndo: Boolean,
    onClick: (DeckButton) -> Unit,
) {
    val colors = MaterialTheme.swipeColors
    DecisionButton(
        icon = R.drawable.ic_delete,
        label = stringResource(R.string.deck_mark_for_deletion),
        container = colors.delete,
        content = colors.onDelete,
        onClick = { onClick(DeckButton.MARK_FOR_DELETION) },
    )
    UndoButton(enabled = canUndo, onClick = { onClick(DeckButton.UNDO) })
    OutlinedIconButton(onClick = { onClick(DeckButton.MENU) }, modifier = Modifier.size(48.dp)) {
        Icon(
            painter = painterResource(R.drawable.ic_more_horiz),
            contentDescription = stringResource(R.string.deck_menu),
        )
    }
    DecisionButton(
        icon = R.drawable.ic_check,
        label = stringResource(R.string.deck_keep),
        container = colors.keep,
        content = colors.onKeep,
        onClick = { onClick(DeckButton.KEEP) },
    )
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
    @DrawableRes icon: Int,
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
        Icon(painter = painterResource(icon), contentDescription = label, modifier = Modifier.size(32.dp))
    }
}
