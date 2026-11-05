package fr.babeuloula.photoswipe.domain.model

import java.time.LocalDate

/**
 * Décision du journal d'un jour : ce que l'undo doit pouvoir annuler, même après la fermeture de
 * l'app (SPEC §3.4, ADR 0019).
 *
 * @property id rang dans le journal, croissant : il ordonne les décisions d'un jour (l'instant de
 * la décision peut être le même pour deux décisions).
 * @property day jour cible de la pile où la décision a été prise ; son année est l'année de revue.
 * @property made entrée écrite par la décision (garder, marquer, favori), telle qu'au moment du swipe.
 * @property previous entrée que la décision a remplacée pour ce couple (média, année de revue),
 * `null` si le média était à trier. L'undo la restaure, ou supprime l'entrée si elle est `null`.
 */
data class DecisionRecord(
    val id: Long,
    val day: LocalDate,
    val made: ReviewEntry,
    val previous: ReviewEntry? = null,
) {
    init {
        require(made.reviewYear == day.year) { "made entry must belong to review year ${day.year}: $made" }
        require(previous == null || (previous.mediaId == made.mediaId && previous.reviewYear == made.reviewYear)) {
            "previous entry must belong to media ${made.mediaId} and review year ${day.year}: $previous"
        }
    }

    /** Média décidé. */
    val mediaId: MediaId get() = made.mediaId
}
