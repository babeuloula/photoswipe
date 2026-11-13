package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakeDecisionJournal
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.ReviewDecision
import fr.babeuloula.photoswipe.domain.model.ReviewSession
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

/** Une paire RAW + JPEG se décide comme une seule photo (PS-166). */
class RawPairDecisionsTest {
    private val day = LocalDate.of(2026, 7, 15)
    private val clock = FixedClock.at(LocalDateTime.of(2026, 7, 15, 21, 0))
    private val repository = FakeReviewRepository()
    private val journal = FakeDecisionJournal(repository)
    private val keepMedia = KeepMedia(repository, journal, clock)
    private val markForDeletion = MarkForDeletion(repository, journal, clock)
    private val favoriteMedia = FavoriteMedia(repository, journal, clock)
    private val undo = UndoLastDecision(repository, journal)
    private val raw = aMediaItem(id = 2, sizeBytes = 25_000_000)
    private val pair = aMediaItem(id = 1, sizeBytes = 3_000_000).copy(rawCompanion = raw)

    private suspend fun decisions(): Map<Long, ReviewDecision> =
        repository.entriesFor(2026, setOf(MediaId(1), MediaId(2))).associate { it.mediaId.value to it.decision }

    @Test
    fun `keeps the raw file with its jpeg`() =
        runTest {
            val recorded = keepMedia(pair, day)

            assertEquals(mapOf(1L to ReviewDecision.KEPT, 2L to ReviewDecision.KEPT), decisions())
            assertEquals(pair.id, recorded.last().media.id)
        }

    @Test
    fun `marks the raw file for deletion with its jpeg and its own size`() =
        runTest {
            markForDeletion.withCompanion(pair, day)

            assertEquals(
                mapOf(1L to ReviewDecision.MARKED_FOR_DELETION, 2L to ReviewDecision.MARKED_FOR_DELETION),
                decisions(),
            )
            assertEquals(
                25_000_000,
                repository.entries.single { it.mediaId == raw.id }.sizeBytes,
            )
        }

    @Test
    fun `keeps the raw file of a favorite jpeg without asking the gallery for it`() =
        runTest {
            favoriteMedia(pair, day)

            assertEquals(mapOf(1L to ReviewDecision.KEPT, 2L to ReviewDecision.KEPT), decisions())
            assertTrue(repository.entries.single { it.mediaId == pair.id }.favoritePending)
            assertTrue(!repository.entries.single { it.mediaId == raw.id }.favoritePending)
        }

    @Test
    fun `marks only the media itself without companion`() =
        runTest {
            markForDeletion.withCompanion(aMediaItem(id = 1), day)

            assertEquals(mapOf(1L to ReviewDecision.MARKED_FOR_DELETION), decisions())
        }

    @Test
    fun `undoes both files of a pair by undoing the whole card`() =
        runTest {
            var session = ReviewSession().record(markForDeletion.withCompanion(pair, day))

            repeat(2) {
                session = (undo(session) as UndoLastDecision.Result.Undone).session
            }

            assertEquals(emptyMap<Long, ReviewDecision>(), decisions())
        }
}
