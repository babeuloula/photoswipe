package fr.babeuloula.photoswipe.domain.model

/** Indice prudent sur une photo peut-être ratée (PS-164) : jamais une décision, seulement une pastille. */
enum class QualityHint {
    /** Peu de détails nets : la photo est peut-être floue. */
    BLURRY,

    /** Image très sombre dans son ensemble. */
    DARK,
}

/**
 * Mesures d'une photo, prises sur une vignette réduite en niveaux de gris (PS-164, ADR 0039).
 *
 * @property sharpness variance du laplacien : faible pour une image floue.
 * @property brightness luminance moyenne, de 0 (noir) à 255 (blanc).
 */
data class PhotoMeasure(
    val sharpness: Double,
    val brightness: Double,
)

/**
 * Image en niveaux de gris, ligne par ligne, à mesurer.
 *
 * @property width largeur en pixels.
 * @property height hauteur en pixels.
 * @property luma luminance de chaque pixel, de 0 à 255, `width × height` valeurs.
 */
class GrayImage(
    val width: Int,
    val height: Int,
    val luma: IntArray,
) {
    init {
        require(width > 0 && height > 0) { "an image has a size: ${width}x$height" }
        require(luma.size == width * height) { "${luma.size} pixels for a ${width}x$height image" }
    }
}
