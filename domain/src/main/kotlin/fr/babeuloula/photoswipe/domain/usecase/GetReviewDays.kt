package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.ReviewDay
import fr.babeuloula.photoswipe.domain.model.ReviewEntry
import fr.babeuloula.photoswipe.domain.port.MediaSource
import fr.babeuloula.photoswipe.domain.port.ReviewRepository
import fr.babeuloula.photoswipe.domain.service.ReviewWindowProvider
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/**
 * Jours de la fenêtre de revue avec, pour chacun, le nombre de médias qui restent à trier
 * (SPEC §3.3 et §4, accueil), quelques vignettes et les années des souvenirs (ADR 0025).
 *
 * Un média est à trier pour le jour `D` s'il en est un souvenir (SPEC §3.1) et qu'il n'a aucune
 * entrée pour l'année de revue de `D`, quelle que soit la décision (SPEC §3.3). Une décision d'une
 * autre année ne compte pas : un média gardé l'an dernier redevient à trier.
 *
 * Le flux suit les décisions : chaque écriture dans [ReviewRepository] recalcule les nombres, et
 * une liste identique n'est pas réémise. La fenêtre et les médias de chaque jour sont lus une fois,
 * à la collecte : un média ajouté à la galerie ou le passage à minuit ne sont pris en compte qu'à
 * la collecte suivante (l'accueil recollecte en revenant au premier plan, ADR 0008).
 *
 * @param clock la même horloge que celle de [windowProvider] : son fuseau sert à apparier les médias.
 */
class GetReviewDays(
    private val windowProvider: ReviewWindowProvider,
    private val sameDayMatcher: SameDayMatcher,
    private val mediaSource: MediaSource,
    private val reviewRepository: ReviewRepository,
    private val clock: Clock,
) {
    /** Jours de la fenêtre, d'aujourd'hui au plus ancien, avec leur nombre de médias à trier. */
    operator fun invoke(): Flow<List<ReviewDay>> =
        flow {
            val zone = clock.zone
            val mediaByDay =
                windowProvider.days().associateWith { day ->
                    mediaSource.mediaTakenIn(sameDayMatcher.rangesFor(day, zone)).distinctBy { it.id }
                }
            val reviewYears = mediaByDay.keys.mapTo(HashSet()) { it.year }
            emitAll(
                reviewRepository.observeEntries(reviewYears).map { entries ->
                    reviewDays(mediaByDay, entries, zone)
                },
            )
        }.distinctUntilChanged()

    private fun reviewDays(
        mediaByDay: Map<LocalDate, List<MediaItem>>,
        entries: List<ReviewEntry>,
        zone: ZoneId,
    ): List<ReviewDay> {
        val treatedByYear = entries.groupBy({ it.reviewYear }, { it.mediaId }).mapValues { it.value.toSet() }
        return mediaByDay.map { (day, media) ->
            val treated = treatedByYear[day.year].orEmpty()
            val remaining = media.filter { it.id !in treated }
            val years = media.map { it.takenAt.atZone(zone).year }
            ReviewDay(
                date = day,
                remainingCount = remaining.size,
                memoryCount = media.size,
                previews = previews(remaining.ifEmpty { media }, zone),
                years = if (years.isEmpty()) null else years.min()..years.max(),
            )
        }
    }

    /** Une vignette par année, de la plus récente à la plus ancienne, la première photo de chaque année. */
    private fun previews(
        media: List<MediaItem>,
        zone: ZoneId,
    ): List<MediaItem> =
        media
            .sortedBy { it.takenAt }
            .groupBy { it.takenAt.atZone(zone).year }
            .toSortedMap(compareByDescending { it })
            .values
            .map { it.first() }
            .take(ReviewDay.PREVIEW_COUNT)
}
