package fr.babeuloula.photoswipe.domain.fixtures

import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.port.MediaSource
import java.time.Instant

/**
 * [MediaSource] en mémoire : une galerie que le test remplit et modifie à la main.
 *
 * Elle respecte le contrat du port : plages ouvertes à droite, chaque média renvoyé une seule
 * fois même si plusieurs plages le couvrent. Retirer un média ([remove]) simule sa suppression,
 * sa mise à la corbeille ou sa sortie de la sélection en accès partiel : il n'est plus renvoyé.
 * Les résultats suivent l'ordre d'ajout, mais le port ne garantit aucun ordre.
 *
 * @param media médias présents au départ.
 */
class FakeMediaSource(
    media: Iterable<MediaItem> = emptyList(),
) : MediaSource {
    private val gallery = LinkedHashMap<MediaId, MediaItem>()

    init {
        add(media)
    }

    /** Médias actuellement présents, dans l'ordre d'ajout. */
    val media: List<MediaItem> get() = gallery.values.toList()

    /** Ajoute [media], en remplaçant ceux qui ont le même identifiant. */
    fun add(vararg media: MediaItem) {
        add(media.asList())
    }

    /** Ajoute [media], en remplaçant ceux qui ont le même identifiant. */
    fun add(media: Iterable<MediaItem>) {
        media.forEach { gallery[it.id] = it }
    }

    /** Retire le média [id] : il n'est plus renvoyé. Un identifiant inconnu est ignoré. */
    fun remove(id: MediaId) {
        gallery.remove(id)
    }

    /** Retire les médias [ids] : ils ne sont plus renvoyés. Un identifiant inconnu est ignoré. */
    fun remove(ids: Iterable<MediaId>) {
        ids.forEach { gallery.remove(it) }
    }

    override suspend fun mediaTakenIn(ranges: List<OpenEndRange<Instant>>): List<MediaItem> =
        gallery.values.filter { item -> ranges.any { item.takenAt in it } }

    override suspend fun mediaByIds(ids: Set<MediaId>): List<MediaItem> = gallery.values.filter { it.id in ids }
}
