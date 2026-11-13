package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.DayMemories
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime.NOON

class GetYearMemoriesTest {
    // 2026 n'est pas bissextile.
    private val clock = FixedClock.at(LocalDateTime.of(2026, 7, 15, 21, 0))
    private val repository = FakeReviewRepository()

    private fun takenOn(
        id: Long,
        date: LocalDate,
    ): MediaItem = aMediaItem(id = id, takenAt = date.atTime(NOON).atZone(FixedClock.PARIS).toInstant())

    private fun yearMemories(vararg media: MediaItem) =
        GetYearMemories(SameDayMatcher(), FakeMediaSource(media.toList()), repository, clock)

    @Test
    fun `counts the memories of every day of the year from all the past years`() =
        runTest {
            val memories =
                yearMemories(
                    takenOn(1, LocalDate.of(2025, 3, 14)),
                    takenOn(2, LocalDate.of(2019, 3, 14)),
                    takenOn(3, LocalDate.of(2024, 12, 31)),
                    // Pris cette année : pas encore un souvenir.
                    takenOn(4, LocalDate.of(2026, 3, 14)),
                )().first()

            assertEquals(2026, memories.year)
            assertEquals(DayMemories(memoryCount = 2, remainingCount = 2), memories[LocalDate.of(2026, 3, 14)])
            assertEquals(DayMemories(memoryCount = 1, remainingCount = 1), memories[LocalDate.of(2026, 12, 31)])
            assertNull(memories[LocalDate.of(2026, 3, 15)])
        }

    @Test
    fun `shows the memories of February 29 on February 28 in a non leap year`() =
        runTest {
            val memories =
                yearMemories(
                    takenOn(1, LocalDate.of(2024, 2, 29)),
                    takenOn(2, LocalDate.of(2025, 2, 28)),
                )().first()

            assertEquals(DayMemories(memoryCount = 2, remainingCount = 2), memories[LocalDate.of(2026, 2, 28)])
        }

    @Test
    fun `counts as left to sort only the memories without a decision this year, a reopened day included`() =
        runTest {
            repository.save(aReviewEntry(mediaId = 1, reviewYear = 2026))
            repository.save(aReviewEntry(mediaId = 2, reviewYear = 2025))
            repository.save(aReviewEntry(mediaId = 3, reviewYear = 2026, reopened = true))
            val memories =
                yearMemories(
                    takenOn(1, LocalDate.of(2025, 8, 1)),
                    takenOn(2, LocalDate.of(2024, 8, 1)),
                    takenOn(3, LocalDate.of(2023, 8, 1)),
                )().first()

            assertEquals(DayMemories(memoryCount = 3, remainingCount = 2), memories[LocalDate.of(2026, 8, 1)])
        }

    @Test
    fun `follows the decisions of the year, a day sorted in advance included`() =
        runTest {
            val counts = mutableListOf<DayMemories?>()
            val day = LocalDate.of(2026, 12, 25)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                yearMemories(takenOn(1, LocalDate.of(2020, 12, 25)))().take(2).toList().forEach { counts += it[day] }
            }

            repository.save(aReviewEntry(mediaId = 1, reviewYear = 2026))

            assertEquals(listOf(DayMemories(1, 1), DayMemories(1, 0)), counts)
            assertEquals(true, counts.last()?.isSorted)
        }
}
