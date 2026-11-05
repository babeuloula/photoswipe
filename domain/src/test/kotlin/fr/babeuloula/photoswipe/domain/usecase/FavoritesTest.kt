package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakeDecisionJournal
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaActionRequestFactory
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaActionRequestFactory.FakeMediaActionRequest
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaActionRequestFactory.Kind
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.KEPT
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.MARKED_FOR_DELETION
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.TRASHED
import fr.babeuloula.photoswipe.domain.model.ReviewEntry
import fr.babeuloula.photoswipe.domain.port.MediaActionRequest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** `PrepareFavorites` et `RecordFavoriteResult`, testés ensemble comme l'écran de confirmation les enchaîne. */
class FavoritesTest {
    private val gallery = FakeMediaSource((1L..5L).map { aMediaItem(id = it) })

    @Test
    fun `prepares no request when no favorite is pending`() =
        runTest {
            val repository = FakeReviewRepository(listOf(aReviewEntry(mediaId = 1, decision = KEPT)))

            assertEquals(emptyList<MediaActionRequest>(), prepareFavorites(repository)())
        }

    @Test
    fun `splits the pending favorites into several favorite requests`() =
        runTest {
            val repository = FakeReviewRepository((1L..5L).map { favorite(it) })

            val requests = prepareFavorites(repository, batchSize = 2)()

            assertEquals(listOf(ids(1, 2), ids(3, 4), ids(5)), requests.map { it.mediaIds })
            assertEquals(setOf(Kind.FAVORITE), requests.mapTo(HashSet()) { (it as FakeMediaActionRequest).kind })
        }

    @Test
    fun `requests every pending favorite once whatever its review year`() =
        runTest {
            val repository =
                FakeReviewRepository(
                    listOf(
                        favorite(1, reviewYear = 2025),
                        favorite(1, reviewYear = 2026),
                        favorite(2, reviewYear = 2025),
                    ),
                )

            assertEquals(listOf(ids(1, 2)), prepareFavorites(repository)().map { it.mediaIds })
        }

    @Test
    fun `leaves out a favorite that can no longer be read`() =
        runTest {
            val repository = FakeReviewRepository(listOf(favorite(1), favorite(2)))
            gallery.remove(MediaId(2))

            assertEquals(listOf(ids(1)), prepareFavorites(repository)().map { it.mediaIds })
            assertEquals(2, repository.entriesWithPendingFavorite().size)
        }

    @Test
    fun `keeps favorites and deletions in separate requests`() =
        runTest {
            val repository =
                FakeReviewRepository(
                    listOf(favorite(1), aReviewEntry(mediaId = 2, decision = MARKED_FOR_DELETION), favorite(3)),
                )
            val factory = FakeMediaActionRequestFactory()

            val trash = PrepareTrash(repository, gallery, factory)()
            val favorites = PrepareFavorites(repository, gallery, factory)()

            assertEquals(listOf(FakeMediaActionRequest(Kind.TRASH, ids(2))), trash)
            assertEquals(listOf(FakeMediaActionRequest(Kind.FAVORITE, ids(1, 3))), favorites)
        }

    @Test
    fun `leaves out a favorite whose media was just moved to the trash`() =
        runTest {
            val repository =
                FakeReviewRepository(
                    listOf(
                        favorite(1, reviewYear = 2025),
                        aReviewEntry(mediaId = 1, reviewYear = 2026, decision = MARKED_FOR_DELETION),
                        favorite(2),
                    ),
                )
            RecordTrashResult(
                repository,
                FakeDecisionJournal(repository),
            )(PrepareTrash(repository, gallery, FakeMediaActionRequestFactory())().single(), true)
            gallery.remove(MediaId(1))

            assertEquals(listOf(ids(2)), prepareFavorites(repository)().map { it.mediaIds })
            assertEquals(
                TRASHED,
                repository.entries.single { it.reviewYear == 2026 && it.mediaId == MediaId(1) }.decision,
            )
        }

    @Test
    fun `applies the favorites of an accepted request`() =
        runTest {
            val repository =
                FakeReviewRepository(
                    listOf(favorite(1, reviewYear = 2025), favorite(1, reviewYear = 2026), favorite(2)),
                )
            val request = prepareFavorites(repository)().single()

            RecordFavoriteResult(repository)(request, confirmed = true)

            assertEquals(emptyList<ReviewEntry>(), repository.entriesWithPendingFavorite())
            assertEquals(List(3) { KEPT }, repository.entries.map { it.decision })
        }

    @Test
    fun `keeps the favorites pending when the request is refused`() =
        runTest {
            val entries = listOf(favorite(1), favorite(2))
            val repository = FakeReviewRepository(entries)

            RecordFavoriteResult(repository)(prepareFavorites(repository)().single(), confirmed = false)

            assertEquals(entries, repository.entries)
        }

    @Test
    fun `applies only the batches accepted by the user`() =
        runTest {
            val repository = FakeReviewRepository((1L..5L).map { favorite(it) })
            val requests = prepareFavorites(repository, batchSize = 2)()

            requests.forEachIndexed { index, request ->
                RecordFavoriteResult(
                    repository,
                )(request, confirmed = index != 1)
            }

            assertEquals(listOf(3L, 4L), repository.entriesWithPendingFavorite().map { it.mediaId.value })
        }

    private fun prepareFavorites(
        repository: FakeReviewRepository,
        batchSize: Int = FakeMediaActionRequestFactory.DEFAULT_BATCH_SIZE,
    ): PrepareFavorites = PrepareFavorites(repository, gallery, FakeMediaActionRequestFactory(batchSize))

    private fun favorite(
        id: Long,
        reviewYear: Int = 2026,
    ): ReviewEntry = aReviewEntry(mediaId = id, reviewYear = reviewYear, decision = KEPT, favoritePending = true)

    private fun ids(vararg ids: Long): Set<MediaId> = ids.mapTo(LinkedHashSet()) { MediaId(it) }
}
