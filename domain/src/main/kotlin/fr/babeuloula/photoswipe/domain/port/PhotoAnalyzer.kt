package fr.babeuloula.photoswipe.domain.port

import fr.babeuloula.photoswipe.domain.model.GrayImage
import fr.babeuloula.photoswipe.domain.model.MediaItem

/** Lit une photo réduite en niveaux de gris, pour la mesurer (PS-164). Implémenté dans `:data`. */
interface PhotoAnalyzer {
    /** Vignette de [media] en niveaux de gris, `null` si elle ne se lit pas. */
    suspend fun grayThumbnail(media: MediaItem): GrayImage?
}
