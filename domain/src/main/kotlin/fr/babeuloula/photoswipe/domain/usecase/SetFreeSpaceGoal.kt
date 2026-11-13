package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.FreeSpaceGoal
import fr.babeuloula.photoswipe.domain.port.AchievementsRepository
import fr.babeuloula.photoswipe.domain.port.UsageStatisticsRepository

/**
 * Fixe l'objectif de place à libérer (PS-158), compté à partir de maintenant, ou le retire.
 */
class SetFreeSpaceGoal(
    private val achievements: AchievementsRepository,
    private val statistics: UsageStatisticsRepository,
) {
    /** Fixe un objectif de [targetBytes] octets ; `null` le retire. */
    suspend operator fun invoke(targetBytes: Long?) {
        val goal = targetBytes?.let { FreeSpaceGoal(it, statistics.statistics().freedBytes) }
        achievements.saveFreeSpaceGoal(goal)
    }
}
