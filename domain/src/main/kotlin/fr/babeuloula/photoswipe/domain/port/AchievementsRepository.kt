package fr.babeuloula.photoswipe.domain.port

import fr.babeuloula.photoswipe.domain.model.Badge
import fr.babeuloula.photoswipe.domain.model.FreeSpaceGoal

/**
 * Badges obtenus et objectif de place à libérer (PS-158). Implémenté dans `:data` sur DataStore.
 */
interface AchievementsRepository {
    /** Badges déjà obtenus, et donc déjà annoncés. */
    suspend fun earnedBadges(): Set<Badge>

    /** Enregistre les badges obtenus [badges]. */
    suspend fun saveEarnedBadges(badges: Set<Badge>)

    /** Objectif en cours, `null` s'il n'y en a pas. */
    suspend fun freeSpaceGoal(): FreeSpaceGoal?

    /** Fixe l'objectif [goal], ou le retire avec `null`. */
    suspend fun saveFreeSpaceGoal(goal: FreeSpaceGoal?)
}
