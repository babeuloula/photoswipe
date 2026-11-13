package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakeActivityHistory
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.model.DailyActivity
import fr.babeuloula.photoswipe.domain.model.MonthlyActivity
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class GetActivityReportTest {
    private val clock = FixedClock.at(LocalDateTime.of(2026, 2, 10, 21, 0))

    private fun report(vararg days: DailyActivity) = GetActivityReport(FakeActivityHistory(days.toList()), clock)

    @Test
    fun `lists the last six months without a gap, across the new year, the current one last`() =
        runTest {
            val months = report()().months.map { it.month }

            assertEquals((9..12).map { YearMonth.of(2025, it) } + (1..2).map { YearMonth.of(2026, it) }, months)
        }

    @Test
    fun `adds up the days of each month`() =
        runTest {
            val activity =
                report(
                    DailyActivity(LocalDate.of(2026, 1, 3), keptCount = 5, favoriteCount = 1),
                    DailyActivity(LocalDate.of(2026, 1, 31), keptCount = 2, trashedCount = 4, freedBytes = 900),
                    DailyActivity(LocalDate.of(2026, 2, 1), trashedCount = 1, freedBytes = 100),
                )()

            assertEquals(
                MonthlyActivity(
                    YearMonth.of(2026, 1),
                    keptCount = 7,
                    favoriteCount = 1,
                    trashedCount = 4,
                    freedBytes = 900,
                ),
                activity.months[4],
            )
            assertEquals(MonthlyActivity(YearMonth.of(2026, 2), trashedCount = 1, freedBytes = 100), activity.months[5])
            assertEquals(11, activity.months[4].sortedCount)
        }

    @Test
    fun `leaves out what is older than the six months but still tells when the history starts`() =
        runTest {
            val activity = report(DailyActivity(LocalDate.of(2025, 8, 31), keptCount = 9))()

            assertTrue(activity.isEmpty)
            assertEquals(LocalDate.of(2025, 8, 31), activity.since)
        }

    @Test
    fun `gives the share of kept media among those sorted`() =
        runTest {
            val activity = report(DailyActivity(LocalDate.of(2026, 2, 1), keptCount = 3, trashedCount = 1))()

            assertEquals(75, activity.keptPercent)
            assertEquals(3, activity.keptCount)
            assertEquals(1, activity.trashedCount)
        }

    @Test
    fun `has no share and no start while nothing was sorted`() =
        runTest {
            val activity = report()()

            assertTrue(activity.isEmpty)
            assertNull(activity.keptPercent)
            assertNull(activity.since)
        }
}
