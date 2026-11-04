package fr.babeuloula.photoswipe.domain.model

import java.time.Duration
import java.time.Instant

/**
 * Média (photo ou vidéo) de la galerie, tel que le domaine le voit (SPEC §5.2).
 *
 * @property id identifiant MediaStore du média.
 * @property uri URI de contenu, gardée en `String` pour que le domaine ignore `android.net.Uri`.
 * @property type photo ou vidéo.
 * @property takenAt instant de la prise de vue. La couche données lit `DATE_TAKEN` et se replie
 * sur `DATE_ADDED` s'il est absent (SPEC §3.1). On garde un instant, sans fuseau : la conversion
 * en jour local se fait au moment du calcul, avec le fuseau de l'appareil (ADR 0006).
 * @property sizeBytes taille du fichier en octets, utilisée pour l'espace libéré estimé.
 * @property duration durée d'une vidéo, `null` si elle est inconnue ; toujours `null` pour une photo.
 */
data class MediaItem(
    val id: MediaId,
    val uri: String,
    val type: MediaType,
    val takenAt: Instant,
    val sizeBytes: Long,
    val duration: Duration? = null,
) {
    init {
        require(sizeBytes >= 0) { "sizeBytes must not be negative: $sizeBytes" }
        require(type == MediaType.VIDEO || duration == null) { "an image has no duration" }
        require(duration == null || !duration.isNegative) { "duration must not be negative: $duration" }
    }
}
