package fr.babeuloula.photoswipe.ui.deck

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.ui.common.LocalPlatformFeatures
import fr.babeuloula.photoswipe.ui.theme.motion
import fr.babeuloula.photoswipe.ui.theme.swipeColors

/**
 * Boutons équivalents aux gestes, sous la carte (SPEC §4.3, §8) : « Supprimer » à gauche comme le
 * glissement à gauche, « Garder » à droite comme le glissement à droite, et entre les deux
 * « Annuler » et « Plus d'actions » (comme le glissement vers le haut), chacun avec son libellé
 * dessous. Ils déclenchent les mêmes actions que les gestes, pour TalkBack, la navigation au clavier
 * ou par préférence ; l'appelant fait sortir la carte comme un geste avant d'enregistrer la
 * décision. Pendant un geste vers [highlighted], le bouton de ce côté se remplit et s'arrondit
 * (ADR 0025). Sur grand écran ou en paysage, ils se placent en colonne à côté de la carte
 * ([vertical]).
 */
@Composable
fun DecisionButtons(
    canUndo: Boolean,
    onClick: (DeckButton) -> Unit,
    modifier: Modifier = Modifier,
    vertical: Boolean = false,
    highlighted: CardSide? = null,
) {
    if (vertical) {
        // À côté de la carte, sur grand écran ou en paysage : même ordre de haut en bas.
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { Buttons(canUndo, onClick, highlighted) }
    } else {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) { Buttons(canUndo, onClick, highlighted) }
    }
}

/** Les quatre boutons, dans l'ordre de lecture : « Supprimer », « Annuler », « Plus d'actions », « Garder ». */
@Composable
private fun Buttons(
    canUndo: Boolean,
    onClick: (DeckButton) -> Unit,
    highlighted: CardSide?,
) {
    val colors = MaterialTheme.swipeColors
    val scheme = MaterialTheme.colorScheme
    val deleting = highlighted == CardSide.LEFT
    LabeledButton(
        face =
            ButtonFace(
                R.drawable.ic_delete,
                stringResource(R.string.deck_mark_for_deletion),
                stringResource(R.string.deck_delete),
            ),
        look =
            ButtonLook(
                container = if (deleting) colors.delete else colors.deleteContainer,
                content = if (deleting) colors.onDelete else colors.onDeleteContainer,
                width = DECISION_WIDTH,
                height = DECISION_HEIGHT,
                radius = if (deleting) DECISION_HEIGHT / 2 else DECISION_RADIUS,
            ),
        onClick = { onClick(DeckButton.MARK_FOR_DELETION) },
    )
    UndoButton(enabled = canUndo, onClick = { onClick(DeckButton.UNDO) }, labeled = true)
    LabeledButton(
        face =
            ButtonFace(
                R.drawable.ic_more_horiz,
                stringResource(R.string.deck_menu),
                stringResource(R.string.deck_menu),
            ),
        look = menuButtonLook(highlighted == CardSide.TOP),
        onClick = { onClick(DeckButton.MENU) },
    )
    val keeping = highlighted == CardSide.RIGHT
    LabeledButton(
        face = ButtonFace(R.drawable.ic_check, stringResource(R.string.deck_keep), stringResource(R.string.deck_keep)),
        look =
            ButtonLook(
                container = if (keeping) colors.keep else colors.keepContainer,
                content = if (keeping) colors.onKeep else colors.onKeepContainer,
                width = DECISION_WIDTH,
                height = DECISION_HEIGHT,
                radius = if (keeping) DECISION_HEIGHT / 2 else DECISION_RADIUS,
            ),
        onClick = { onClick(DeckButton.KEEP) },
    )
}

/**
 * « Annuler » (SPEC §3.4), entre les deux décisions et plus discret qu'elles ; inactif tant que la
 * session n'a rien à annuler. Rond de 64 dp ; [labeled] ajoute son libellé dessous, comme dans la
 * rangée des boutons de la pile.
 */
@Composable
fun UndoButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    labeled: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    LabeledButton(
        face =
            ButtonFace(
                icon = R.drawable.ic_undo,
                description = stringResource(R.string.deck_undo),
                label = if (labeled) stringResource(R.string.deck_undo) else null,
            ),
        look = ButtonLook(container = scheme.surfaceContainerHigh, content = scheme.onSurfaceVariant),
        enabled = enabled,
        onClick = onClick,
        modifier = modifier,
    )
}

/**
 * Contenu d'un bouton de la pile.
 *
 * @property icon icône Material Symbols.
 * @property description description lue par TalkBack.
 * @property label libellé visible sous le bouton, que TalkBack ne relit pas ; `null` pour aucun.
 */
private data class ButtonFace(
    @DrawableRes val icon: Int,
    val description: String,
    val label: String?,
)

/**
 * Apparence d'un bouton de la pile.
 *
 * @property container couleur de fond.
 * @property content couleur de l'icône.
 * @property width largeur.
 * @property height hauteur.
 * @property radius rayon des coins, animé quand il change (morphose quand un geste vise ce bouton).
 */
private data class ButtonLook(
    val container: Color,
    val content: Color,
    val width: Dp = ROUND_SIZE,
    val height: Dp = ROUND_SIZE,
    val radius: Dp = ROUND_SIZE / 2,
)

/** Bouton de la pile, de contenu [face] et d'apparence [look], son libellé dessous. */
@Composable
private fun LabeledButton(
    face: ButtonFace,
    look: ButtonLook,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val motion = MaterialTheme.motion
    val animatedRadius by animateDpAsState(look.radius, motion.fastSpatial(), label = "radius")
    val animatedContainer by animateColorAsState(look.container, motion.defaultEffects(), label = "container")
    val animatedContent by animateColorAsState(look.content, motion.defaultEffects(), label = "content")
    Column(
        modifier = modifier.width(look.width),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FilledIconButton(
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(animatedRadius),
            colors =
                IconButtonDefaults.filledIconButtonColors(
                    containerColor = animatedContainer,
                    contentColor = animatedContent,
                ),
            modifier = Modifier.size(width = look.width, height = look.height),
        ) {
            Icon(painter = painterResource(face.icon), contentDescription = face.description)
        }
        if (face.label != null) {
            Text(
                text = face.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
    }
}

/** Taille des boutons ronds (« Annuler », « Plus d'actions »), au-delà des 48 dp minimaux (SPEC §8). */
private val ROUND_SIZE = 64.dp

/** Largeur des boutons de décision, plus larges que les autres : ce sont les actions principales. */
private val DECISION_WIDTH = 88.dp

/** Hauteur des boutons de décision. */
private val DECISION_HEIGHT = 72.dp

/** Rayon des boutons de décision au repos. */
private val DECISION_RADIUS = 20.dp

/**
 * Aspect du bouton « Plus d'actions » : bleu du favori pendant un glissement vers le haut
 * ([favoring]), ou neutre sans favoris de la galerie (Android 10), le haut n'ouvrant alors que le
 * menu (PS-183).
 */
@Composable
private fun menuButtonLook(favoring: Boolean): ButtonLook {
    val colors = MaterialTheme.swipeColors
    val scheme = MaterialTheme.colorScheme
    val favorites = LocalPlatformFeatures.current.galleryFavorites
    return when {
        !favoring -> ButtonLook(container = scheme.surfaceContainerHigh, content = scheme.onSurfaceVariant)
        favorites -> ButtonLook(container = colors.favorite, content = colors.onFavorite)
        else -> ButtonLook(container = scheme.secondaryContainer, content = scheme.onSecondaryContainer)
    }
}
