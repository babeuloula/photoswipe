package fr.babeuloula.photoswipe.domain.fixtures

import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.port.MediaActionRequest
import fr.babeuloula.photoswipe.domain.port.MediaActionRequestFactory

/**
 * [MediaActionRequestFactory] en mémoire : comme l'implémentation MediaStore, il découpe les médias
 * en lots de [batchSize] au plus, dans l'ordre reçu, et une liste vide donne une liste vide.
 *
 * @param batchSize nombre maximal de médias par demande ; petit dans les tests pour avoir plusieurs lots.
 */
class FakeMediaActionRequestFactory(
    private val batchSize: Int = DEFAULT_BATCH_SIZE,
) : MediaActionRequestFactory {
    init {
        require(batchSize > 0) { "batchSize must be positive: $batchSize" }
    }

    override fun trashRequests(media: List<MediaItem>): List<MediaActionRequest> = requests(Kind.TRASH, media)

    override fun favoriteRequests(media: List<MediaItem>): List<MediaActionRequest> = requests(Kind.FAVORITE, media)

    override fun unfavoriteRequests(media: List<MediaItem>): List<MediaActionRequest> = requests(Kind.UNFAVORITE, media)

    override fun restoreRequests(media: List<MediaItem>): List<MediaActionRequest> = requests(Kind.RESTORE, media)

    override fun deleteRequests(media: List<MediaItem>): List<MediaActionRequest> = requests(Kind.DELETE, media)

    private fun requests(
        kind: Kind,
        media: List<MediaItem>,
    ): List<MediaActionRequest> =
        media.chunked(batchSize) { batch -> FakeMediaActionRequest(kind, batch.mapTo(LinkedHashSet()) { it.id }) }

    /** Nature d'une demande : corbeille, favori, restauration ou suppression définitive. */
    enum class Kind {
        /** Mise à la corbeille. */
        TRASH,

        /** Ajout aux favoris. */
        FAVORITE,

        /** Retrait des favoris. */
        UNFAVORITE,

        /** Sortie de la corbeille (PS-168). */
        RESTORE,

        /** Suppression définitive (PS-168). */
        DELETE,
    }

    /**
     * Demande factice : sa nature et les médias qu'elle couvre, dans l'ordre reçu.
     *
     * @property kind corbeille, ajout ou retrait de favori.
     */
    data class FakeMediaActionRequest(
        val kind: Kind,
        override val mediaIds: Set<MediaId>,
    ) : MediaActionRequest

    companion object {
        /** Taille de lot par défaut, celle prévue par la SPEC §3.5. */
        const val DEFAULT_BATCH_SIZE: Int = 500
    }
}
