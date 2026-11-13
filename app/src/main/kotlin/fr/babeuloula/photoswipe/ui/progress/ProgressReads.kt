package fr.babeuloula.photoswipe.ui.progress

import fr.babeuloula.photoswipe.domain.usecase.GetAchievements
import fr.babeuloula.photoswipe.domain.usecase.GetActivityReport
import fr.babeuloula.photoswipe.domain.usecase.GetStreak
import fr.babeuloula.photoswipe.domain.usecase.GetUsageStatistics
import fr.babeuloula.photoswipe.domain.usecase.GetYearMemories
import javax.inject.Inject

/**
 * Use cases que la page « Progression » lit, regroupés (epic 13).
 *
 * @property getUsageStatistics statistiques d'utilisation (PS-122).
 * @property getActivityReport activité des six derniers mois (PS-154).
 * @property getStreak série de jours (PS-155).
 * @property getYearMemories souvenirs de chaque jour de l'année, pour le calendrier (PS-157).
 * @property getAchievements badges et objectif (PS-158).
 */
class ProgressReads
    @Inject
    constructor(
        val getUsageStatistics: GetUsageStatistics,
        val getActivityReport: GetActivityReport,
        val getStreak: GetStreak,
        val getYearMemories: GetYearMemories,
        val getAchievements: GetAchievements,
    )
