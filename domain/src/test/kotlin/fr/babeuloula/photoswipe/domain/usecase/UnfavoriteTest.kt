package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaActionRequestFactory
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaActionRequestFactory.Kind.UNFAVORITE
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.KEPT
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Retrait d'un favori depuis la pile (PS-124). */
class UnfavoriteTest {
    @Test
    fun `asks the gallery to remove a single media from its favorites`() {
        val request = RequestUnfavorite(FakeMediaActionRequestFactory())(aMediaItem(id = 4))

        assertEquals(FakeMediaActionRequestFactory.FakeMediaActionRequest(UNFAVORITE, setOf(MediaId(4))), request)
    }

    @Test
    fun `drops a pending favorite for every review year and keeps the media`() =
        runTest {
            val repository =
                FakeReviewRepository(
                    listOf(
                        aReviewEntry(mediaId = 4, reviewYear = 2026, decision = KEPT, favoritePending = true),
                        aReviewEntry(mediaId = 4, reviewYear = 2025, decision = KEPT, favoritePending = true),
                    ),
                )

            DropPendingFavorite(repository)(MediaId(4))

            assertTrue(repository.entries.none { it.favoritePending })
            assertTrue(repository.entries.all { it.decision == KEPT })
        }
}
