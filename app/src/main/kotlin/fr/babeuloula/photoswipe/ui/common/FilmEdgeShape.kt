package fr.babeuloula.photoswipe.ui.common

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/**
 * Forme du repère de bord de film (ADR 0025) : trois coins arrondis de [cornerRadius] et le coin du
 * haut côté fin de ligne (haut-droit en français) coupé en biais sur [cut]. Compose n'a pas de
 * forme qui mêle coins arrondis et coin coupé.
 */
class FilmEdgeShape(
    private val cornerRadius: Dp = FILM_EDGE_CORNER,
    private val cut: Dp = FILM_EDGE_CUT,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val radius = with(density) { cornerRadius.toPx() }.coerceAtMost(size.minDimension / 2)
        val cutPx = with(density) { cut.toPx() }.coerceAtMost(size.minDimension / 2)
        val path =
            Path().apply {
                // Tracé pour une langue de gauche à droite, retourné pour une langue de droite à gauche.
                moveTo(radius, 0f)
                lineTo(size.width - cutPx, 0f)
                lineTo(size.width, cutPx)
                lineTo(size.width, size.height - radius)
                quadraticTo(size.width, size.height, size.width - radius, size.height)
                lineTo(radius, size.height)
                quadraticTo(0f, size.height, 0f, size.height - radius)
                lineTo(0f, radius)
                quadraticTo(0f, 0f, radius, 0f)
                close()
            }
        if (layoutDirection == LayoutDirection.Rtl) {
            path.transform(
                Matrix().apply {
                    translate(size.width, 0f)
                    scale(-1f, 1f)
                },
            )
        }
        return Outline.Generic(path)
    }
}
