package fr.babeuloula.photoswipe.domain.model

import java.time.LocalDate

/**
 * Vue d'ensemble d'un jour cible (PS-139) : tous ses souvenirs, triés ou non, rangés par année, pour
 * se remémorer la journée et voir les photos proches avant de décider.
 *
 * @property date jour cible.
 * @property years souvenirs de chaque année, de la plus récente à la plus ancienne ; vide sans souvenir.
 */
data class DayOverview(
    val date: LocalDate,
    val years: List<OverviewYear>,
) {
    /** Nombre de souvenirs du jour, toutes années confondues. */
    val memoryCount: Int get() = years.sumOf { it.memories.size }
}

/**
 * Souvenirs d'une année dans la vue d'ensemble d'un jour.
 *
 * @property year année de prise de vue.
 * @property memories souvenirs de cette année, dans l'ordre de la pile.
 */
data class OverviewYear(
    val year: Int,
    val memories: List<OverviewMemory>,
)

/**
 * Un souvenir de la vue d'ensemble et sa décision pour l'année de revue du jour.
 *
 * @property media le média.
 * @property decision décision prise ce jour-là, `null` s'il reste à trier (y compris sur un jour
 * rouvert pour être retrié, ADR 0026).
 * @property favorite vrai s'il est en favori dans la galerie, ou demandé en favori depuis la pile.
 * @property similar place de la photo parmi des photos prises à la suite (PS-140), `null` sinon.
 */
data class OverviewMemory(
    val media: MediaItem,
    val decision: ReviewDecision?,
    val favorite: Boolean,
    val similar: SimilarGroup? = null,
)
