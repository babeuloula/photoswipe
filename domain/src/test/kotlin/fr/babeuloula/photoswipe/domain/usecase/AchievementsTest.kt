package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakeAchievementsRepository
import fr.babeuloula.photoswipe.domain.fixtures.FakeActivityHistory
import fr.babeuloula.photoswipe.domain.fixtures.FakeSettingsRepository
import fr.babeuloula.photoswipe.domain.fixtures.FakeUsageStatisticsRepository
import fr.babeuloula.photoswipe.domain.model.Badge
import fr.babeuloula.photoswipe.domain.model.DailyActivity
import fr.babeuloula.photoswipe.domain.model.DayStatus
import fr.babeuloula.photoswipe.domain.model.FreeSpaceGoal
import fr.babeuloula.photoswipe.domain.model.GIGABYTE
import fr.babeuloula.photoswipe.domain.model.Streak
import fr.babeuloula.photoswipe.domain.model.StreakState
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

/** `GetAchievements` et `SetFreeSpaceGoal`, testés ensemble comme la page « Progression » les enchaîne. */
class AchievementsTest {
    private val statistics = FakeUsageStatisticsRepository(LocalDate.of(2026, 3, 2))
    private val activity = FakeActivityHistory(listOf(DailyActivity(LocalDate.of(2026, 7, 1), keptCount = 60)))
    private val settings = FakeSettingsRepository()
    private val repository = FakeAchievementsRepository()
    private val getAchievements = GetAchievements(statistics, activity, settings, repository)

    @Test
    fun `earns the badges whose measure is reached, kept and deleted media counting as sorted`() =
        runTest {
            statistics.recordDeletion(40, 2 * GIGABYTE)

            val achievements = getAchievements()

            val earned = achievements.badges.filter { it.earned }.map { it.badge }
            assertEquals(listOf(Badge.FIRST_SORT, Badge.SORTED_100, Badge.FREED_1_GB), earned)
            assertEquals(900L, achievements.badges.single { it.badge == Badge.SORTED_1000 }.remaining)
            assertEquals(3 * GIGABYTE, achievements.badges.single { it.badge == Badge.FREED_5_GB }.remaining)
        }

    @Test
    fun `announces a badge once, and keeps it when its measure goes down`() =
        runTest {
            val first = getAchievements()
            assertEquals(listOf(Badge.FIRST_SORT), first.newlyEarned)

            val again = GetAchievements(statistics, FakeActivityHistory(), settings, repository)()

            assertEquals(emptyList<Badge>(), again.newlyEarned)
            assertTrue(again.badges.single { it.badge == Badge.FIRST_SORT }.earned)
        }

    @Test
    fun `counts the best streak and a whole week caught up`() =
        runTest {
            val week = (0L..6L).associate { LocalDate.of(2026, 7, 15).minusDays(it) to DayStatus.SORTED }
            settings.saveStreakState(StreakState(recent = week, streak = Streak(current = 7, best = 8)))

            val earned = getAchievements().badges.filter { it.earned }.map { it.badge }

            assertTrue(Badge.STREAK_7 in earned)
            assertTrue(Badge.WEEK_CAUGHT_UP in earned)
            assertFalse(Badge.STREAK_30 in earned)
        }

    @Test
    fun `does not count a week with a day left to sort as caught up`() =
        runTest {
            val week =
                (0L..6L).associate {
                    LocalDate.of(2026, 7, 15).minusDays(it) to if (it == 3L) DayStatus.PENDING else DayStatus.SORTED
                }
            settings.saveStreakState(StreakState(recent = week))

            assertFalse(getAchievements().badges.single { it.badge == Badge.WEEK_CAUGHT_UP }.earned)
        }

    @Test
    fun `counts a goal from the moment it is set, until it is removed`() =
        runTest {
            statistics.recordDeletion(10, GIGABYTE)
            SetFreeSpaceGoal(repository, statistics)(5 * GIGABYTE)
            statistics.recordDeletion(10, 2 * GIGABYTE)

            val goal = getAchievements().goal

            assertEquals(FreeSpaceGoal(5 * GIGABYTE, GIGABYTE), goal?.goal)
            assertEquals(2 * GIGABYTE, goal?.freedBytes)
            assertEquals(0.4f, goal?.fraction)
            assertFalse(goal!!.reached)

            SetFreeSpaceGoal(repository, statistics)(null)
            assertNull(getAchievements().goal)
        }
}
