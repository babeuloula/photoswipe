package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.KEPT
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.MARKED_FOR_DELETION
import fr.babeuloula.photoswipe.domain.model.ReviewEntry
import fr.babeuloula.photoswipe.domain.model.SessionDecision
import fr.babeuloula.photoswipe.domain.service.RecentYearsFirstOrdering
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime.NOON

class MarkForDeletionTest {
    private val today = LocalDate.of(2026, 7, 15)
    private val clock = FixedClock.at(LocalDateTime.of(2026, 7, 15, 21, 30))
    private val repository = FakeReviewRepository()
    private val markForDeletion = MarkForDeletion(repository, clock)

    @Test
    fun `records a marked for deletion decision with the size of the media`() =
        runTest {
            val media = aMediaItem(id = 42, sizeBytes = 3_500_000)

            markForDeletion(media, today)

            assertEquals(
                listOf(
                    ReviewEntry(
                        mediaId = media.id,
                        reviewYear = 2026,
                        decision = MARKED_FOR_DELETION,
                        sizeBytes = 3_500_000,
                        decidedAt = clock.instant(),
                    ),
                ),
                repository.entries,
            )
        }

    @Test
    fun `keeps the media pending deletion without sending it to the trash`() =
        runTest {
            markForDeletion(aMediaItem(id = 1), today)
            markForDeletion(aMediaItem(id = 2), LocalDate.of(2026, 7, 12))

            assertEquals(listOf(1L, 2L), repository.entriesMarkedForDeletion().map { it.mediaId.value })
        }

    @Test
    fun `replaces a previous decision of the same review year`() =
        runTest {
            val repository = FakeReviewRepository(listOf(aReviewEntry(mediaId = 1, reviewYear = 2026, decision = KEPT)))

            MarkForDeletion(repository, clock)(aMediaItem(id = 1), today)

            assertEquals(MARKED_FOR_DELETION, repository.entries.single().decision)
        }

    @Test
    fun `removes the marked media from the deck of the day`() =
        runTest {
            val media =
                listOf(1L, 2L).map {
                    aMediaItem(
                        id = it,
                        takenAt = LocalDateTime.of(2024, 7, 15, it.toInt(), 0).atZone(FixedClock.PARIS).toInstant(),
                    )
                }

            markForDeletion(media[0], today)

            val deck =
                GetDeckForDay(
                    SameDayMatcher(),
                    FakeMediaSource(media),
                    repository,
                    RecentYearsFirstOrdering(),
                    FixedClock.at(today.atTime(NOON)),
                )(
                    today,
                )
            assertEquals(listOf(media[1]), deck)
        }

    @Test
    fun `does not touch the decisions of other media`() =
        runTest {
            val kept = aReviewEntry(mediaId = 2, reviewYear = 2026, decision = KEPT)
            val repository = FakeReviewRepository(listOf(kept))

            MarkForDeletion(repository, clock)(aMediaItem(id = 1), today)

            assertEquals(listOf(kept), repository.entries.filter { it.mediaId.value == 2L })
        }

    @Test
    fun `returns the decision to stack for undo with the entry it replaced`() =
        runTest {
            val replaced = aReviewEntry(mediaId = 1, reviewYear = 2026, decision = KEPT)
            val repository = FakeReviewRepository(listOf(replaced))
            val media = aMediaItem(id = 1)

            val decision = MarkForDeletion(repository, clock)(media, LocalDate.of(2026, 7, 15))

            assertEquals(SessionDecision(media, LocalDate.of(2026, 7, 15), previous = replaced), decision)
        }

    @Test
    fun `returns a decision without previous entry for a media to sort`() =
        runTest {
            val media = aMediaItem(id = 1)

            val decision = markForDeletion(media, LocalDate.of(2026, 7, 15))

            assertEquals(SessionDecision(media, LocalDate.of(2026, 7, 15)), decision)
        }
}
