package fr.babeuloula.photoswipe.domain.service

import fr.babeuloula.photoswipe.domain.model.GrayImage

/**
 * Empreinte perceptuelle d'une photo (PS-165, ADR 0035 complété) : la vignette est réduite à
 * 9 × 8 pixels, et chaque bit dit si un pixel est plus clair que son voisin de droite (« dHash »).
 * Deux copies d'une même image, réencodées ou redimensionnées, ont des empreintes qui diffèrent de
 * très peu de bits ; deux photos différentes, même prises en rafale, de beaucoup plus.
 */
object PhotoFingerprint {
    /** Bits d'écart sous lesquels deux photos sont des copies de la même image. */
    const val DUPLICATE_DISTANCE: Int = 4

    private const val COLUMNS = 9
    private const val ROWS = 8

    /** Empreinte de [image]. */
    fun of(image: GrayImage): Long {
        var hash = 0L
        for (row in 0 until ROWS) {
            for (column in 0 until COLUMNS - 1) {
                hash = hash shl 1
                if (cell(image, column, row) > cell(image, column + 1, row)) hash = hash or 1L
            }
        }
        return hash
    }

    /** Bits qui diffèrent entre [a] et [b]. */
    fun distance(
        a: Long,
        b: Long,
    ): Int = java.lang.Long.bitCount(a xor b)

    /** Vrai si [a] et [b] sont des empreintes d'une même image. */
    fun sameImage(
        a: Long,
        b: Long,
    ): Boolean = distance(a, b) <= DUPLICATE_DISTANCE

    /** Moyenne des pixels de [image] dans la case ([column], [row]) de la grille 9 × 8. */
    private fun cell(
        image: GrayImage,
        column: Int,
        row: Int,
    ): Int {
        val left = column * image.width / COLUMNS
        val right = maxOf(left + 1, (column + 1) * image.width / COLUMNS)
        val top = row * image.height / ROWS
        val bottom = maxOf(top + 1, (row + 1) * image.height / ROWS)
        var sum = 0
        var count = 0
        for (y in top until minOf(bottom, image.height)) {
            for (x in left until minOf(right, image.width)) {
                sum += image.luma[y * image.width + x]
                count++
            }
        }
        return if (count == 0) 0 else sum / count
    }
}
