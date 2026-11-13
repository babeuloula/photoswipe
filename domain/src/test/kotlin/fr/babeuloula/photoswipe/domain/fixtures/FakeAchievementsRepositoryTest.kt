package fr.babeuloula.photoswipe.domain.fixtures

import fr.babeuloula.photoswipe.domain.model.Badge
import fr.babeuloula.photoswipe.domain.model.FreeSpaceGoal
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** `FakeAchievementsRepository` respecte le contrat de son port. */
class FakeAchievementsRepositoryTest {
    @Test
    fun `starts with no badge and no goal, then keeps what is saved`() =
        runTest {
            val repository = FakeAchievementsRepository()
            assertEquals(emptySet<Badge>(), repository.earnedBadges())
            assertNull(repository.freeSpaceGoal())

            repository.saveEarnedBadges(setOf(Badge.STREAK_7))
            repository.saveFreeSpaceGoal(FreeSpaceGoal(10, 2))

            assertEquals(setOf(Badge.STREAK_7), repository.earnedBadges())
            assertEquals(FreeSpaceGoal(10, 2), repository.freeSpaceGoal())
            repository.saveFreeSpaceGoal(null)
            assertNull(repository.freeSpaceGoal())
        }
}
