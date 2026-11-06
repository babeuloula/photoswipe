package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakeDecisionJournal
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.KEPT
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.MARKED_FOR_DELETION
import fr.babeuloula.photoswipe.domain.model.ReviewSession
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Un média gardé (ou mis en favori) une année quitte les suppressions en attente des autres années
 * (PS-113) : une décision vaut pour un couple (média, année), mais la corbeille agit par média.
 */
class KeepClearsOtherMarkingsTest {
    private val clock = FixedClock.at(LocalDateTime.of(2026, 7, 15, 21, 30))
    private val today = LocalDate.of(2026, 7, 15)

    /** Photo prise le 15 juillet 2020 à midi : un souvenir du 15 juillet chaque année. */
    private val photo = aMediaItem(id = 7, takenAt = LocalDateTime.of(2020, 7, 15, 12, 0).toInstant(ZoneOffset.UTC))

    private val repository =
        FakeReviewRepository(listOf(aReviewEntry(mediaId = 7, reviewYear = 2025, decision = MARKED_FOR_DELETION)))
    private val journal = FakeDecisionJournal(repository)

    @Test
    fun `a media marked in 2025 then kept in 2026 is no longer marked for deletion`() =
        runTest {
            KeepMedia(repository, journal, clock)(photo, today)

            assertTrue(repository.entriesMarkedForDeletion().isEmpty())
            assertEquals(
                setOf(2025 to KEPT, 2026 to KEPT),
                repository.entries.map { it.reviewYear to it.decision }.toSet(),
            )
        }

    @Test
    fun `a favorite also clears the markings of the other years`() =
        runTest {
            FavoriteMedia(repository, journal, clock)(photo, today)

            assertTrue(repository.entriesMarkedForDeletion().isEmpty())
        }

    @Test
    fun `the day decision comes last, so undo brings the card back from its own side`() =
        runTest {
            val made = KeepMedia(repository, journal, clock)(photo, today)

            assertEquals(listOf(2025, 2026), made.map { it.reviewYear })
            assertEquals(LocalDate.of(2025, 7, 15), made.first().day)
        }

    @Test
    fun `undoing every decision of the card marks it again for the other year`() =
        runTest {
            var session = ReviewSession().record(KeepMedia(repository, journal, clock)(photo, today))
            val undo = UndoLastDecision(repository, journal)

            while (!session.isEmpty) {
                session = (undo(session) as UndoLastDecision.Result.Undone).session
            }

            assertEquals(listOf(2025), repository.entriesMarkedForDeletion().map { it.reviewYear })
            assertTrue(repository.entries.none { it.reviewYear == 2026 })
        }

    @Test
    fun `marking leaves the markings of the other years untouched`() =
        runTest {
            MarkForDeletion(repository, journal, clock)(photo, today)

            assertEquals(listOf(2025, 2026), repository.entriesMarkedForDeletion().map { it.reviewYear }.sorted())
        }
}
