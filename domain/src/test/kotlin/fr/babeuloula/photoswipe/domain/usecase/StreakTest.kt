package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakeSettingsRepository
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.model.DayStatus
import fr.babeuloula.photoswipe.domain.model.ReviewDay
import fr.babeuloula.photoswipe.domain.model.Streak
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

/** `UpdateStreak` et `GetStreak`, testés ensemble comme l'accueil puis la page « Progression » les enchaînent. */
class StreakTest {
    private val clock = FixedClock.at(LocalDateTime.of(2026, 7, 15, 21, 0))
    private val today = LocalDate.of(2026, 7, 15)
    private val settings = FakeSettingsRepository()
    private val updateStreak = UpdateStreak(settings, clock)

    @Test
    fun `reads a sorted day, a day emptied by its deletions, a day without memory and a day left to sort`() =
        runTest {
            val streak =
                updateStreak(
                    listOf(
                        ReviewDay(today, remainingCount = 0, memoryCount = 4),
                        ReviewDay(today.minusDays(1), remainingCount = 0, memoryCount = 0, deletedCount = 2),
                        ReviewDay(today.minusDays(2), remainingCount = 0, memoryCount = 0),
                        ReviewDay(today.minusDays(3), remainingCount = 0, memoryCount = 1),
                        ReviewDay(today.minusDays(4), remainingCount = 2, memoryCount = 5),
                    ),
                )

            assertEquals(Streak(current = 4, best = 4), streak)
            assertEquals(
                listOf(DayStatus.PENDING, DayStatus.SORTED, DayStatus.EMPTY, DayStatus.SORTED, DayStatus.SORTED),
                settings.streakState.recent
                    .toSortedMap()
                    .values
                    .toList(),
            )
        }

    @Test
    fun `gives the progress page the streak last counted by the home screen`() =
        runTest {
            assertEquals(Streak(), GetStreak(settings)())

            updateStreak(listOf(ReviewDay(today, remainingCount = 0, memoryCount = 1)))

            assertEquals(Streak(current = 1, best = 1), GetStreak(settings)())
        }
}
