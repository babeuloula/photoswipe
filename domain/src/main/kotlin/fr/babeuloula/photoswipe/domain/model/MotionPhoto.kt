package fr.babeuloula.photoswipe.domain.model

/**
 * Photo animée (PS-167, ADR 0040) : un JPEG suivi d'une courte vidéo MP4, dont on garde l'endroit.
 *
 * @property format façon dont l'appareil a décrit la vidéo.
 * @property videoStart position du premier octet de la vidéo dans le fichier.
 * @property videoLength nombre d'octets de la vidéo.
 */
data class MotionPhoto(
    val format: MotionPhotoFormat,
    val videoStart: Long,
    val videoLength: Long,
) {
    init {
        require(videoStart > 0) { "videoStart must be after the photo: $videoStart" }
        require(videoLength > 0) { "videoLength must be positive: $videoLength" }
    }
}

/** Façon dont une photo animée décrit sa vidéo. */
enum class MotionPhotoFormat {
    /** XMP de Google (`GCamera:MicroVideo`, `GCamera:MotionPhoto`, `Camera:MotionPhoto`). */
    GOOGLE_XMP,

    /** Trailer `SEFT` de Samsung, avec son bloc `MotionPhoto_Data`. */
    SAMSUNG_TRAILER,
}
