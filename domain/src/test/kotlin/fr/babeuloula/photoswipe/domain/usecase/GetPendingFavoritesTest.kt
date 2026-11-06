package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.KEPT
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetPendingFavoritesTest {
    @Test
    fun `lists each media with a pending favorite once, whatever its review year`() =
        runTest {
            val repository =
                FakeReviewRepository(
                    listOf(
                        aReviewEntry(mediaId = 1, reviewYear = 2025, decision = KEPT, favoritePending = true),
                        aReviewEntry(mediaId = 1, reviewYear = 2026, decision = KEPT, favoritePending = true),
                        aReviewEntry(mediaId = 2, decision = KEPT),
                    ),
                )

            assertEquals(setOf(MediaId(1)), GetPendingFavorites(repository)())
        }
}
