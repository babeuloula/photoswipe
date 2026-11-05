package fr.babeuloula.photoswipe.data.review

import android.content.Context
import androidx.room.Room
import fr.babeuloula.photoswipe.data.database.PhotoSwipeDatabase
import fr.babeuloula.photoswipe.data.database.ReviewEntryDao
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.KEPT
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.MARKED_FOR_DELETION
import fr.babeuloula.photoswipe.domain.model.ReviewEntry
import fr.babeuloula.photoswipe.domain.service.ReviewWindowProvider
import fr.babeuloula.photoswipe.domain.usecase.GetDaySession
import fr.babeuloula.photoswipe.domain.usecase.KeepMedia
import fr.babeuloula.photoswipe.domain.usecase.MarkForDeletion
import fr.babeuloula.photoswipe.domain.usecase.UndoLastDecision
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.time.LocalDate
import java.time.LocalDateTime

/** [RoomDecisionJournal] sur une vraie base Room (Robolectric), en mémoire ou dans un fichier. */
@RunWith(RobolectricTestRunner::class)
class RoomDecisionJournalTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val day = LocalDate.of(2026, 7, 15)
    private val opened = mutableListOf<PhotoSwipeDatabase>()

    private fun inMemory(): PhotoSwipeDatabase =
        Room.inMemoryDatabaseBuilder(context, PhotoSwipeDatabase::class.java).build().also { opened += it }

    private fun onDisk(): PhotoSwipeDatabase =
        Room.databaseBuilder(context, PhotoSwipeDatabase::class.java, FILE).build().also { opened += it }

    @After
    fun closeDatabases() {
        opened.forEach { it.close() }
        context.deleteDatabase(FILE)
    }

    @Test
    fun `writes the decision with its record and lists a day in order`() =
        runTest {
            val database = inMemory()
            val journal = RoomDecisionJournal(database.decisionJournalDao())
            val reviews = RoomReviewRepository(database.reviewEntryDao())
            val replaced = aReviewEntry(mediaId = 2, decision = KEPT)
            reviews.save(replaced)

            val first = journal.record(day, aReviewEntry(mediaId = 1), previous = null)
            val second = journal.record(day, aReviewEntry(mediaId = 2, decision = MARKED_FOR_DELETION), replaced)
            journal.record(day.plusDays(1), aReviewEntry(mediaId = 3), previous = null)

            assertEquals(listOf(first, second), journal.recordsFor(day))
            assertTrue(first.id < second.id)
            assertEquals(MARKED_FOR_DELETION, reviews.entriesFor(2026, setOf(MediaId(2))).single().decision)
        }

    @Test
    fun `undo restores the replaced entry or deletes it, and leaves the journal`() =
        runTest {
            val database = inMemory()
            val journal = RoomDecisionJournal(database.decisionJournalDao())
            val reviews = RoomReviewRepository(database.reviewEntryDao())
            val kept = aReviewEntry(mediaId = 1, decision = KEPT)
            reviews.save(kept)
            val replacing = journal.record(day, aReviewEntry(mediaId = 1, decision = MARKED_FOR_DELETION), kept)
            val fresh = journal.record(day, aReviewEntry(mediaId = 2), previous = null)

            journal.undo(fresh)
            journal.undo(replacing)

            assertEquals(listOf(kept), reviews.entriesFor(2026, setOf(MediaId(1), MediaId(2))))
            assertEquals(emptyList<ReviewEntry>(), journal.recordsFor(day))
        }

    @Test
    fun `forgets the records of many media and of the days before a date`() =
        runTest {
            val journal = RoomDecisionJournal(inMemory().decisionJournalDao())
            val manyIds = (1L..(ReviewEntryDao.MAX_IDS_PER_QUERY * 2L + 1))
            manyIds.forEach { journal.record(day, aReviewEntry(mediaId = it), previous = null) }
            journal.record(day, aReviewEntry(mediaId = 9_999), previous = null)
            journal.record(day.minusDays(8), aReviewEntry(mediaId = 9_998, reviewYear = 2026), previous = null)

            journal.forget(manyIds.mapTo(HashSet()) { MediaId(it) })
            journal.forgetDaysBefore(day)

            assertEquals(listOf(MediaId(9_999)), journal.recordsFor(day).map { it.mediaId })
            assertEquals(emptyList<Any>(), journal.recordsFor(day.minusDays(8)))
        }

    @Test
    fun `undoes the decisions of a day after the database is closed and reopened`() =
        runTest {
            val clock = FixedClock.at(LocalDateTime.of(2026, 7, 15, 21, 0))
            val gallery = FakeMediaSource((1L..3L).map { aMediaItem(id = it) })
            val before = onDisk()
            val reviewsBefore = RoomReviewRepository(before.reviewEntryDao())
            val journalBefore = RoomDecisionJournal(before.decisionJournalDao())
            KeepMedia(reviewsBefore, journalBefore, clock)(aMediaItem(id = 1), day)
            MarkForDeletion(reviewsBefore, journalBefore, clock)(aMediaItem(id = 2), day)
            before.close()

            val after = onDisk()
            val reviews = RoomReviewRepository(after.reviewEntryDao())
            val journal = RoomDecisionJournal(after.decisionJournalDao())
            val session = GetDaySession(journal, reviews, gallery, ReviewWindowProvider(clock))(day)
            val undone = UndoLastDecision(reviews, journal)(session) as UndoLastDecision.Result.Undone

            assertEquals(listOf(1L, 2L), session.decisions.map { it.media.id.value })
            assertEquals(MediaId(2), undone.decision.media.id)
            assertEquals(listOf(MediaId(1)), reviews.entriesFor(2026, setOf(MediaId(1), MediaId(2))).map { it.mediaId })
            assertEquals(listOf(MediaId(1)), journal.recordsFor(day).map { it.mediaId })
        }

    private companion object {
        const val FILE = "decision-journal-test.db"
    }
}
