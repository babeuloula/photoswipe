package fr.babeuloula.photoswipe.domain.fixtures

import fr.babeuloula.photoswipe.domain.model.DailyActivity
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

/** `FakeActivityHistory` respecte le contrat de son port. */
class FakeActivityHistoryTest {
    private val july14 = LocalDate.of(2026, 7, 14)
    private val july15 = LocalDate.of(2026, 7, 15)

    @Test
    fun `starts empty`() =
        runTest {
            val history = FakeActivityHistory()

            assertEquals(emptyList<DailyActivity>(), history.since(LocalDate.MIN))
            assertNull(history.firstDay())
        }

    @Test
    fun `adds up the confirmed deletions of a day and keeps its kept media`() =
        runTest {
            val history = FakeActivityHistory(listOf(DailyActivity(july15, keptCount = 4, favoriteCount = 1)))

            history.recordTrashed(july15, count = 2, bytes = 1_500)
            history.recordTrashed(july15, count = 1, bytes = 500)

            assertEquals(
                listOf(DailyActivity(july15, keptCount = 4, favoriteCount = 1, trashedCount = 3, freedBytes = 2_000)),
                history.since(july15),
            )
        }

    @Test
    fun `returns the days from the given one, oldest first, and the first recorded day`() =
        runTest {
            val history = FakeActivityHistory()
            history.recordTrashed(july15, count = 1, bytes = 10)
            history.recordTrashed(july14, count = 2, bytes = 20)

            assertEquals(listOf(july14, july15), history.since(july14).map { it.day })
            assertEquals(listOf(july15), history.since(july15).map { it.day })
            assertEquals(july14, history.firstDay())
        }

    @Test
    fun `removes restored media from the day they were confirmed, never below zero`() =
        runTest {
            val history = FakeActivityHistory(listOf(DailyActivity(july15, keptCount = 4)))
            history.recordTrashed(july15, count = 3, bytes = 3_000)

            history.removeTrashed(july15, count = 1, bytes = 1_000)
            assertEquals(
                listOf(DailyActivity(july15, keptCount = 4, trashedCount = 2, freedBytes = 2_000)),
                history.days,
            )

            history.removeTrashed(july15, count = 5, bytes = 9_000)
            history.removeTrashed(july14, count = 1, bytes = 10)
            assertEquals(listOf(DailyActivity(july15, keptCount = 4)), history.days)
        }
}
