package fr.babeuloula.photoswipe.ui.deck

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.QualityHint
import fr.babeuloula.photoswipe.ui.common.FilmEdgeChip

/**
 * Pastille prudente d'une photo peut-être ratée (PS-164) : « Floue ? » ou « Très sombre ? ».
 * Elle n'est qu'une aide : la décision reste à l'utilisateur. TalkBack lit une phrase entière.
 */
@Composable
internal fun QualityHintChip(
    hint: QualityHint,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(qualityHintDescription(hint))
    FilmEdgeChip(modifier = modifier.clearAndSetSemantics { contentDescription = description }) {
        Text(stringResource(qualityHintLabel(hint)))
    }
}

/** Texte court de la pastille. */
internal fun qualityHintLabel(hint: QualityHint): Int =
    when (hint) {
        QualityHint.BLURRY -> R.string.deck_quality_blurry
        QualityHint.DARK -> R.string.deck_quality_dark
    }

/** Phrase lue par TalkBack. */
internal fun qualityHintDescription(hint: QualityHint): Int =
    when (hint) {
        QualityHint.BLURRY -> R.string.deck_quality_blurry_description
        QualityHint.DARK -> R.string.deck_quality_dark_description
    }
