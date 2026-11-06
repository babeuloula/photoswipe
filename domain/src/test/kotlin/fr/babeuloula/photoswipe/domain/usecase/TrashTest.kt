package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakeDecisionJournal
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaActionRequestFactory
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.KEPT
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.MARKED_FOR_DELETION
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.TRASHED
import fr.babeuloula.photoswipe.domain.model.ReviewEntry
import fr.babeuloula.photoswipe.domain.model.ReviewSession
import fr.babeuloula.photoswipe.domain.port.MediaActionRequest
import fr.babeuloula.photoswipe.domain.service.ReviewWindowProvider
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/** `PrepareTrash` et `RecordTrashResult`, testés ensemble comme l'écran de confirmation les enchaîne. */
class TrashTest {
    private val gallery = FakeMediaSource((1L..5L).map { aMediaItem(id = it, sizeBytes = it * 1_000) })

    @Test
    fun `prepares no request when nothing is marked`() =
        runTest {
            val repository = FakeReviewRepository(listOf(aReviewEntry(mediaId = 1, decision = KEPT)))

            assertEquals(emptyList<MediaActionRequest>(), prepareTrash(repository)())
        }

    @Test
    fun `splits the marked media into several batches`() =
        runTest {
            val repository = FakeReviewRepository((1L..5L).map { marked(it) })

            val requests = prepareTrash(repository, batchSize = 2)()

            assertEquals(listOf(ids(1, 2), ids(3, 4), ids(5)), requests.map { it.mediaIds })
        }

    @Test
    fun `requests every pending deletion once whatever its review year`() =
        runTest {
            val repository =
                FakeReviewRepository(
                    listOf(marked(1, reviewYear = 2025), marked(1, reviewYear = 2026), marked(2, reviewYear = 2025)),
                )

            assertEquals(listOf(ids(1, 2)), prepareTrash(repository)().map { it.mediaIds })
        }

    @Test
    fun `leaves out a marked media that can no longer be read`() =
        runTest {
            val repository = FakeReviewRepository(listOf(marked(1), marked(2)))
            gallery.remove(MediaId(2))

            assertEquals(listOf(ids(1)), prepareTrash(repository)().map { it.mediaIds })
            assertEquals(2, repository.entriesMarkedForDeletion().size)
        }

    @Test
    fun `moves the media of a confirmed request to the trash`() =
        runTest {
            val repository =
                FakeReviewRepository(listOf(marked(1), marked(2), aReviewEntry(mediaId = 3, decision = KEPT)))
            val request = prepareTrash(repository)().single()

            RecordTrashResult(repository, FakeDecisionJournal(repository))(request, confirmed = true)

            assertEquals(listOf(TRASHED, TRASHED, KEPT), repository.entries.map { it.decision })
        }

    @Test
    fun `keeps the markings when the request is refused`() =
        runTest {
            val entries = listOf(marked(1), marked(2))
            val repository = FakeReviewRepository(entries)

            RecordTrashResult(
                repository,
                FakeDecisionJournal(repository),
            )(prepareTrash(repository)().single(), confirmed = false)

            assertEquals(entries, repository.entries)
        }

    @Test
    fun `trashes only the batches confirmed by the user`() =
        runTest {
            val repository = FakeReviewRepository((1L..5L).map { marked(it) })
            val requests = prepareTrash(repository, batchSize = 2)()

            requests.forEachIndexed { index, request ->
                RecordTrashResult(repository, FakeDecisionJournal(repository))(
                    request,
                    confirmed =
                        index != 1,
                )
            }

            assertEquals(
                listOf(TRASHED, TRASHED, MARKED_FOR_DELETION, MARKED_FOR_DELETION, TRASHED),
                repository.entries.map { it.decision },
            )
        }

    @Test
    fun `requests exactly the media and size announced by the summary`() =
        runTest {
            val repository =
                FakeReviewRepository(
                    listOf(
                        marked(1),
                        marked(1, reviewYear = 2025),
                        marked(2, reviewYear = 2025),
                        marked(4),
                        aReviewEntry(mediaId = 5, decision = KEPT),
                    ),
                )

            val clock = FixedClock.at(LocalDateTime.of(2026, 7, 15, 21, 0))
            val summary =
                GetSessionSummary(repository, FakeDecisionJournal(repository), ReviewWindowProvider(clock), clock)()
            val requested = prepareTrash(repository, batchSize = 2)().flatMap { it.mediaIds }

            assertEquals(summary.markedForDeletionCount, requested.size)
            assertEquals(summary.estimatedFreedBytes, gallery.mediaByIds(requested.toSet()).sumOf { it.sizeBytes })
        }

    private fun prepareTrash(
        repository: FakeReviewRepository,
        batchSize: Int = FakeMediaActionRequestFactory.DEFAULT_BATCH_SIZE,
    ): PrepareTrash = PrepareTrash(repository, gallery, FakeMediaActionRequestFactory(batchSize))

    private fun marked(
        id: Long,
        reviewYear: Int = 2026,
    ): ReviewEntry =
        aReviewEntry(
            mediaId = id,
            reviewYear = reviewYear,
            decision = MARKED_FOR_DELETION,
            sizeBytes =
                id * 1_000,
        )

    private fun ids(vararg ids: Long): Set<MediaId> = ids.mapTo(LinkedHashSet()) { MediaId(it) }
}
