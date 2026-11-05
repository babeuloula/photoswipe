package fr.babeuloula.photoswipe.domain.fixtures

import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.KEPT
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.MARKED_FOR_DELETION
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

/** Le fake respecte le contrat de `DecisionJournal`, comme l'implémentation Room. */
class FakeDecisionJournalTest {
    private val reviews = FakeReviewRepository()
    private val journal = FakeDecisionJournal(reviews)
    private val day = LocalDate.of(2026, 7, 15)

    @Test
    fun `writes the decision and lists the records of a day in order`() =
        runTest {
            val first = journal.record(day, aReviewEntry(mediaId = 1), previous = null)
            val second = journal.record(day, aReviewEntry(mediaId = 2, decision = MARKED_FOR_DELETION), previous = null)
            journal.record(day.plusDays(1), aReviewEntry(mediaId = 3), previous = null)

            assertEquals(listOf(first, second), journal.recordsFor(day))
            assertTrue(first.id < second.id)
            assertEquals(3, reviews.entries.size)
        }

    @Test
    fun `undo restores the replaced entry or deletes it, and leaves the journal`() =
        runTest {
            val kept = aReviewEntry(mediaId = 1, decision = KEPT)
            reviews.save(kept)
            val replacing =
                journal.record(
                    day,
                    aReviewEntry(mediaId = 1, decision = MARKED_FOR_DELETION),
                    previous = kept,
                )
            val fresh = journal.record(day, aReviewEntry(mediaId = 2), previous = null)

            journal.undo(fresh)
            journal.undo(replacing)

            assertEquals(listOf(kept), reviews.entries)
            assertEquals(emptyList<Any>(), journal.recordsFor(day))
        }

    @Test
    fun `forgets the records of some media or of the days before a date`() =
        runTest {
            journal.record(day.minusDays(8), aReviewEntry(mediaId = 1), previous = null)
            journal.record(day, aReviewEntry(mediaId = 2), previous = null)
            journal.record(day, aReviewEntry(mediaId = 3), previous = null)

            journal.forget(setOf(MediaId(2)))
            journal.forgetDaysBefore(day)

            assertEquals(listOf(MediaId(3)), journal.records.map { it.mediaId })
            assertEquals(3, reviews.entries.size)
        }
}
