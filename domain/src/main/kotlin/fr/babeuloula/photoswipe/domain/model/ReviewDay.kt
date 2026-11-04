package fr.babeuloula.photoswipe.domain.model

import java.time.LocalDate

/**
 * Jour cible de la fenêtre de revue (aujourd'hui ou l'un des 6 jours précédents, SPEC §3.1).
 *
 * @property date jour cible, dans le fuseau de l'appareil.
 * @property remainingCount nombre de médias qui restent à trier pour ce jour.
 */
data class ReviewDay(
    val date: LocalDate,
    val remainingCount: Int,
) {
    init {
        require(remainingCount >= 0) { "remainingCount must not be negative: $remainingCount" }
    }

    /** Année de revue à laquelle les décisions de ce jour sont rattachées (SPEC §3.3). */
    val reviewYear: Int get() = date.year
}
