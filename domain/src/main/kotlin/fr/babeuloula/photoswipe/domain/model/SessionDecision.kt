package fr.babeuloula.photoswipe.domain.model

import java.time.LocalDate

/**
 * Décision de la pile d'un jour, telle que l'undo doit pouvoir l'annuler (SPEC §3.4) : sa ligne du
 * journal et le média, à remettre sur la pile en cas d'undo.
 *
 * @property media média décidé.
 * @property record décision enregistrée dans le journal du jour.
 */
data class SessionDecision(
    val media: MediaItem,
    val record: DecisionRecord,
) {
    init {
        require(record.mediaId == media.id) { "record must belong to media ${media.id}: $record" }
    }

    /** Jour cible de la pile où le média a été décidé. */
    val day: LocalDate get() = record.day

    /** Année de revue de la décision (SPEC §3.3). */
    val reviewYear: Int get() = day.year

    /** Entrée écrite par la décision. */
    val made: ReviewEntry get() = record.made

    /** Entrée remplacée par la décision, `null` si le média était à trier. */
    val previous: ReviewEntry? get() = record.previous
}
