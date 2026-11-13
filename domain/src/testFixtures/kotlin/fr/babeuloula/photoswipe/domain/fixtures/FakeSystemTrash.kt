package fr.babeuloula.photoswipe.domain.fixtures

import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.TrashedMedia
import fr.babeuloula.photoswipe.domain.port.SystemTrash
import java.time.Instant

/**
 * [SystemTrash] en mémoire : une corbeille que le test remplit ([add]) et vide ([remove], comme une
 * restauration, une suppression définitive ou la fin des 30 jours). Les résultats suivent l'ordre
 * d'ajout, mais le port ne garantit aucun ordre.
 *
 * @param trashed médias présents au départ dans la corbeille.
 */
class FakeSystemTrash(
    trashed: Iterable<TrashedMedia> = emptyList(),
) : SystemTrash {
    private val content = LinkedHashMap<MediaId, TrashedMedia>()

    init {
        trashed.forEach { content[it.media.id] = it }
    }

    /** Médias actuellement dans la corbeille, dans l'ordre d'ajout. */
    val media: List<TrashedMedia> get() = content.values.toList()

    /** Ajoute [media] à la corbeille, à vider à [purgeAt] (inconnu si `null`). */
    fun add(
        media: MediaItem,
        purgeAt: Instant? = null,
    ) {
        content[media.id] = TrashedMedia(media, purgeAt)
    }

    /** Retire les médias [ids] de la corbeille ; un identifiant inconnu est ignoré. */
    fun remove(ids: Iterable<MediaId>) {
        ids.forEach { content.remove(it) }
    }

    override suspend fun trashed(ids: Set<MediaId>): List<TrashedMedia> = content.values.filter { it.media.id in ids }
}
