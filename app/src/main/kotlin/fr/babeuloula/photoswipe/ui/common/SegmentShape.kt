package fr.babeuloula.photoswipe.ui.common

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Forme d'un élément de liste segmentée (ADR 0025) : 16 dp aux extrémités de la liste, 4 dp ailleurs. */
fun segmentShape(
    index: Int,
    count: Int,
): Shape {
    val first = index == 0
    val last = index == count - 1
    return RoundedCornerShape(
        topStart = if (first) SEGMENT_OUTER else SEGMENT_INNER,
        topEnd = if (first) SEGMENT_OUTER else SEGMENT_INNER,
        bottomStart = if (last) SEGMENT_OUTER else SEGMENT_INNER,
        bottomEnd = if (last) SEGMENT_OUTER else SEGMENT_INNER,
    )
}

/** Rayon des coins aux extrémités d'une liste segmentée. */
private val SEGMENT_OUTER = 16.dp

/** Rayon des coins entre deux éléments d'une liste segmentée. */
private val SEGMENT_INNER = 4.dp
