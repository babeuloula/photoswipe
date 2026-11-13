package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.DayMemories
import fr.babeuloula.photoswipe.domain.model.YearMemories
import fr.babeuloula.photoswipe.domain.model.isSorted
import fr.babeuloula.photoswipe.domain.port.MediaSource
import fr.babeuloula.photoswipe.domain.port.ReviewRepository
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate

/**
 * Pour chaque jour de l'année en cours, le nombre de souvenirs et de ceux qui restent à trier
 * (PS-156), pour le calendrier de la page « Progression ».
 *
 * Les souvenirs d'un jour de l'année `Y` sont les médias pris le même jour une année antérieure
 * (SPEC §3.1) : ceux de toute l'année se lisent donc en une seule fois, tous les médias pris avant
 * le 1er janvier de `Y`, puis se rangent par jour avec les règles de [SameDayMatcher] (29 février
 * affiché le 28 les années non bissextiles). Comme [GetReviewDays], la galerie est lue une fois à
 * la collecte et les nombres suivent ensuite les décisions de l'année en direct : un média ajouté
 * n'est pris en compte qu'à la collecte suivante. Avec un accès partiel, seuls les médias
 * accessibles comptent.
 *
 * @param clock année en cours et fuseau de l'appareil.
 */
class GetYearMemories(
    private val sameDayMatcher: SameDayMatcher,
    private val mediaSource: MediaSource,
    private val reviewRepository: ReviewRepository,
    private val clock: Clock,
) {
    /** Souvenirs de chaque jour de l'année en cours. */
    operator fun invoke(): Flow<YearMemories> =
        flow {
            val zone = clock.zone
            val year = LocalDate.now(clock).year
            val start = LocalDate.of(SameDayMatcher.EARLIEST_YEAR, 1, 1).atStartOfDay(zone).toInstant()
            val end = LocalDate.of(year, 1, 1).atStartOfDay(zone).toInstant()
            val byDay =
                mediaSource
                    .mediaTakenIn(listOf(start..<end))
                    .distinctBy { it.id }
                    .groupBy({ sameDayMatcher.reviewDay(it, year, zone) }, { it.id })
            emitAll(
                reviewRepository.observeEntries(setOf(year)).map { entries ->
                    val sorted = entries.filter { it.reviewYear == year && it.isSorted }.mapTo(HashSet()) { it.mediaId }
                    YearMemories(
                        year,
                        byDay.mapValues { (_, ids) -> DayMemories(ids.size, ids.count { it !in sorted }) },
                    )
                },
            )
        }.distinctUntilChanged()
}
