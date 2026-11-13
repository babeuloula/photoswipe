package fr.babeuloula.photoswipe.domain.model

import java.time.Instant

/**
 * Média encore dans la corbeille d'Android, tel que MediaStore le décrit (PS-168).
 *
 * @property media le média, avec l'URI de sa collection, que les demandes de restauration et de
 * suppression définitive attendent.
 * @property purgeAt instant où Android le supprimera définitivement, `null` s'il est inconnu.
 */
data class TrashedMedia(
    val media: MediaItem,
    val purgeAt: Instant?,
)

/**
 * Une ligne de la page « Corbeille » : un média supprimé par PhotoSwipe, encore restaurable.
 *
 * @property media le média, avec son poids dans la corbeille.
 * @property confirmedAt instant où sa suppression a été confirmée.
 * @property purgeAt instant où Android le supprimera définitivement.
 */
data class TrashItem(
    val media: MediaItem,
    val confirmedAt: Instant,
    val purgeAt: Instant,
)

/**
 * Corbeille de PhotoSwipe : les médias qu'il a envoyés à la corbeille d'Android et qui y sont encore
 * (PS-168), du plus proche de sa suppression définitive au plus lointain.
 *
 * @property items médias encore restaurables.
 */
data class Trash(
    val items: List<TrashItem> = emptyList(),
) {
    /** Place que ces médias occupent encore, jusqu'à leur suppression définitive. */
    val occupiedBytes: Long get() = items.sumOf { it.media.sizeBytes }

    /** Vrai si aucun média supprimé par PhotoSwipe n'est encore dans la corbeille. */
    val isEmpty: Boolean get() = items.isEmpty()
}
