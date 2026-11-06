package fr.babeuloula.photoswipe.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.ui.theme.monoLabel

/**
 * Repère de bord de film « Il y a 3 ans · 6 nov. 2023 », la signature de PhotoSwipe (ADR 0025) :
 * texte ambre en chasse fixe sur brun, coin coupé. Rôles fixes du thème : identique en clair et en
 * sombre, et lisible sur n'importe quelle photo.
 */
@Composable
fun FilmEdgeLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    FilmEdgeSurface(
        shape = FilmEdgeShape(),
        contentColor = MaterialTheme.colorScheme.primaryFixedDim,
        endPadding = 14.dp,
        modifier = modifier,
    ) {
        Text(text = text, maxLines = 1)
    }
}

/**
 * Puce secondaire du bord de film (poids, durée) : même conteneur que [FilmEdgeLabel], sans coin
 * coupé, avec un contenu libre (icône et texte).
 */
@Composable
fun FilmEdgeChip(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    FilmEdgeSurface(
        shape = RoundedCornerShape(FILM_EDGE_CORNER),
        contentColor = MaterialTheme.colorScheme.primaryFixed,
        endPadding = 12.dp,
        modifier = modifier,
        content = content,
    )
}

/** Conteneur commun du repère et des puces : 32 dp de haut, brun du bord de film, texte en chasse fixe. */
@Composable
private fun FilmEdgeSurface(
    shape: Shape,
    contentColor: androidx.compose.ui.graphics.Color,
    endPadding: Dp,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.onPrimaryFixed,
        contentColor = contentColor,
        shape = shape,
        modifier = modifier,
    ) {
        CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.monoLabel) {
            Row(
                modifier = Modifier.heightIn(min = FILM_EDGE_HEIGHT).padding(start = 12.dp, end = endPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                content = content,
            )
        }
    }
}

/** Hauteur du repère et des puces. */
private val FILM_EDGE_HEIGHT = 32.dp

/** Rayon des coins : les 28 dp de la carte moins ses 16 dp de marge. */
internal val FILM_EDGE_CORNER = 12.dp

/** Longueur du coin coupé. */
internal val FILM_EDGE_CUT = 10.dp
