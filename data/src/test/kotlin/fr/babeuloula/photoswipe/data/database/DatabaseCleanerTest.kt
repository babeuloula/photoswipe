package fr.babeuloula.photoswipe.data.database

import androidx.room.Room
import fr.babeuloula.photoswipe.data.review.RoomDecisionJournal
import fr.babeuloula.photoswipe.data.review.RoomReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.MediaId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.time.LocalDate

/** [DatabaseCleaner] sur une base Room en mémoire (Robolectric). */
@RunWith(RobolectricTestRunner::class)
class DatabaseCleanerTest {
    private val database =
        Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), PhotoSwipeDatabase::class.java).build()

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `forgets every decision and the whole undo history`() =
        runTest {
            val reviews = RoomReviewRepository(database.reviewEntryDao())
            val journal = RoomDecisionJournal(database.decisionJournalDao())
            val day = LocalDate.of(2026, 11, 6)
            journal.record(day, aReviewEntry(mediaId = 1), previous = null)
            reviews.save(aReviewEntry(mediaId = 2, reviewYear = 2025))

            DatabaseCleaner(database, Dispatchers.IO).clearAll()

            assertEquals(emptySet<MediaId>(), reviews.knownMediaIds())
            assertEquals(emptyList<Any>(), journal.recordsFor(day))
        }
}
