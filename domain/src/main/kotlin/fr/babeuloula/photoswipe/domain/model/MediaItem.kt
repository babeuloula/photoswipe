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
 * @property isFavorite vrai si le média est déjà en favori dans la galerie (`IS_FAVORITE`).
 * @property fileName nom du fichier (`DISPLAY_NAME`), `null` s'il est inconnu (PS-166).
 * @property folder dossier du fichier (`RELATIVE_PATH`), `null` s'il est inconnu.
 * @property rawCompanion fichier RAW de la même prise de vue qu'une photo JPEG (PS-166) : la paire
 * se trie comme une seule photo, et chaque décision vaut pour les deux fichiers ; `null` sinon.
 */
data class MediaItem(
    val id: MediaId,
    val uri: String,
    val type: MediaType,
    val takenAt: Instant,
    val sizeBytes: Long,
    val duration: Duration? = null,
    val isFavorite: Boolean = false,
    val fileName: String? = null,
    val folder: String? = null,
    val rawCompanion: MediaItem? = null,
) {
    /** Poids de la photo et de son fichier RAW s'il en a un : ce que libère sa suppression. */
    val totalSizeBytes: Long get() = sizeBytes + (rawCompanion?.sizeBytes ?: 0)

    init {
        require(sizeBytes >= 0) { "sizeBytes must not be negative: $sizeBytes" }
        require(type == MediaType.VIDEO || duration == null) { "an image has no duration" }
        require(duration == null || !duration.isNegative) { "duration must not be negative: $duration" }
    }
}
