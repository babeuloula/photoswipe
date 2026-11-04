package fr.babeuloula.photoswipe.domain.fixtures

import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.MediaType
import java.time.Duration
import java.time.Instant

/** Instant de prise de vue par défaut des builders : fixe, pour des tests déterministes. */
val DEFAULT_TAKEN_AT: Instant = Instant.parse("2020-07-15T10:00:00Z")

/** Taille par défaut d'un média construit par [aMediaItem] : 2 Mo. */
const val DEFAULT_SIZE_BYTES: Long = 2_000_000

/**
 * Construit un [MediaItem] valide, dont le test ne précise que ce qui compte pour lui.
 *
 * Par défaut : une photo d'identifiant 1, prise à [DEFAULT_TAKEN_AT]. L'URI est déduite de
 * l'identifiant et du type, comme celle de MediaStore (`copy(uri = …)` pour en imposer une autre).
 * Les identifiants sont explicites : aucun compteur global, pour que les tests ne dépendent pas
 * de leur ordre d'exécution.
 */
fun aMediaItem(
    id: Long = 1,
    type: MediaType = MediaType.IMAGE,
    takenAt: Instant = DEFAULT_TAKEN_AT,
    sizeBytes: Long = DEFAULT_SIZE_BYTES,
    duration: Duration? = null,
): MediaItem =
    MediaItem(
        id = MediaId(id),
        uri = contentUri(id, type),
        type = type,
        takenAt = takenAt,
        sizeBytes = sizeBytes,
        duration = duration,
    )

/** URI de contenu MediaStore d'un média, du type `content://media/external/images/media/42`. */
private fun contentUri(
    id: Long,
    type: MediaType,
): String {
    val collection =
        when (type) {
            MediaType.IMAGE -> "images"
            MediaType.VIDEO -> "video"
        }
    return "content://media/external/$collection/media/$id"
}
