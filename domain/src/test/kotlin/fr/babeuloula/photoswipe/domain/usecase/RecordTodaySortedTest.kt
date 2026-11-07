package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakeSettingsRepository
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.model.RatingInvitation
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

class RecordTodaySortedTest {
    private val clock = FixedClock.at(LocalDateTime.of(2026, 11, 7, 20, 0))
    private val repository = FakeSettingsRepository()
    private val recordTodaySorted = RecordTodaySorted(repository, clock)
    private val markRatingAsked = MarkRatingAsked(repository)

    private suspend fun sortOnNextDay(): Boolean {
        clock.advanceBy(Duration.ofDays(1))
        return recordTodaySorted()
    }

    @Test
    fun `invites to rate the app on the third different sorted day`() =
        runTest {
            assertFalse(recordTodaySorted())
            assertFalse(sortOnNextDay())

            assertTrue(sortOnNextDay())
            assertEquals(3, repository.ratingInvitation.sortedDays)
        }

    @Test
    fun `counts the same day only once, even when sorted again`() =
        runTest {
            recordTodaySorted()
            recordTodaySorted()
            sortOnNextDay()

            assertFalse(recordTodaySorted())
            assertEquals(
                RatingInvitation(sortedDays = 2, lastSortedDay = LocalDate.of(2026, 11, 8)),
                repository.ratingInvitation,
            )
        }

    @Test
    fun `never invites again once asked`() =
        runTest {
            recordTodaySorted()
            sortOnNextDay()
            sortOnNextDay()

            markRatingAsked()

            assertFalse(recordTodaySorted())
            assertFalse(sortOnNextDay())
            assertTrue(repository.ratingInvitation.asked)
        }
}
