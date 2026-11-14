package fr.babeuloula.photoswipe.data.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.babeuloula.photoswipe.data.review.RoomDecisionJournal
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.ReviewDecision
import fr.babeuloula.photoswipe.domain.model.ReviewEntry
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Journal des décisions sur le SQLite du téléphone, pas celui de Robolectric : Android 10 embarque
 * SQLite 3.22, sans `INSERT … ON CONFLICT … DO UPDATE` (3.24). Garder un média y échouait, et la pile
 * restait figée sur sa première carte.
 */
@RunWith(AndroidJUnit4::class)
class DecisionJournalDaoInstrumentedTest {
    private val database =
        Room
            .inMemoryDatabaseBuilder(
                InstrumentationRegistry.getInstrumentation().targetContext,
                PhotoSwipeDatabase::class.java,
            ).build()
    private val clock = Clock.fixed(Instant.parse("2026-07-15T19:00:00Z"), ZoneOffset.UTC)
    private val journal = RoomDecisionJournal(database.decisionJournalDao(), clock)
    private val day = LocalDate.of(2026, 7, 15)

    @After
    fun close() {
        database.close()
    }

    private fun kept(mediaId: Long) =
        ReviewEntry(
            mediaId = MediaId(mediaId),
            reviewYear = 2026,
            decision = ReviewDecision.KEPT,
            sizeBytes = 1_000,
            decidedAt = clock.instant(),
        )

    private suspend fun keptToday(): Int =
        database
            .dailyActivityDao()
            .since(day.toEpochDay())
            .singleOrNull()
            ?.keptCount ?: 0

    @Test
    fun keepsMediaAndCountsThemInTheActivityOfTheDayOnTheSqliteOfThePhone() =
        runTest {
            journal.record(day, kept(1), previous = null)
            val second = journal.record(day, kept(2), previous = null)

            assertEquals(2, keptToday())
            assertEquals(2, journal.recordsFor(day).size)

            journal.undo(second)

            assertEquals(1, keptToday())
        }
}
