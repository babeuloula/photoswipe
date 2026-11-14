package fr.babeuloula.photoswipe.ui.summary

import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaActionRequestFactory.FakeMediaActionRequest
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.port.MediaActionRequest
import fr.babeuloula.photoswipe.ui.common.DirectDeletion

/** Aucune suppression directe : chaque demande passe par un dialogue d'Android (Android 11+). */
object NoDirectDeletion : DirectDeletion {
    override fun handles(request: MediaActionRequest): Boolean = false

    override suspend fun delete(request: MediaActionRequest): MediaActionRequest? = error("no direct deletion")
}

/**
 * Suppression directe d'Android 10 simulée (PS-184) : chaque demande est supprimée sans dialogue ;
 * les médias de [refused] ne le sont pas, comme ceux qu'Android refuse de supprimer.
 */
class FakeDirectDeletion(
    private val refused: Set<MediaId> = emptySet(),
) : DirectDeletion {
    /** Médias supprimés, dans l'ordre. */
    val deleted = mutableListOf<MediaId>()

    override fun handles(request: MediaActionRequest): Boolean = true

    override suspend fun delete(request: MediaActionRequest): MediaActionRequest? {
        val ids = request.mediaIds - refused
        deleted += ids
        return if (ids.isEmpty()) null else FakeMediaActionRequest((request as FakeMediaActionRequest).kind, ids)
    }
}
