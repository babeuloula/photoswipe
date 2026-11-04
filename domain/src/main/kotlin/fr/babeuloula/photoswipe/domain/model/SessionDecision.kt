package fr.babeuloula.photoswipe.domain.model

import java.time.LocalDate

/**
 * Décision prise pendant une session de tri, telle que l'undo doit pouvoir l'annuler (SPEC §3.4).
 *
 * @property media média décidé, à remettre sur la pile en cas d'undo.
 * @property day jour cible de la pile où il a été décidé ; son année est l'année de revue.
 * @property previous entrée que la décision a remplacée pour ce couple (média, année de revue),
 * `null` si le média était à trier. L'undo la restaure, ou supprime l'entrée si elle est `null`.
 */
data class SessionDecision(
    val media: MediaItem,
    val day: LocalDate,
    val previous: ReviewEntry? = null,
) {
    init {
        require(previous == null || (previous.mediaId == media.id && previous.reviewYear == day.year)) {
            "previous entry must belong to media ${media.id} and review year ${day.year}: $previous"
        }
    }

    /** Année de revue de la décision (SPEC §3.3). */
    val reviewYear: Int get() = day.year
}
