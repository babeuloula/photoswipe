package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaActionRequestFactory
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaActionRequestFactory.FakeMediaActionRequest
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaActionRequestFactory.Kind
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.KEPT
import fr.babeuloula.photoswipe.domain.model.ReviewEntry
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** `RequestFavorite` et `RecordFavoriteResult`, testés ensemble comme la pile les enchaîne (SPEC §2). */
class FavoritesTest {
    private val requestFavorite = RequestFavorite(FakeMediaActionRequestFactory())

    @Test
    fun `requests the gallery favorite of the single media just favorited`() {
        val request = requestFavorite(aMediaItem(id = 4))

        assertEquals(FakeMediaActionRequest(Kind.FAVORITE, setOf(MediaId(4))), request)
    }

    @Test
    fun `applies the favorite of every review year once the request is accepted`() =
        runTest {
            val repository =
                FakeReviewRepository(
                    listOf(favorite(1, reviewYear = 2025), favorite(1, reviewYear = 2026), favorite(2)),
                )

            RecordFavoriteResult(repository)(requestFavorite(aMediaItem(id = 1)), confirmed = true)

            assertEquals(listOf(2L), repository.entriesWithPendingFavorite().map { it.mediaId.value })
            assertEquals(List(3) { KEPT }, repository.entries.map { it.decision })
        }

    @Test
    fun `keeps the media kept with its favorite pending when the request is refused`() =
        runTest {
            val entries = listOf(favorite(1))
            val repository = FakeReviewRepository(entries)

            RecordFavoriteResult(repository)(requestFavorite(aMediaItem(id = 1)), confirmed = false)

            assertEquals(entries, repository.entries)
        }

    private fun favorite(
        id: Long,
        reviewYear: Int = 2026,
    ): ReviewEntry = aReviewEntry(mediaId = id, reviewYear = reviewYear, decision = KEPT, favoritePending = true)
}
