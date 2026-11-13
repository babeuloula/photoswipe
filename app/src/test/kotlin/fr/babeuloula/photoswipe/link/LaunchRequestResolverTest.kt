package fr.babeuloula.photoswipe.link

import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.KEPT
import fr.babeuloula.photoswipe.domain.model.ReviewEntry
import fr.babeuloula.photoswipe.domain.port.MediaSource
import fr.babeuloula.photoswipe.domain.service.RecentYearsFirstOrdering
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import fr.babeuloula.photoswipe.domain.usecase.GetDayOverview
import fr.babeuloula.photoswipe.ui.navigation.DayOverviewRoute
import fr.babeuloula.photoswipe.ui.navigation.DeckRoute
import fr.babeuloula.photoswipe.ui.navigation.HomeRoute
import fr.babeuloula.photoswipe.ui.navigation.SummaryRoute
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime.NOON

class LaunchRequestResolverTest {
    private val today = LocalDate.of(2026, 7, 15)

    @Test
    fun `opens the deck of today when a memory is left to sort`() =
        runTest {
            val resolver = resolver(listOf(takenOn(1, LocalDate.of(2024, 7, 15))))

            assertEquals(DeckRoute.of(today), resolver.routeFor(LaunchRequest.TodayDeck))
        }

    @Test
    fun `opens the home screen when nothing is left to sort today`() =
        runTest {
            val media = listOf(takenOn(1, LocalDate.of(2024, 7, 15)))
            val sorted = listOf(aReviewEntry(mediaId = 1, reviewYear = 2026, decision = KEPT))

            assertEquals(HomeRoute, resolver(media, sorted).routeFor(LaunchRequest.TodayDeck))
            assertEquals(HomeRoute, resolver().routeFor(LaunchRequest.TodayDeck))
        }

    @Test
    fun `opens the overview of a day with memories, sorted or not`() =
        runTest {
            val day = LocalDate.of(2026, 3, 2)
            val media = listOf(takenOn(1, LocalDate.of(2020, 3, 2)))
            val sorted = listOf(aReviewEntry(mediaId = 1, reviewYear = 2026, decision = KEPT))

            assertEquals(DayOverviewRoute.of(day), resolver(media, sorted).routeFor(LaunchRequest.DayOverview(day)))
        }

    @Test
    fun `opens the home screen for a day without memories`() =
        runTest {
            val resolver = resolver(listOf(takenOn(1, LocalDate.of(2020, 3, 2))))

            assertEquals(HomeRoute, resolver.routeFor(LaunchRequest.DayOverview(LocalDate.of(2026, 3, 3))))
        }

    @Test
    fun `opens the home screen when the gallery cannot be read`() =
        runTest {
            val resolver = resolver(UnreadableGallery)

            assertEquals(HomeRoute, resolver.routeFor(LaunchRequest.TodayDeck))
            assertEquals(HomeRoute, resolver.routeFor(LaunchRequest.DayOverview(today)))
        }

    @Test
    fun `always opens the summary, the deck of the reminder and the home screen`() =
        runTest {
            val resolver = resolver()

            assertEquals(SummaryRoute, resolver.routeFor(LaunchRequest.Summary))
            assertEquals(DeckRoute.of(today), resolver.routeFor(LaunchRequest.Deck(today)))
            assertEquals(HomeRoute, resolver.routeFor(LaunchRequest.Home))
        }

    private fun resolver(
        media: List<MediaItem> = emptyList(),
        entries: List<ReviewEntry> = emptyList(),
    ): LaunchRequestResolver = resolver(FakeMediaSource(media), entries)

    private fun resolver(
        mediaSource: MediaSource,
        entries: List<ReviewEntry> = emptyList(),
    ): LaunchRequestResolver {
        val clock = FixedClock.at(today.atTime(NOON))
        val getDayOverview =
            GetDayOverview(
                sameDayMatcher = SameDayMatcher(),
                mediaSource = mediaSource,
                reviewRepository = FakeReviewRepository(entries),
                deckOrdering = RecentYearsFirstOrdering(),
                clock = clock,
            )
        return LaunchRequestResolver(getDayOverview, clock)
    }

    /** Galerie dont l'accès a été retiré. */
    private object UnreadableGallery : MediaSource {
        override suspend fun mediaTakenIn(ranges: List<OpenEndRange<Instant>>): List<MediaItem> =
            throw SecurityException("no access")

        override suspend fun mediaByIds(ids: Set<MediaId>): List<MediaItem> = throw SecurityException("no access")
    }

    private fun takenOn(
        id: Long,
        date: LocalDate,
    ): MediaItem = aMediaItem(id = id, takenAt = date.atTime(NOON).atZone(FixedClock.PARIS).toInstant())
}
