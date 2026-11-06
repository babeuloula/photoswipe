package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.isSorted
import fr.babeuloula.photoswipe.domain.port.MediaSource
import fr.babeuloula.photoswipe.domain.port.ReviewRepository
import fr.babeuloula.photoswipe.domain.service.DeckOrdering
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import java.time.Clock
import java.time.LocalDate

/**
 * Paquet des médias à trier pour un jour cible, dans l'ordre de [DeckOrdering] (SPEC §3.2).
 *
 * Même règle que `GetReviewDays` : un média est à trier pour le jour `D` s'il en est un souvenir
 * (SPEC §3.1) et qu'il n'a aucune entrée pour l'année de revue de `D`, quelle que soit la décision
 * (SPEC §3.3), ou que son jour a été rouvert pour être retrié (ADR 0026). La taille du paquet est donc le nombre affiché sur l'accueil pour ce jour.
 *
 * Le paquet est une photo instantanée (`suspend`), pas un flux : la pile de swipe le charge une
 * fois, puis retire elle-même chaque carte décidée. Un flux réémettrait à chaque décision et ferait
 * changer la pile sous le doigt de l'utilisateur (ADR 0009).
 *
 * @param clock horloge de l'appareil : son fuseau sert à apparier et à ordonner les médias.
 */
class GetDeckForDay(
    private val sameDayMatcher: SameDayMatcher,
    private val mediaSource: MediaSource,
    private val reviewRepository: ReviewRepository,
    private val deckOrdering: DeckOrdering,
    private val clock: Clock,
) {
    /** Médias encore à trier pour le jour [day], dans l'ordre où ils seront proposés. */
    suspend operator fun invoke(day: LocalDate): List<MediaItem> {
        val zone = clock.zone
        val memories = mediaSource.mediaTakenIn(sameDayMatcher.rangesFor(day, zone))
        if (memories.isEmpty()) return emptyList()

        val memoryIds = memories.mapTo(HashSet()) { it.id }
        val treated =
            reviewRepository
                .entriesFor(day.year, memoryIds)
                .filter { it.isSorted }
                .mapTo(HashSet()) { it.mediaId }
        return deckOrdering.order(memories.filterNot { it.id in treated }, zone)
    }
}
