package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.ReviewDay
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.KEPT
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.MARKED_FOR_DELETION
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.TRASHED
import fr.babeuloula.photoswipe.domain.model.ReviewEntry
import fr.babeuloula.photoswipe.domain.service.ReviewWindowProvider
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime.NOON

class GetReviewDaysTest {
    @Test
    fun `lists every day of the window with nothing to sort when the gallery is empty`() =
        runTest {
            val days = getReviewDays(today = LocalDate.of(2026, 7, 15))().first()

            assertEquals(
                (15 downTo 9).map { ReviewDay(LocalDate.of(2026, 7, it), remainingCount = 0) },
                days,
            )
        }

    @Test
    fun `counts the memories of each day that have no decision for the review year`() =
        runTest {
            val media =
                listOf(
                    takenOn(id = 1, LocalDate.of(2025, 7, 15)),
                    takenOn(id = 2, LocalDate.of(2024, 7, 15)),
                    takenOn(id = 3, LocalDate.of(2020, 7, 15)),
                    takenOn(id = 4, LocalDate.of(2026, 7, 15)),
                    takenOn(id = 5, LocalDate.of(2023, 7, 14)),
                )
            val entries =
                listOf(
                    aReviewEntry(mediaId = 1, reviewYear = 2026, decision = KEPT),
                    aReviewEntry(mediaId = 2, reviewYear = 2026, decision = MARKED_FOR_DELETION),
                )

            val days = getReviewDays(today = LocalDate.of(2026, 7, 15), media, entries)().first()

            assertEquals(ReviewDay(LocalDate.of(2026, 7, 15), remainingCount = 1, memoryCount = 3), days[0])
            assertEquals(ReviewDay(LocalDate.of(2026, 7, 14), remainingCount = 1), days[1])
        }

    @Test
    fun `does not count a media sent to the trash for the review year`() =
        runTest {
            val media = listOf(takenOn(id = 1, LocalDate.of(2025, 7, 15)))
            val entries = listOf(aReviewEntry(mediaId = 1, reviewYear = 2026, decision = TRASHED))

            val days = getReviewDays(today = LocalDate.of(2026, 7, 15), media, entries)().first()

            assertEquals(0, days[0].remainingCount)
        }

    @Test
    fun `counts again a media kept the previous year`() =
        runTest {
            val media = listOf(takenOn(id = 1, LocalDate.of(2025, 7, 15)))
            val entries = listOf(aReviewEntry(mediaId = 1, reviewYear = 2026, decision = KEPT))

            val days = getReviewDays(today = LocalDate.of(2027, 7, 15), media, entries)().first()

            assertEquals(ReviewDay(LocalDate.of(2027, 7, 15), remainingCount = 1), days[0])
        }

    @Test
    fun `uses the review year of each day when the window spans two years`() =
        runTest {
            val media =
                listOf(
                    takenOn(id = 1, LocalDate.of(2025, 12, 28)),
                    takenOn(id = 2, LocalDate.of(2025, 1, 2)),
                )
            val entries =
                listOf(
                    aReviewEntry(mediaId = 1, reviewYear = 2026),
                    aReviewEntry(mediaId = 2, reviewYear = 2026),
                )

            val days = getReviewDays(today = LocalDate.of(2027, 1, 2), media, entries)().first()

            assertEquals(ReviewDay(LocalDate.of(2027, 1, 2), remainingCount = 1), days.first())
            assertEquals(ReviewDay(LocalDate.of(2026, 12, 28), remainingCount = 0, memoryCount = 1), days[5])
        }

    @Test
    fun `counts February 29 memories on February 28 of a non-leap year`() =
        runTest {
            val media =
                listOf(
                    takenOn(id = 1, LocalDate.of(2024, 2, 29)),
                    takenOn(id = 2, LocalDate.of(2025, 2, 28)),
                )

            val days = getReviewDays(today = LocalDate.of(2027, 2, 28), media)().first()

            assertEquals(ReviewDay(LocalDate.of(2027, 2, 28), remainingCount = 2), days[0])
        }

    @Test
    fun `updates the counts after each decision`() =
        runTest {
            val media = listOf(takenOn(id = 1, LocalDate.of(2025, 7, 15)), takenOn(id = 2, LocalDate.of(2024, 7, 15)))
            val repository = FakeReviewRepository()
            val emissions = collect(getReviewDays(LocalDate.of(2026, 7, 15), media, repository = repository))

            repository.save(aReviewEntry(mediaId = 1, reviewYear = 2026))
            repository.delete(MediaId(1), 2026)

            assertEquals(listOf(2, 1, 2), emissions.map { it[0].remainingCount })
        }

    @Test
    fun `does not emit again when a write leaves the counts unchanged`() =
        runTest {
            val media = listOf(takenOn(id = 1, LocalDate.of(2025, 7, 15)))
            val repository = FakeReviewRepository()
            val emissions = collect(getReviewDays(LocalDate.of(2026, 7, 15), media, repository = repository))

            repository.save(aReviewEntry(mediaId = 99, reviewYear = 2026))
            repository.save(aReviewEntry(mediaId = 1, reviewYear = 2025))

            assertEquals(1, emissions.size)
        }

    private fun getReviewDays(
        today: LocalDate,
        media: List<MediaItem> = emptyList(),
        entries: List<ReviewEntry> = emptyList(),
        repository: FakeReviewRepository = FakeReviewRepository(entries),
    ): GetReviewDays {
        val clock = FixedClock.at(today.atTime(NOON))
        return GetReviewDays(
            windowProvider = ReviewWindowProvider(clock),
            sameDayMatcher = SameDayMatcher(),
            mediaSource = FakeMediaSource(media),
            reviewRepository = repository,
            clock = clock,
        )
    }

    /** Collecte le flux en arrière-plan ; la liste reçoit chaque émission dès qu'elle a lieu. */
    private fun TestScope.collect(useCase: GetReviewDays): List<List<ReviewDay>> {
        val emissions = mutableListOf<List<ReviewDay>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { useCase().toList(emissions) }
        return emissions
    }

    private fun takenOn(
        id: Long,
        date: LocalDate,
    ): MediaItem = aMediaItem(id = id, takenAt = LocalDateTime.of(date, NOON).atZone(FixedClock.PARIS).toInstant())
}
