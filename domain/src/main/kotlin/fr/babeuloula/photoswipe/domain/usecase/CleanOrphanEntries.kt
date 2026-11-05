package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.MediaAccess
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.port.DecisionJournal
import fr.babeuloula.photoswipe.domain.port.MediaSource
import fr.babeuloula.photoswipe.domain.port.ReviewRepository

/**
 * Supprime les entrées des médias qui n'existent plus dans la galerie (SPEC §6 et §14), au
 * démarrage et après chaque confirmation.
 *
 * Un média a disparu s'il n'est plus renvoyé par [MediaSource.mediaByIds] : supprimé hors de
 * l'app, ou envoyé à la corbeille système (après une confirmation `TRASHED`, ou depuis la galerie).
 * Toutes ses entrées sont supprimées, quelle que soit l'année de revue, ainsi que ses décisions
 * du journal (ADR 0019).
 *
 * Cette preuve ne vaut qu'avec un accès complet : en accès partiel, un média absent peut seulement
 * être hors de la sélection, et sans accès la galerie paraît vide. Dans ces deux cas, rien n'est
 * supprimé, pour ne pas effacer de vraies décisions (ADR 0007). Idempotent : un second passage ne
 * trouve plus d'orphelin.
 */
class CleanOrphanEntries(
    private val reviewRepository: ReviewRepository,
    private val mediaSource: MediaSource,
    private val journal: DecisionJournal,
) {
    /**
     * Supprime les entrées orphelines si [access] est complet.
     *
     * @return nombre de médias dont les entrées ont été supprimées.
     */
    suspend operator fun invoke(access: MediaAccess): Int {
        val orphans = if (access == MediaAccess.FULL) orphanIds() else emptySet()
        if (orphans.isNotEmpty()) {
            reviewRepository.deleteAllFor(orphans)
            journal.forget(orphans)
        }
        return orphans.size
    }

    /** Médias qui ont une entrée mais ne sont plus dans la galerie. */
    private suspend fun orphanIds(): Set<MediaId> {
        val known = reviewRepository.knownMediaIds()
        if (known.isEmpty()) return emptySet()

        val present = mediaSource.mediaByIds(known).mapTo(HashSet()) { it.id }
        return known - present
    }
}
