package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakeDecisionJournal
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.ReviewDecision
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class SortInBatchTest {
    private val day = LocalDate.of(2026, 7, 15)
    private val clock = FixedClock.at(LocalDateTime.of(2026, 7, 15, 21, 0))
    private val repository = FakeReviewRepository()
    private val journal = FakeDecisionJournal(repository)
    private val sortInBatch =
        SortInBatch(
            KeepMedia(repository, journal, clock),
            MarkForDeletion(repository, journal, clock),
            UndoLastDecision(repository, journal),
        )
    private val media = (1L..3L).map { aMediaItem(id = it) }
    private val ids = (1L..3L).mapTo(HashSet()) { MediaId(it) }

    private suspend fun decisions(): Map<Long, ReviewDecision> =
        repository.entriesFor(2026, ids).associate { it.mediaId.value to it.decision }

    @Test
    fun `keeps every selected memory and journals each decision in the day`() =
        runTest {
            val batch = sortInBatch.keep(media, day)

            assertEquals(
                mapOf(1L to ReviewDecision.KEPT, 2L to ReviewDecision.KEPT, 3L to ReviewDecision.KEPT),
                decisions(),
            )
            assertEquals(3, batch.decisions.size)
            assertEquals(3, journal.recordsFor(day).size)
        }

    @Test
    fun `marks every selected memory, a kept one changing its decision`() =
        runTest {
            repository.save(aReviewEntry(mediaId = 2, reviewYear = 2026, decision = ReviewDecision.KEPT))

            sortInBatch.mark(media, day)

            assertEquals(3, decisions().values.count { it == ReviewDecision.MARKED_FOR_DELETION })
        }

    @Test
    fun `undoes the whole batch, bringing back what was there before`() =
        runTest {
            repository.save(aReviewEntry(mediaId = 2, reviewYear = 2026, decision = ReviewDecision.KEPT))
            val batch = sortInBatch.mark(media, day)

            sortInBatch.undo(batch)

            assertEquals(mapOf(2L to ReviewDecision.KEPT), decisions())
        }

    @Test
    fun `stops undoing at a decision already confirmed`() =
        runTest {
            val batch = sortInBatch.mark(media, day)
            repository.markTrashed(setOf(MediaId(3)))

            sortInBatch.undo(batch)

            // Le dernier marquage est déjà dans la corbeille : rien n'est annulé.
            assertEquals(
                mapOf(
                    1L to ReviewDecision.MARKED_FOR_DELETION,
                    2L to ReviewDecision.MARKED_FOR_DELETION,
                    3L to ReviewDecision.TRASHED,
                ),
                decisions(),
            )
        }
}
