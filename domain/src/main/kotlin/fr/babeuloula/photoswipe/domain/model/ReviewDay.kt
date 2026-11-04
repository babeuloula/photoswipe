package fr.babeuloula.photoswipe.domain.model

import java.time.LocalDate

/**
 * Jour cible de la fenêtre de revue (aujourd'hui ou l'un des 6 jours précédents, SPEC §3.1).
 *
 * @property date jour cible, dans le fuseau de l'appareil.
 * @property remainingCount nombre de médias qui restent à trier pour ce jour.
 * @property memoryCount nombre de souvenirs de ce jour, triés ou non : 0 signifie qu'aucun média
 * n'a été trouvé, et non que tout est trié. Par défaut, égal à [remainingCount] (rien de trié).
 */
data class ReviewDay(
    val date: LocalDate,
    val remainingCount: Int,
    val memoryCount: Int = remainingCount,
) {
    init {
        require(remainingCount >= 0) { "remainingCount must not be negative: $remainingCount" }
        require(memoryCount >= remainingCount) {
            "memoryCount ($memoryCount) must be at least remainingCount ($remainingCount)"
        }
    }

    /** Année de revue à laquelle les décisions de ce jour sont rattachées (SPEC §3.3). */
    val reviewYear: Int get() = date.year
}
