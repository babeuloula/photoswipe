package fr.babeuloula.photoswipe.domain.service

import fr.babeuloula.photoswipe.domain.model.GrayImage
import fr.babeuloula.photoswipe.domain.model.PhotoMeasure
import fr.babeuloula.photoswipe.domain.model.QualityHint

/**
 * Repère les photos peut-être ratées (PS-164, ADR 0039), sans bibliothèque ni modèle : la netteté
 * est la variance du laplacien d'une vignette en niveaux de gris, la luminosité sa moyenne.
 *
 * Les seuils sont prudents : une photo de nuit voulue ou un arrière-plan flou volontaire peuvent
 * être signalés, c'est pourquoi l'application ne fait qu'afficher une pastille interrogative.
 *
 * @param blurThreshold netteté sous laquelle la photo est dite floue.
 * @param darkThreshold luminance moyenne sous laquelle elle est dite très sombre.
 */
class PhotoQualityRules(
    private val blurThreshold: Double = DEFAULT_BLUR_THRESHOLD,
    private val darkThreshold: Double = DEFAULT_DARK_THRESHOLD,
) {
    /** Mesure [image] : variance du laplacien à 4 voisins, luminance moyenne et empreinte (PS-165). */
    fun measure(image: GrayImage): PhotoMeasure {
        val w = image.width
        val h = image.height
        val luma = image.luma
        val brightness = luma.average()
        val fingerprint = PhotoFingerprint.of(image)
        if (w < MIN_SIZE || h < MIN_SIZE) return PhotoMeasure(0.0, brightness, fingerprint)
        var sum = 0.0
        var sumOfSquares = 0.0
        var count = 0
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val i = y * w + x
                val laplacian =
                    (luma[i - 1] + luma[i + 1] + luma[i - w] + luma[i + w] - NEIGHBOURS * luma[i])
                        .toDouble()
                sum += laplacian
                sumOfSquares += laplacian * laplacian
                count++
            }
        }
        val mean = sum / count
        return PhotoMeasure(sumOfSquares / count - mean * mean, brightness, fingerprint)
    }

    /** Indice de [measure] : très sombre d'abord (le flou s'y mesure mal), puis floue ; `null` sinon. */
    fun hint(measure: PhotoMeasure): QualityHint? =
        when {
            measure.brightness < darkThreshold -> QualityHint.DARK
            measure.sharpness < blurThreshold -> QualityHint.BLURRY
            else -> null
        }

    companion object {
        /** Côté minimal pour mesurer le laplacien : un pixel et ses quatre voisins. */
        private const val MIN_SIZE = 3

        /** Voisins du laplacien à 4 voisins. */
        private const val NEIGHBOURS = 4

        /** Netteté d'une vignette de 256 px nettement floue ; une photo nette dépasse largement 100. */
        const val DEFAULT_BLUR_THRESHOLD: Double = 15.0

        /** Luminance moyenne d'une photo presque noire (sur 255). */
        const val DEFAULT_DARK_THRESHOLD: Double = 30.0
    }
}
