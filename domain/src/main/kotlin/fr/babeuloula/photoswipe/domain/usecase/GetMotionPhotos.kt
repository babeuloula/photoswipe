package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.MediaType
import fr.babeuloula.photoswipe.domain.model.MotionPhoto
import fr.babeuloula.photoswipe.domain.port.MotionPhotoReader

/**
 * Photos animées parmi [media] (PS-167) : seules les photos sont lues, une fois chacune ; le
 * lecteur garde ce qu'il a trouvé. Une photo qui ne se lit pas reste une photo ordinaire.
 */
class GetMotionPhotos(
    private val reader: MotionPhotoReader,
) {
    /** Vidéo de chaque photo animée de [media]. */
    suspend operator fun invoke(media: List<MediaItem>): Map<MediaId, MotionPhoto> =
        media
            .filter { it.type == MediaType.IMAGE }
            .distinctBy { it.id }
            .mapNotNull { photo -> reader.motionPhoto(photo)?.let { photo.id to it } }
            .toMap()
}
