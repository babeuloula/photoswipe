package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakeDecisionJournal
import fr.babeuloula.photoswipe.domain.fixtures.FakeDeletionHistory
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaActionRequestFactory
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.FakeUsageStatisticsRepository
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.model.MediaAccess
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.ReviewSession
import fr.babeuloula.photoswipe.domain.service.ReviewWindowProvider
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * `GetDaySession` reprend la pile d'undo d'un jour depuis le journal : en revenant sur la pile ou
 * après la mort du process, avec de nouvelles instances de use cases sur le même stockage.
 */
class GetDaySessionTest {
    private val today = LocalDate.of(2026, 7, 15)
    private val clock = FixedClock.at(LocalDateTime.of(2026, 7, 15, 21, 0))
    private val gallery = FakeMediaSource((1L..4L).map { aMediaItem(id = it) })
    private val reviews = FakeReviewRepository()
    private val journal = FakeDecisionJournal(reviews)

    private fun getDaySession() = GetDaySession(journal, reviews, gallery, ReviewWindowProvider(clock))

    private suspend fun keep(
        id: Long,
        day: LocalDate = today,
    ) = KeepMedia(reviews, journal, clock)(aMediaItem(id = id), day)

    private suspend fun mark(id: Long) = MarkForDeletion(reviews, journal, clock)(aMediaItem(id = id), today)

    @Test
    fun `starts an empty session for a day without decisions`() =
        runTest {
            assertEquals(ReviewSession(), getDaySession()(today))
        }

    @Test
    fun `resumes the decisions of the day in order and undoes them after a restart`() =
        runTest {
            keep(1)
            mark(2)
            keep(3, day = today.minusDays(1))

            val session = getDaySession()(today)
            assertEquals(listOf(1L, 2L), session.decisions.map { it.media.id.value })

            val undo = UndoLastDecision(reviews, journal)
            val undone = undo(session) as UndoLastDecision.Result.Undone
            assertEquals(MediaId(2), undone.decision.media.id)
            undo(undone.session)

            assertEquals(listOf(MediaId(3)), reviews.entries.map { it.mediaId })
            assertEquals(ReviewSession(), getDaySession()(today))
        }

    @Test
    fun `leaves out a media that can no longer be read`() =
        runTest {
            keep(1)
            keep(2)
            gallery.remove(MediaId(1))

            assertEquals(listOf(2L), getDaySession()(today).decisions.map { it.media.id.value })
        }

    @Test
    fun `forgets the decisions of media moved to the trash`() =
        runTest {
            keep(1)
            mark(2)
            val request = PrepareTrash(reviews, gallery, FakeMediaActionRequestFactory())().single()

            RecordTrashResult(
                reviews,
                journal,
                FakeUsageStatisticsRepository(),
                FakeDeletionHistory(),
                ReviewWindowProvider(clock),
                clock,
            )(request, confirmed = true)

            assertEquals(listOf(1L), getDaySession()(today).decisions.map { it.media.id.value })
        }

    @Test
    fun `forgets the decisions of orphan media`() =
        runTest {
            keep(1)
            keep(2)
            gallery.remove(MediaId(2))

            CleanOrphanEntries(reviews, gallery, journal)(MediaAccess.FULL)

            assertEquals(listOf(MediaId(1)), journal.records.map { it.mediaId })
        }

    @Test
    fun `forgets the days that left the review window`() =
        runTest {
            keep(1, day = today.minusDays(7))
            keep(2, day = today.minusDays(6))

            getDaySession()(today)

            assertEquals(listOf(MediaId(2)), journal.records.map { it.mediaId })
            assertNull(journal.records.firstOrNull { it.day == today.minusDays(7) })
        }
}
