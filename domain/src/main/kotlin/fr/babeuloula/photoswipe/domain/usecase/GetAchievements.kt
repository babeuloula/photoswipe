package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.Achievements
import fr.babeuloula.photoswipe.domain.model.Badge
import fr.babeuloula.photoswipe.domain.model.BadgeMeasures
import fr.babeuloula.photoswipe.domain.model.BadgeStatus
import fr.babeuloula.photoswipe.domain.model.DayStatus
import fr.babeuloula.photoswipe.domain.model.GoalProgress
import fr.babeuloula.photoswipe.domain.port.AchievementsRepository
import fr.babeuloula.photoswipe.domain.port.ActivityHistory
import fr.babeuloula.photoswipe.domain.port.SettingsRepository
import fr.babeuloula.photoswipe.domain.port.UsageStatisticsRepository
import java.time.LocalDate

/**
 * Badges et objectif de la page « Progression » (PS-158), calculés depuis les statistiques
 * d'utilisation, l'activité de tri et la série de jours, sans table dédiée. Un badge atteint est
 * enregistré : il reste obtenu si sa mesure redescend, et n'est annoncé qu'une fois
 * ([Achievements.newlyEarned]).
 *
 * Les médias gardés ne sont connus que depuis l'historique d'activité (ADR 0037) : les badges de
 * médias triés ne comptent donc les médias gardés qu'à partir de cette version.
 */
class GetAchievements(
    private val statistics: UsageStatisticsRepository,
    private val activity: ActivityHistory,
    private val settings: SettingsRepository,
    private val achievements: AchievementsRepository,
) {
    /** Calcule les badges, enregistre ceux qui viennent d'être obtenus et lit l'objectif. */
    suspend operator fun invoke(): Achievements {
        val usage = statistics.statistics()
        val streak = settings.streakState()
        // La semaine la plus récente, quelle que soit la durée de la fenêtre de rattrapage (PS-169).
        val window =
            streak.recent
                .toSortedMap(compareByDescending { it })
                .values
                .take(WEEK_DAYS)
        val measures =
            BadgeMeasures(
                sorted = usage.deletedCount + activity.since(LocalDate.EPOCH).sumOf { it.keptCount.toLong() },
                bestStreak = streak.streak.best.toLong(),
                freedBytes = usage.freedBytes,
                weekCaughtUp =
                    window.size >= WEEK_DAYS && DayStatus.PENDING !in window && DayStatus.SORTED in window,
            )
        val alreadyEarned = achievements.earnedBadges()
        val reached = Badge.entries.filterTo(HashSet()) { measures.valueOf(it.kind) >= it.threshold }
        val newlyEarned = Badge.entries.filter { it in reached && it !in alreadyEarned }
        if (newlyEarned.isNotEmpty()) achievements.saveEarnedBadges(alreadyEarned + newlyEarned)
        val earned = alreadyEarned + reached
        return Achievements(
            badges =
                Badge.entries.map { badge ->
                    val left = if (badge in earned) 0 else badge.threshold - measures.valueOf(badge.kind)
                    BadgeStatus(badge, earned = badge in earned, remaining = left.coerceAtLeast(0))
                },
            newlyEarned = newlyEarned,
            goal =
                achievements.freeSpaceGoal()?.let { goal ->
                    GoalProgress(goal, (usage.freedBytes - goal.startFreedBytes).coerceAtLeast(0))
                },
        )
    }

    private companion object {
        /** Jours les plus récents à avoir tous triés pour « Semaine rattrapée ». */
        const val WEEK_DAYS = 7
    }
}
