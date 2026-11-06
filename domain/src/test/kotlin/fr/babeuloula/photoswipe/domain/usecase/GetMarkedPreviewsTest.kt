package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.KEPT
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.MARKED_FOR_DELETION
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class GetMarkedPreviewsTest {
    private val repository = FakeReviewRepository()

    private suspend fun mark(
        id: Long,
        at: Long,
        reviewYear: Int = 2026,
    ) = repository.save(
        aReviewEntry(
            mediaId = id,
            reviewYear = reviewYear,
            decision = MARKED_FOR_DELETION,
            decidedAt = Instant.ofEpochSecond(at),
        ),
    )

    private fun previews(vararg galleryIds: Long) =
        GetMarkedPreviews(
            repository,
            FakeMediaSource(
                galleryIds.map {
                    aMediaItem(id = it)
                },
            ),
        )

    @Test
    fun `previews the most recently marked media first, once each, up to the limit`() =
        runTest {
            mark(1, at = 10)
            mark(2, at = 30)
            mark(3, at = 20)
            mark(2, at = 5, reviewYear = 2025)
            repository.save(aReviewEntry(mediaId = 4, decision = KEPT))

            val media = previews(1, 2, 3, 4)(limit = 2)

            assertEquals(listOf(MediaId(2), MediaId(3)), media.map { it.id })
        }

    @Test
    fun `leaves out marked media that are no longer in the gallery`() =
        runTest {
            mark(1, at = 10)
            mark(2, at = 20)

            assertEquals(listOf(MediaId(1)), previews(1)().map { it.id })
        }

    @Test
    fun `previews nothing when nothing is marked`() =
        runTest {
            assertTrue(previews(1)().isEmpty())
        }
}
