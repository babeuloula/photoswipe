package fr.babeuloula.photoswipe.data.achievements

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import fr.babeuloula.photoswipe.domain.model.Badge
import fr.babeuloula.photoswipe.domain.model.FreeSpaceGoal
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** DataStore réel, sur un fichier temporaire : aucune dépendance au framework Android. */
class DataStoreAchievementsRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun TestScope.dataStore(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { File(folder.root, "achievements-${System.nanoTime()}.preferences_pb") },
        )

    @Test
    fun `stores the earned badges, none at first`() =
        runTest {
            val repository = DataStoreAchievementsRepository(dataStore())
            assertEquals(emptySet<Badge>(), repository.earnedBadges())

            repository.saveEarnedBadges(setOf(Badge.FIRST_SORT, Badge.STREAK_7))

            assertEquals(setOf(Badge.FIRST_SORT, Badge.STREAK_7), repository.earnedBadges())
        }

    @Test
    fun `ignores a badge name it does not know`() =
        runTest {
            val store = dataStore()
            store.edit { it[stringSetPreferencesKey("earned_badges")] = setOf("FIRST_SORT", "RENAMED") }

            assertEquals(setOf(Badge.FIRST_SORT), DataStoreAchievementsRepository(store).earnedBadges())
        }

    @Test
    fun `stores the goal and removes it`() =
        runTest {
            val repository = DataStoreAchievementsRepository(dataStore())
            assertNull(repository.freeSpaceGoal())

            repository.saveFreeSpaceGoal(FreeSpaceGoal(targetBytes = 5_000_000_000, startFreedBytes = 120))
            assertEquals(FreeSpaceGoal(5_000_000_000, 120), repository.freeSpaceGoal())

            repository.saveFreeSpaceGoal(null)
            assertNull(repository.freeSpaceGoal())
        }
}
