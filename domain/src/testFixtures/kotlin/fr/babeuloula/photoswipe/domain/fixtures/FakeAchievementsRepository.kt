package fr.babeuloula.photoswipe.domain.fixtures

import fr.babeuloula.photoswipe.domain.model.Badge
import fr.babeuloula.photoswipe.domain.model.FreeSpaceGoal
import fr.babeuloula.photoswipe.domain.port.AchievementsRepository

/** [AchievementsRepository] en mémoire. */
class FakeAchievementsRepository(
    earned: Set<Badge> = emptySet(),
    goal: FreeSpaceGoal? = null,
) : AchievementsRepository {
    /** Badges obtenus enregistrés, pour les vérifications du test. */
    var earned: Set<Badge> = earned
        private set

    /** Objectif enregistré, pour les vérifications du test. */
    var goal: FreeSpaceGoal? = goal
        private set

    override suspend fun earnedBadges(): Set<Badge> = earned

    override suspend fun saveEarnedBadges(badges: Set<Badge>) {
        earned = badges
    }

    override suspend fun freeSpaceGoal(): FreeSpaceGoal? = goal

    override suspend fun saveFreeSpaceGoal(goal: FreeSpaceGoal?) {
        this.goal = goal
    }
}
