package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakeDecisionJournal
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.KEPT
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.MARKED_FOR_DELETION
import fr.babeuloula.photoswipe.domain.model.ReviewEntry
import fr.babeuloula.photoswipe.domain.model.ReviewSession
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

class UndoLastDecisionTest {
    private val today = LocalDate.of(2026, 7, 15)
    private val clock = FixedClock.at(LocalDateTime.of(2026, 7, 15, 21, 0))
    private val repository = FakeReviewRepository()
    private val keepMedia = KeepMedia(repository, FakeDecisionJournal(repository), clock)
    private val markForDeletion = MarkForDeletion(repository, FakeDecisionJournal(repository), clock)
    private val undo = UndoLastDecision(repository, FakeDecisionJournal(repository))

    @Test
    fun `has nothing to undo in an empty session`() =
        runTest {
            assertEquals(UndoLastDecision.Result.NothingToUndo, undo(ReviewSession()))
        }

    @Test
    fun `undoes the last decision and makes the media to sort again`() =
        runTest {
            val media = aMediaItem(id = 1)
            val session = ReviewSession().record(markForDeletion(media, today))

            val result = assertInstanceOf(UndoLastDecision.Result.Undone::class.java, undo(session))

            assertEquals(media, result.decision.media)
            assertEquals(today, result.decision.day)
            assertEquals(ReviewSession(), result.session)
            assertEquals(emptyList<ReviewEntry>(), repository.entries)
        }

    @Test
    fun `undoes several decisions from the most recent to the oldest`() =
        runTest {
            var session = ReviewSession()
            session = session.record(keepMedia(aMediaItem(id = 1), today))
            clock.advanceBy(Duration.ofSeconds(1))
            session = session.record(markForDeletion(aMediaItem(id = 2), today))
            clock.advanceBy(Duration.ofSeconds(1))
            session = session.record(keepMedia(aMediaItem(id = 3), LocalDate.of(2026, 7, 12)))

            val undone = mutableListOf<Long>()
            while (true) {
                val result = undo(session) as? UndoLastDecision.Result.Undone ?: break
                undone += result.decision.media.id.value
                session = result.session
            }

            assertEquals(listOf(3L, 2L, 1L), undone)
            assertEquals(emptyList<ReviewEntry>(), repository.entries)
            assertEquals(UndoLastDecision.Result.NothingToUndo, undo(session))
        }

    @Test
    fun `undoes only the last decision and keeps the older ones`() =
        runTest {
            val session =
                ReviewSession()
                    .record(keepMedia(aMediaItem(id = 1), today))
                    .record(markForDeletion(aMediaItem(id = 2), today))

            val result = assertInstanceOf(UndoLastDecision.Result.Undone::class.java, undo(session))

            assertEquals(listOf(MediaId(1)), repository.entries.map { it.mediaId })
            assertEquals(
                MediaId(1),
                result.session.lastDecision
                    ?.media
                    ?.id,
            )
        }

    @Test
    fun `restores the decision that the undone one replaced`() =
        runTest {
            val kept = aReviewEntry(mediaId = 1, reviewYear = 2026, decision = KEPT)
            val repository = FakeReviewRepository(listOf(kept))
            val session =
                ReviewSession().record(
                    MarkForDeletion(repository, FakeDecisionJournal(repository), clock)(aMediaItem(id = 1), today),
                )

            UndoLastDecision(repository, FakeDecisionJournal(repository))(session)

            assertEquals(listOf(kept), repository.entries)
        }

    @Test
    fun `refuses to undo a deletion already confirmed`() =
        runTest {
            val media = aMediaItem(id = 1)
            val session = ReviewSession().record(markForDeletion(media, today))
            repository.markTrashed(setOf(media.id))

            assertEquals(UndoLastDecision.Result.AlreadyConfirmed, undo(session))
            assertEquals(1, repository.entries.size)
        }

    @Test
    fun `does not touch the decision of the same media for another review year`() =
        runTest {
            // Un marquage ne lève rien des autres années, contrairement à « garder » (PS-113).
            val lastYear = aReviewEntry(mediaId = 1, reviewYear = 2025, decision = KEPT)
            val repository = FakeReviewRepository(listOf(lastYear))
            val session =
                ReviewSession().record(
                    MarkForDeletion(repository, FakeDecisionJournal(repository), clock)(aMediaItem(id = 1), today),
                )

            UndoLastDecision(repository, FakeDecisionJournal(repository))(session)

            assertEquals(listOf(lastYear), repository.entries)
        }
}
