package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.DayOverview
import fr.babeuloula.photoswipe.domain.model.OverviewMemory
import fr.babeuloula.photoswipe.domain.model.OverviewYear
import fr.babeuloula.photoswipe.domain.model.isSorted
import fr.babeuloula.photoswipe.domain.port.MediaSource
import fr.babeuloula.photoswipe.domain.port.ReviewRepository
import fr.babeuloula.photoswipe.domain.service.DeckOrdering
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import java.time.Clock
import java.time.LocalDate

/**
 * Vue d'ensemble d'un jour (PS-139) : tous ses souvenirs (SPEC §3.1), triés ou non, avec la décision
 * prise pour son année de revue, rangés par année dans l'ordre de la pile ([DeckOrdering]) : de la
 * plus récente à la plus ancienne, et dans l'ordre de prise de vue au sein d'une année.
 *
 * Photo instantanée (`suspend`) : l'écran la relit à son retour au premier plan.
 *
 * @param clock horloge de l'appareil : son fuseau sert à apparier, ordonner et ranger les médias.
 */
class GetDayOverview(
    private val sameDayMatcher: SameDayMatcher,
    private val mediaSource: MediaSource,
    private val reviewRepository: ReviewRepository,
    private val deckOrdering: DeckOrdering,
    private val clock: Clock,
) {
    /** Souvenirs du jour [day], rangés par année, avec leur décision. */
    suspend operator fun invoke(day: LocalDate): DayOverview {
        val zone = clock.zone
        val memories = mediaSource.mediaTakenIn(sameDayMatcher.rangesFor(day, zone)).distinctBy { it.id }
        if (memories.isEmpty()) return DayOverview(day, emptyList())

        val entries =
            reviewRepository
                .entriesFor(day.year, memories.mapTo(HashSet()) { it.id })
                .associateBy { it.mediaId }
        val years =
            deckOrdering
                .order(memories, zone)
                .map { media ->
                    val entry = entries[media.id]?.takeIf { it.isSorted }
                    OverviewMemory(media, entry?.decision, media.isFavorite || entry?.favoritePending == true)
                }.groupBy {
                    it.media.takenAt
                        .atZone(zone)
                        .year
                }.map { (year, inYear) -> OverviewYear(year, inYear) }
        return DayOverview(day, years)
    }
}
