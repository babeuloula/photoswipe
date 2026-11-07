package fr.babeuloula.photoswipe.domain.model

import java.time.LocalDate

/**
 * Invitation unique à noter l'application et à soutenir son développeur (PS-121, ADR 0029) :
 * proposée une fois que la pile d'aujourd'hui a été entièrement triée [SORTED_DAYS_BEFORE_ASKING]
 * jours différents, puis plus jamais.
 *
 * @property sortedDays jours différents où la pile du jour a été entièrement triée.
 * @property lastSortedDay dernier de ces jours, pour ne pas compter deux fois le même.
 * @property asked vrai une fois l'invitation montrée et fermée, quel que soit le choix.
 */
data class RatingInvitation(
    val sortedDays: Int = 0,
    val lastSortedDay: LocalDate? = null,
    val asked: Boolean = false,
) {
    /** Vrai s'il est temps de montrer l'invitation. */
    val due: Boolean get() = !asked && sortedDays >= SORTED_DAYS_BEFORE_ASKING

    /** Invitation après avoir entièrement trié la pile du jour [day] ; un même jour ne compte qu'une fois. */
    fun afterSorting(day: LocalDate): RatingInvitation =
        if (day == lastSortedDay) this else copy(sortedDays = sortedDays + 1, lastSortedDay = day)

    companion object {
        /** Jours différents à trier entièrement avant l'invitation. */
        const val SORTED_DAYS_BEFORE_ASKING: Int = 3

        /** Rien de trié, rien de proposé. */
        val DEFAULT: RatingInvitation = RatingInvitation()
    }
}
