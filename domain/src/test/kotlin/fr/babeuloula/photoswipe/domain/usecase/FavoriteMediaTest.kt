package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.KEPT
import fr.babeuloula.photoswipe.domain.model.ReviewEntry
import fr.babeuloula.photoswipe.domain.model.ReviewSession
import fr.babeuloula.photoswipe.domain.model.SessionDecision
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class FavoriteMediaTest {
    private val today = LocalDate.of(2026, 7, 15)
    private val clock = FixedClock.at(LocalDateTime.of(2026, 7, 15, 21, 30))
    private val repository = FakeReviewRepository()
    private val favoriteMedia = FavoriteMedia(repository, clock)

    @Test
    fun `keeps the media with a pending favorite request`() =
        runTest {
            val media = aMediaItem(id = 42, sizeBytes = 3_500_000)

            val decision = favoriteMedia(media, today)

            assertEquals(
                listOf(
                    ReviewEntry(
                        mediaId = media.id,
                        reviewYear = 2026,
                        decision = KEPT,
                        sizeBytes = 3_500_000,
                        decidedAt = clock.instant(),
                        favoritePending = true,
                    ),
                ),
                repository.entries,
            )
            assertEquals(SessionDecision(media, today), decision)
        }

    @Test
    fun `leaves the favorite to apply later at the end of the session`() =
        runTest {
            favoriteMedia(aMediaItem(id = 1), today)
            favoriteMedia(aMediaItem(id = 2), LocalDate.of(2026, 7, 12))

            assertEquals(listOf(1L, 2L), repository.entriesWithPendingFavorite().map { it.mediaId.value })
        }

    @Test
    fun `adds the favorite to a media already kept this year`() =
        runTest {
            val kept = aReviewEntry(mediaId = 1, reviewYear = 2026, decision = KEPT)
            val repository = FakeReviewRepository(listOf(kept))
            val media = aMediaItem(id = 1)

            val decision = FavoriteMedia(repository, clock)(media, today)

            assertEquals(listOf(true), repository.entries.map { it.favoritePending })
            assertEquals(SessionDecision(media, today, previous = kept), decision)
        }

    @Test
    fun `makes the media to sort again when the favorite is undone`() =
        runTest {
            val session = ReviewSession().record(favoriteMedia(aMediaItem(id = 1), today))

            UndoLastDecision(repository)(session)

            assertEquals(emptyList<ReviewEntry>(), repository.entries)
            assertEquals(emptyList<ReviewEntry>(), repository.entriesWithPendingFavorite())
        }

    @Test
    fun `restores the plain kept decision when the favorite on a kept media is undone`() =
        runTest {
            val kept = aReviewEntry(mediaId = 1, reviewYear = 2026, decision = KEPT)
            val repository = FakeReviewRepository(listOf(kept))
            val session = ReviewSession().record(FavoriteMedia(repository, clock)(aMediaItem(id = 1), today))

            UndoLastDecision(repository)(session)

            assertEquals(listOf(kept), repository.entries)
        }
}
