package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakeDeletionHistory
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.FakeSettingsRepository
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.fixtures.aConfirmedDeletion
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.CatchUpWindow
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
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
            assertTrue(days.all { it.previews.isEmpty() && it.years == null })
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

            assertEquals(
                ReviewDay(LocalDate.of(2026, 7, 15), remainingCount = 1, memoryCount = 3).counts(),
                days[0].counts(),
            )
            assertEquals(ReviewDay(LocalDate.of(2026, 7, 14), remainingCount = 1).counts(), days[1].counts())
        }

    @Test
    fun `previews one memory to sort per year, the most recent year first, with the years of the day`() =
        runTest {
            val media =
                listOf(
                    takenOn(id = 1, LocalDate.of(2016, 7, 15)),
                    takenOn(id = 2, LocalDate.of(2023, 7, 15)),
                    takenOn(id = 3, LocalDate.of(2023, 7, 15)),
                    takenOn(id = 4, LocalDate.of(2025, 7, 15)),
                    takenOn(id = 5, LocalDate.of(2019, 7, 15)),
                    takenOn(id = 6, LocalDate.of(2020, 7, 15)),
                )
            val entries = listOf(aReviewEntry(mediaId = 4, reviewYear = 2026, decision = KEPT))

            val today = getReviewDays(today = LocalDate.of(2026, 7, 15), media, entries)().first()[0]

            assertEquals(listOf(MediaId(2), MediaId(6), MediaId(5)), today.previews.map { it.id })
            assertEquals(2016..2025, today.years)
            assertEquals(3, today.hiddenCount)
        }

    @Test
    fun `previews every memory of the day when they fit in the strip, even from the same year`() =
        runTest {
            val media =
                listOf(
                    takenOn(id = 1, LocalDate.of(2025, 7, 15)),
                    takenOn(id = 2, LocalDate.of(2025, 7, 15)),
                    takenOn(id = 3, LocalDate.of(2020, 7, 15)),
                    takenOn(id = 4, LocalDate.of(2025, 7, 15)),
                )

            val today = getReviewDays(today = LocalDate.of(2026, 7, 15), media)().first()[0]

            assertEquals(4, today.previews.size)
            assertEquals(listOf(1L, 3L), today.previews.take(2).map { it.id.value })
            assertEquals(0, today.hiddenCount)
        }

    @Test
    fun `previews three memories and counts the others from five memories`() =
        runTest {
            val media = (1L..5L).map { takenOn(id = it, LocalDate.of(2020 + it.toInt(), 7, 15)) }

            val today = getReviewDays(today = LocalDate.of(2026, 7, 15), media)().first()[0]

            assertEquals(listOf(5L, 4L, 3L), today.previews.map { it.id.value })
            assertEquals(2, today.hiddenCount)
        }

    @Test
    fun `previews the memories to sort first, then the sorted ones, when they all fit`() =
        runTest {
            val media =
                listOf(
                    takenOn(id = 1, LocalDate.of(2025, 7, 15)),
                    takenOn(id = 2, LocalDate.of(2023, 7, 15)),
                    takenOn(id = 3, LocalDate.of(2021, 7, 15)),
                )
            val entries = listOf(aReviewEntry(mediaId = 1, reviewYear = 2026, decision = KEPT))

            val today = getReviewDays(today = LocalDate.of(2026, 7, 15), media, entries)().first()[0]

            assertEquals(listOf(2L, 3L, 1L), today.previews.map { it.id.value })
            assertEquals(0, today.hiddenCount)
        }

    @Test
    fun `previews the sorted memories once the whole day is sorted`() =
        runTest {
            val media = listOf(takenOn(id = 1, LocalDate.of(2025, 7, 15)))
            val entries = listOf(aReviewEntry(mediaId = 1, reviewYear = 2026, decision = KEPT))

            val today = getReviewDays(today = LocalDate.of(2026, 7, 15), media, entries)().first()[0]

            assertEquals(listOf(MediaId(1)), today.previews.map { it.id })
            assertEquals(0, today.hiddenCount)
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

            assertEquals(ReviewDay(LocalDate.of(2027, 7, 15), remainingCount = 1).counts(), days[0].counts())
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

            assertEquals(ReviewDay(LocalDate.of(2027, 1, 2), remainingCount = 1).counts(), days.first().counts())
            assertEquals(
                ReviewDay(LocalDate.of(2026, 12, 28), remainingCount = 0, memoryCount = 1).counts(),
                days[5].counts(),
            )
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

            assertEquals(ReviewDay(LocalDate.of(2027, 2, 28), remainingCount = 2).counts(), days[0].counts())
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

    @Test
    fun `keeps a trace of what was deleted on each day, even once the day has no memory left`() =
        runTest {
            val media = listOf(takenOn(id = 1, LocalDate.of(2025, 7, 14)))
            val history =
                FakeDeletionHistory(
                    listOf(
                        aConfirmedDeletion(mediaId = 2, day = LocalDate.of(2026, 7, 15), sizeBytes = 2_400_000),
                        aConfirmedDeletion(mediaId = 2, day = LocalDate.of(2026, 7, 15), sizeBytes = 2_400_000),
                        aConfirmedDeletion(mediaId = 3, day = LocalDate.of(2026, 7, 14), sizeBytes = 1_000),
                        aConfirmedDeletion(mediaId = 4, day = null),
                    ),
                )

            val days = getReviewDays(LocalDate.of(2026, 7, 15), media, history = history)().first()

            assertEquals(1, days[0].deletedCount)
            assertEquals(2_400_000L, days[0].freedBytes)
            assertTrue(days[0].isEmptied)
            assertEquals(1, days[1].deletedCount)
            assertEquals(1, days[1].memoryCount)
            assertEquals(false, days[1].isEmptied)
            assertTrue(days.drop(2).all { it.deletedCount == 0 })
        }

    @Test
    fun `follows the deletions confirmed while the flow is collected`() =
        runTest {
            val history = FakeDeletionHistory()
            val emissions = collect(getReviewDays(LocalDate.of(2026, 7, 15), history = history))

            history.record(listOf(aConfirmedDeletion(day = LocalDate.of(2026, 7, 13))))

            assertEquals(1, emissions.last()[2].deletedCount)
        }

    @ParameterizedTest
    @EnumSource(CatchUpWindow::class)
    fun `lists as many days as the chosen catch-up window, with the memories of the oldest one`(window: CatchUpWindow) =
        runTest {
            val today = LocalDate.of(2026, 7, 15)
            val oldest = today.minusDays(window.days - 1L)
            val media =
                listOf(takenOn(id = 1, oldest.minusYears(2)), takenOn(id = 2, oldest.minusDays(1).minusYears(2)))
            val settings = FakeSettingsRepository(catchUpWindow = window)

            val days = getReviewDays(today, media, settings = settings)().first()

            assertEquals(window.days, days.size)
            assertEquals(oldest, days.last().date)
            assertEquals(1, days.last().remainingCount)
            assertEquals(1, days.sumOf { it.remainingCount })
        }

    @Test
    fun `reads the gallery again on the new window when another catch-up window is chosen`() =
        runTest {
            val today = LocalDate.of(2026, 7, 15)
            val settings = FakeSettingsRepository()
            val media = listOf(takenOn(id = 1, LocalDate.of(2024, 6, 20)))
            val emissions = collect(getReviewDays(today, media, settings = settings))

            settings.saveCatchUpWindow(CatchUpWindow.MONTH)

            assertEquals(listOf(7, 30), emissions.map { it.size })
            assertEquals(0, emissions.first().sumOf { it.remainingCount })
            assertEquals(1, emissions.last().sumOf { it.remainingCount })
        }

    /**
     * Mesure grossière (PS-169) : une fenêtre de 30 jours sur une galerie de 30 000 médias en mémoire
     * (300 par jour de l'année sur 10 ans, dont un tiers déjà triés) ; le temps est affiché, et ne
     * doit pas dépasser quelques secondes même sur une machine de CI lente. Il ne mesure que le
     * calcul du domaine, pas la lecture de MediaStore ni de Room.
     */
    @Test
    fun `computes a thirty-day window over a large gallery in a reasonable time`() =
        runTest {
            val today = LocalDate.of(2026, 7, 15)
            val media =
                (0 until LARGE_GALLERY_SIZE).map { index ->
                    takenOn(id = index + 1L, today.minusYears(1L + index % 10).minusDays((index / 10 % 365).toLong()))
                }
            val entries =
                media
                    .filterIndexed {
                        index,
                        _,
                        ->
                        index % 3 == 0
                    }.map { aReviewEntry(mediaId = it.id.value, reviewYear = 2026) }
            val useCase =
                getReviewDays(
                    today,
                    media,
                    entries,
                    settings = FakeSettingsRepository(catchUpWindow = CatchUpWindow.MONTH),
                )

            val start = System.nanoTime()
            val days = useCase().first()
            val elapsedMillis = (System.nanoTime() - start) / NANOS_PER_MILLI
            println("PS-169 : fenêtre de 30 jours sur $LARGE_GALLERY_SIZE médias calculée en $elapsedMillis ms")

            assertEquals(30, days.size)
            assertTrue(days.all { it.memoryCount > 0 })
            assertTrue(elapsedMillis < MAX_MILLIS, "computed in $elapsedMillis ms")
        }

    private fun getReviewDays(
        today: LocalDate,
        media: List<MediaItem> = emptyList(),
        entries: List<ReviewEntry> = emptyList(),
        repository: FakeReviewRepository = FakeReviewRepository(entries),
        history: FakeDeletionHistory = FakeDeletionHistory(),
        settings: FakeSettingsRepository = FakeSettingsRepository(),
    ): GetReviewDays {
        val clock = FixedClock.at(today.atTime(NOON))
        return GetReviewDays(
            windowProvider = ReviewWindowProvider(clock, settings),
            sameDayMatcher = SameDayMatcher(),
            mediaSource = FakeMediaSource(media),
            reviewRepository = repository,
            history = history,
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

    /** Date et nombres d'un jour, sans ses vignettes. */
    private fun ReviewDay.counts() = Triple(date, remainingCount, memoryCount)

    private companion object {
        const val LARGE_GALLERY_SIZE = 30_000
        const val NANOS_PER_MILLI = 1_000_000L
        const val MAX_MILLIS = 10_000L
    }
}
