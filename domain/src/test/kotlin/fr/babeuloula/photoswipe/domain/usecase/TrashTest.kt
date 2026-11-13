package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakeActivityHistory
import fr.babeuloula.photoswipe.domain.fixtures.FakeDecisionJournal
import fr.babeuloula.photoswipe.domain.fixtures.FakeDeletionHistory
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaActionRequestFactory
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.FakeUsageStatisticsRepository
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.fixtures.aConfirmedDeletion
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.CatchUpWindow
import fr.babeuloula.photoswipe.domain.model.ConfirmedDeletion
import fr.babeuloula.photoswipe.domain.model.DailyActivity
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
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

/** `PrepareTrash` et `RecordTrashResult`, testés ensemble comme l'écran de confirmation les enchaîne. */
class TrashTest {
    private val clock = FixedClock.at(LocalDateTime.of(2026, 7, 15, 21, 0))
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

            recordTrashResult(repository)(request, confirmed = true)

            assertEquals(listOf(TRASHED, TRASHED, KEPT), repository.entries.map { it.decision })
        }

    @Test
    fun `adds a confirmed deletion to the statistics, a media marked for two years counting once`() =
        runTest {
            val repository =
                FakeReviewRepository(
                    listOf(
                        marked(1).copy(sizeBytes = 1_000),
                        marked(1, reviewYear = 2025).copy(sizeBytes = 1_000),
                        marked(2).copy(sizeBytes = 500),
                    ),
                )
            val statistics = FakeUsageStatisticsRepository()

            recordTrashResult(repository, statistics)(
                prepareTrash(repository)().single(),
                confirmed = true,
            )

            assertEquals(2, statistics.current.deletedCount)
            assertEquals(1_500L, statistics.current.freedBytes)
        }

    @Test
    fun `adds a confirmed deletion to the activity of the day it is confirmed`() =
        runTest {
            val repository =
                FakeReviewRepository(listOf(marked(1).copy(sizeBytes = 1_000), marked(2).copy(sizeBytes = 500)))
            val activity = FakeActivityHistory()

            recordTrashResult(repository, activity = activity)(prepareTrash(repository)().single(), confirmed = true)

            assertEquals(
                listOf(DailyActivity(LocalDate.of(2026, 7, 15), trashedCount = 2, freedBytes = 1_500)),
                activity.days,
            )
        }

    @Test
    fun `leaves the activity untouched when the deletion is refused`() =
        runTest {
            val repository = FakeReviewRepository(listOf(marked(1)))
            val activity = FakeActivityHistory()

            recordTrashResult(repository, activity = activity)(prepareTrash(repository)().single(), confirmed = false)

            assertEquals(emptyList<DailyActivity>(), activity.days)
        }

    @Test
    fun `does not count a refused deletion in the statistics`() =
        runTest {
            val repository = FakeReviewRepository(listOf(marked(1)))
            val statistics = FakeUsageStatisticsRepository()

            recordTrashResult(repository, statistics)(
                prepareTrash(repository)().single(),
                confirmed = false,
            )

            assertEquals(0, statistics.current.deletedCount)
        }

    @Test
    fun `records each confirmed marking in the history with the day of the window that made it`() =
        runTest {
            val repository = FakeReviewRepository()
            val journal = FakeDecisionJournal(repository)
            val markForDeletion = MarkForDeletion(repository, journal, clock)
            val day = LocalDate.of(2026, 7, 13)
            markForDeletion(gallery.mediaByIds(ids(1)).single(), day)
            repository.save(marked(2, reviewYear = 2025))
            val history = FakeDeletionHistory()

            recordTrashResult(repository, history = history, journal = journal)(
                prepareTrash(repository)().single(),
                confirmed = true,
            )

            assertEquals(
                listOf(
                    ConfirmedDeletion(MediaId(1), day, 1_000, clock.instant()),
                    ConfirmedDeletion(MediaId(2), null, 2_000, clock.instant()),
                ),
                history.deletions,
            )
        }

    @Test
    fun `forgets the deletions confirmed more than 30 days ago, already emptied from the trash`() =
        runTest {
            val repository = FakeReviewRepository(listOf(marked(1)))
            val old = aConfirmedDeletion(mediaId = 9, confirmedAt = Instant.parse("2026-06-10T10:00:00Z"))
            // Sortie de la fenêtre de 7 jours, mais encore dans la corbeille (PS-168).
            val recent = aConfirmedDeletion(mediaId = 8, confirmedAt = Instant.parse("2026-07-01T10:00:00Z"))
            val history = FakeDeletionHistory(listOf(old, recent))

            recordTrashResult(repository, history = history)(prepareTrash(repository)().single(), confirmed = true)

            assertEquals(listOf(MediaId(8), MediaId(1)), history.deletions.map { it.mediaId })
        }

    @Test
    fun `keeps the deletions of the whole month in the history on a month-long catch-up window`() =
        runTest {
            val repository = FakeReviewRepository(listOf(marked(1)))
            val old = aConfirmedDeletion(mediaId = 9, confirmedAt = Instant.parse("2026-07-01T10:00:00Z"))
            val history = FakeDeletionHistory(listOf(old))

            recordTrashResult(repository, history = history, window = CatchUpWindow.MONTH)(
                prepareTrash(repository)().single(),
                confirmed = true,
            )

            assertEquals(listOf(MediaId(9), MediaId(1)), history.deletions.map { it.mediaId })
        }

    @Test
    fun `records nothing in the history when the request is refused`() =
        runTest {
            val repository = FakeReviewRepository(listOf(marked(1)))
            val history = FakeDeletionHistory()

            recordTrashResult(repository, history = history)(prepareTrash(repository)().single(), confirmed = false)

            assertEquals(emptyList<ConfirmedDeletion>(), history.deletions)
        }

    @Test
    fun `keeps the markings when the request is refused`() =
        runTest {
            val entries = listOf(marked(1), marked(2))
            val repository = FakeReviewRepository(entries)

            recordTrashResult(repository)(prepareTrash(repository)().single(), confirmed = false)

            assertEquals(entries, repository.entries)
        }

    @Test
    fun `trashes only the batches confirmed by the user`() =
        runTest {
            val repository = FakeReviewRepository((1L..5L).map { marked(it) })
            val requests = prepareTrash(repository, batchSize = 2)()

            requests.forEachIndexed { index, request ->
                recordTrashResult(repository)(
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

            val summary =
                GetSessionSummary(
                    repository,
                    FakeDecisionJournal(repository),
                    FakeDeletionHistory(),
                    ReviewWindowProvider(clock),
                    clock,
                )()
            val requested = prepareTrash(repository, batchSize = 2)().flatMap { it.mediaIds }

            assertEquals(summary.markedForDeletionCount, requested.size)
            assertEquals(summary.estimatedFreedBytes, gallery.mediaByIds(requested.toSet()).sumOf { it.sizeBytes })
        }

    private fun recordTrashResult(
        repository: FakeReviewRepository,
        statistics: FakeUsageStatisticsRepository = FakeUsageStatisticsRepository(),
        history: FakeDeletionHistory = FakeDeletionHistory(),
        journal: FakeDecisionJournal = FakeDecisionJournal(repository),
        activity: FakeActivityHistory = FakeActivityHistory(),
        window: CatchUpWindow = CatchUpWindow.DEFAULT,
    ): RecordTrashResult =
        RecordTrashResult(
            repository,
            journal,
            statistics,
            history,
            activity,
            ReviewWindowProvider(clock, window),
            clock,
        )

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
