package fr.babeuloula.photoswipe.domain.model

import java.time.LocalDate

/**
 * Souvenirs d'un jour de l'année, pour le calendrier (PS-156).
 *
 * @property memoryCount souvenirs du jour, toutes années passées confondues.
 * @property remainingCount ceux qui restent à trier cette année.
 */
data class DayMemories(
    val memoryCount: Int,
    val remainingCount: Int,
) {
    /** Vrai si tous les souvenirs du jour sont triés. */
    val isSorted: Boolean get() = remainingCount == 0
}

/**
 * Souvenirs de chaque jour d'une année de revue (PS-156) : la base du calendrier de l'année.
 *
 * @property year année de revue.
 * @property days souvenirs de chaque jour qui en a ; un jour absent n'a aucun souvenir.
 */
data class YearMemories(
    val year: Int,
    val days: Map<LocalDate, DayMemories>,
) {
    /** Souvenirs du jour [day], `null` s'il n'en a aucun. */
    operator fun get(day: LocalDate): DayMemories? = days[day]
}
