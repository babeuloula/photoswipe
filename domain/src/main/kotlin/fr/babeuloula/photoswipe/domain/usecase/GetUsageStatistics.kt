package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.UsageStatistics
import fr.babeuloula.photoswipe.domain.port.UsageStatisticsRepository

/** Statistiques d'utilisation affichées dans les réglages (PS-122). */
class GetUsageStatistics(
    private val repository: UsageStatisticsRepository,
) {
    /** Statistiques enregistrées. */
    suspend operator fun invoke(): UsageStatistics = repository.statistics()
}
