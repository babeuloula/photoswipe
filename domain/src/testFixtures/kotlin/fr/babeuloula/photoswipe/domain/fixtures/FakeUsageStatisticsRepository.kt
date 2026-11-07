package fr.babeuloula.photoswipe.domain.fixtures

import fr.babeuloula.photoswipe.domain.model.UsageStatistics
import fr.babeuloula.photoswipe.domain.port.UsageStatisticsRepository
import java.time.LocalDate

/**
 * [UsageStatisticsRepository] en mémoire : des totaux nuls depuis [since] au départ, cumulés à
 * chaque suppression enregistrée.
 */
class FakeUsageStatisticsRepository(
    since: LocalDate = LocalDate.of(2026, 1, 1),
) : UsageStatisticsRepository {
    /** Statistiques enregistrées, pour les vérifications du test. */
    var current: UsageStatistics = UsageStatistics(since)
        private set

    override suspend fun statistics(): UsageStatistics = current

    override suspend fun recordDeletion(
        count: Int,
        bytes: Long,
    ) {
        current = current.afterDeletion(count, bytes)
    }

    override suspend fun clear() {
        current = UsageStatistics(current.since)
    }
}
