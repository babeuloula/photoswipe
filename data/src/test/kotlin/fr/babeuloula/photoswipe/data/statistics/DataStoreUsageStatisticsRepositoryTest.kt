package fr.babeuloula.photoswipe.data.statistics

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.model.UsageStatistics
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

/** DataStore réel, sur un fichier temporaire : aucune dépendance au framework Android. */
class DataStoreUsageStatisticsRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val clock = FixedClock.at(LocalDateTime.of(2026, 11, 7, 10, 0))
    private val installedOn =
        LocalDateTime
            .of(2026, 3, 2, 9, 0)
            .atZone(FixedClock.PARIS)
            .toInstant()
            .toEpochMilli()

    private fun TestScope.repository(
        dataStore: DataStore<Preferences> = dataStore(),
        firstInstallMillis: Long = installedOn,
    ) = DataStoreUsageStatisticsRepository(dataStore, clock) { firstInstallMillis }

    @Test
    fun `starts with nothing deleted since the day the app was installed`() =
        runTest {
            assertEquals(UsageStatistics(LocalDate.of(2026, 3, 2)), repository().statistics())
        }

    @Test
    fun `starts today when the install date is unknown, and keeps that first day`() =
        runTest {
            val dataStore = dataStore()
            assertEquals(LocalDate.of(2026, 11, 7), repository(dataStore, firstInstallMillis = 0).statistics().since)

            assertEquals(LocalDate.of(2026, 11, 7), repository(dataStore).statistics().since)
        }

    @Test
    fun `adds up the confirmed deletions and clears them, keeping the first day`() =
        runTest {
            val repository = repository()
            repository.recordDeletion(3, 3_000_000)
            repository.recordDeletion(1, 500)

            assertEquals(UsageStatistics(LocalDate.of(2026, 3, 2), 4, 3_000_500), repository.statistics())

            repository.clear()

            assertEquals(UsageStatistics(LocalDate.of(2026, 3, 2)), repository.statistics())
        }

    @Test
    fun `removes a restored deletion without going below zero`() =
        runTest {
            val repository = repository()
            repository.recordDeletion(3, 3_000)

            repository.removeDeletion(1, 1_000)
            assertEquals(UsageStatistics(LocalDate.of(2026, 3, 2), 2, 2_000), repository.statistics())

            repository.removeDeletion(5, 9_000)
            assertEquals(UsageStatistics(LocalDate.of(2026, 3, 2)), repository.statistics())
        }

    private fun TestScope.dataStore(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { File(folder.root, "statistics-${System.nanoTime()}.preferences_pb") },
        )
}
