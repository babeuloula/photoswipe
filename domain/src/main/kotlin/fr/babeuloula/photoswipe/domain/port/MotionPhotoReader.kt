package fr.babeuloula.photoswipe.domain.port

import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.MotionPhoto

/** Reconnaît les photos animées (PS-167). Implémenté dans `:data`, sur le fichier de la photo. */
interface MotionPhotoReader {
    /** Vidéo de [media] si c'est une photo animée lisible, `null` sinon ou si le fichier ne se lit pas. */
    suspend fun motionPhoto(media: MediaItem): MotionPhoto?
}
