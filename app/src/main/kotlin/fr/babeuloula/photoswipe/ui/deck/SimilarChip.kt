package fr.babeuloula.photoswipe.ui.deck

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.SimilarGroup
import fr.babeuloula.photoswipe.ui.common.FilmEdgeChip

/**
 * « ⧉ 1/3 » : la photo a des voisines prises à la suite (PS-140), pour comparer avant de décider ;
 * « ⧉ Doublon 1/2 » si une copie de la même image est dans le groupe (PS-165).
 */
@Composable
internal fun SimilarChip(
    similar: SimilarGroup,
    modifier: Modifier = Modifier,
) {
    val description =
        stringResource(
            if (similar.duplicate) R.string.deck_duplicate_description else R.string.deck_similar_description,
            similar.position,
            similar.size,
        )
    FilmEdgeChip(modifier = modifier.clearAndSetSemantics { contentDescription = description }) {
        Icon(painterResource(R.drawable.ic_burst_mode), contentDescription = null, modifier = Modifier.size(18.dp))
        Text(
            stringResource(
                if (similar.duplicate) R.string.deck_duplicate else R.string.deck_similar,
                similar.position,
                similar.size,
            ),
        )
    }
}
