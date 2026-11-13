package fr.babeuloula.photoswipe.domain.fixtures

import fr.babeuloula.photoswipe.domain.model.UsageStatistics
import fr.babeuloula.photoswipe.domain.usecase.GetUsageStatistics
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

/** `FakeUsageStatisticsRepository` respecte le contrat de son port. */
class FakeUsageStatisticsRepositoryTest {
    private val since = LocalDate.of(2026, 11, 7)
    private val repository = FakeUsageStatisticsRepository(since)

    @Test
    fun `starts with nothing deleted since the first day of use`() =
        runTest {
            assertEquals(UsageStatistics(since), GetUsageStatistics(repository)())
        }

    @Test
    fun `adds up the confirmed deletions and clears them`() =
        runTest {
            repository.recordDeletion(3, 3_000)
            repository.recordDeletion(2, 500)

            assertEquals(UsageStatistics(since, deletedCount = 5, freedBytes = 3_500), repository.statistics())

            repository.clear()

            assertEquals(UsageStatistics(since), repository.statistics())
        }

    @Test
    fun `removes a restored deletion without going below zero`() =
        runTest {
            repository.recordDeletion(3, 3_000)

            repository.removeDeletion(1, 1_000)
            assertEquals(UsageStatistics(since, deletedCount = 2, freedBytes = 2_000), repository.statistics())

            repository.removeDeletion(5, 9_000)
            assertEquals(UsageStatistics(since), repository.statistics())
        }
}
