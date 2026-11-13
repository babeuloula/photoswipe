package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakeActivityHistory
import fr.babeuloula.photoswipe.domain.fixtures.FakeDecisionJournal
import fr.babeuloula.photoswipe.domain.fixtures.FakeDeletionHistory
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaActionRequestFactory
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaActionRequestFactory.FakeMediaActionRequest
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaActionRequestFactory.Kind
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.FakeSystemTrash
import fr.babeuloula.photoswipe.domain.fixtures.FakeUsageStatisticsRepository
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.fixtures.aConfirmedDeletion
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.DailyActivity
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.KEPT
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.TRASHED
import fr.babeuloula.photoswipe.domain.model.Trash
import fr.babeuloula.photoswipe.domain.model.TrashItem
import fr.babeuloula.photoswipe.domain.model.UsageStatistics
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

/** Page « Corbeille » (PS-168) : `GetTrash`, `RequestRestore`, `RequestPermanentDeletion` et `RecordRestoreResult`. */
class TrashPageTest {
    private val clock = FixedClock.at(LocalDateTime.of(2026, 7, 15, 21, 0))
    private val july10 = Instant.parse("2026-07-10T10:00:00Z")
    private val july14 = Instant.parse("2026-07-14T10:00:00Z")
    private val trash = FakeSystemTrash()

    @Test
    fun `shows nothing when PhotoSwipe deleted nothing`() =
        runTest {
            trash.add(aMediaItem(id = 1))

            assertEquals(Trash(), GetTrash(FakeDeletionHistory(), trash)())
        }

    @Test
    fun `shows the media deleted by PhotoSwipe still in the trash, soonest purge first`() =
        runTest {
            val history =
                FakeDeletionHistory(
                    listOf(
                        aConfirmedDeletion(mediaId = 1, confirmedAt = july14),
                        aConfirmedDeletion(mediaId = 2, confirmedAt = july10),
                    ),
                )
            trash.add(aMediaItem(id = 1, sizeBytes = 1_000))
            trash.add(aMediaItem(id = 2, sizeBytes = 2_000))
            // Envoyé à la corbeille depuis la galerie : ce n'est pas une suppression de PhotoSwipe.
            trash.add(aMediaItem(id = 3))

            val result = GetTrash(history, trash)()

            assertEquals(
                listOf(
                    TrashItem(aMediaItem(id = 2, sizeBytes = 2_000), july10, july10.plusSeconds(THIRTY_DAYS)),
                    TrashItem(aMediaItem(id = 1, sizeBytes = 1_000), july14, july14.plusSeconds(THIRTY_DAYS)),
                ),
                result.items,
            )
            assertEquals(3_000L, result.occupiedBytes)
        }

    @Test
    fun `prefers the purge date and size read in the trash`() =
        runTest {
            val purgeAt = Instant.parse("2026-08-01T00:00:00Z")
            val history =
                FakeDeletionHistory(listOf(aConfirmedDeletion(mediaId = 1, sizeBytes = 500, confirmedAt = july14)))
            trash.add(aMediaItem(id = 1, sizeBytes = 0), purgeAt)

            val item = GetTrash(history, trash)().items.single()

            assertEquals(purgeAt, item.purgeAt)
            assertEquals(500L, item.media.sizeBytes)
        }

    @Test
    fun `leaves out without error a media already emptied from the trash or restored elsewhere`() =
        runTest {
            val history =
                FakeDeletionHistory(
                    listOf(
                        aConfirmedDeletion(mediaId = 1, confirmedAt = july14),
                        aConfirmedDeletion(mediaId = 2, confirmedAt = july14),
                    ),
                )
            trash.add(aMediaItem(id = 2))

            assertEquals(listOf(MediaId(2)), GetTrash(history, trash)().items.map { it.media.id })
        }

    @Test
    fun `lists a media marked for two review years once, with its latest confirmation`() =
        runTest {
            val history =
                FakeDeletionHistory(
                    listOf(
                        aConfirmedDeletion(mediaId = 1, confirmedAt = july10),
                        aConfirmedDeletion(mediaId = 1, confirmedAt = july14),
                    ),
                )
            trash.add(aMediaItem(id = 1))

            assertEquals(listOf(july14), GetTrash(history, trash)().items.map { it.confirmedAt })
        }

    @Test
    fun `prepares restore and permanent deletion requests`() {
        val factory = FakeMediaActionRequestFactory()
        val media = listOf(aMediaItem(id = 1), aMediaItem(id = 2))

        assertEquals(listOf(FakeMediaActionRequest(Kind.RESTORE, ids(1, 2))), RequestRestore(factory)(media))
        assertEquals(listOf(FakeMediaActionRequest(Kind.DELETE, ids(1, 2))), RequestPermanentDeletion(factory)(media))
    }

    @Test
    fun `makes a restored media a memory to sort again and corrects the statistics and the activity`() =
        runTest {
            val history =
                FakeDeletionHistory(
                    listOf(
                        aConfirmedDeletion(mediaId = 1, sizeBytes = 1_000, confirmedAt = july14),
                        aConfirmedDeletion(mediaId = 1, sizeBytes = 1_000, confirmedAt = july14),
                        aConfirmedDeletion(mediaId = 2, sizeBytes = 2_000, confirmedAt = july10),
                    ),
                )
            val repository =
                FakeReviewRepository(
                    listOf(
                        aReviewEntry(mediaId = 1, decision = TRASHED),
                        aReviewEntry(mediaId = 1, reviewYear = 2025, decision = TRASHED),
                        aReviewEntry(mediaId = 2, decision = TRASHED),
                    ),
                )
            // Journal à part : il écrit ses décisions dans son propre dépôt.
            val journal = FakeDecisionJournal()
            journal.record(LocalDate.of(2026, 7, 15), aReviewEntry(mediaId = 1, decision = KEPT), previous = null)
            val statistics = FakeUsageStatisticsRepository()
            statistics.recordDeletion(2, 3_000)
            val activity = FakeActivityHistory()
            activity.recordTrashed(LocalDate.of(2026, 7, 14), 1, 1_000)
            activity.recordTrashed(LocalDate.of(2026, 7, 10), 1, 2_000)
            val record = RecordRestoreResult(history, repository, journal, statistics, activity, clock)

            record(FakeMediaActionRequest(Kind.RESTORE, ids(1)), confirmed = true)

            assertEquals(listOf(MediaId(2)), history.deletions.map { it.mediaId })
            assertEquals(listOf(MediaId(2)), repository.entries.map { it.mediaId })
            assertTrue(journal.records.isEmpty())
            assertEquals(
                UsageStatistics(statistics.current.since, deletedCount = 1, freedBytes = 2_000),
                statistics.current,
            )
            assertEquals(
                listOf(
                    DailyActivity(LocalDate.of(2026, 7, 10), trashedCount = 1, freedBytes = 2_000),
                    DailyActivity(LocalDate.of(2026, 7, 14)),
                ),
                activity.days,
            )
        }

    @Test
    fun `changes nothing when the restore is refused`() =
        runTest {
            val history = FakeDeletionHistory(listOf(aConfirmedDeletion(mediaId = 1)))
            val repository = FakeReviewRepository(listOf(aReviewEntry(mediaId = 1, decision = TRASHED)))
            val statistics = FakeUsageStatisticsRepository()
            statistics.recordDeletion(1, 1_000)
            val record =
                RecordRestoreResult(
                    history,
                    repository,
                    FakeDecisionJournal(repository),
                    statistics,
                    FakeActivityHistory(),
                    clock,
                )

            record(FakeMediaActionRequest(Kind.RESTORE, ids(1)), confirmed = false)

            assertEquals(1, history.deletions.size)
            assertEquals(1, repository.entries.size)
            assertEquals(1, statistics.current.deletedCount)
        }

    private fun ids(vararg values: Long): Set<MediaId> = values.mapTo(LinkedHashSet()) { MediaId(it) }

    private companion object {
        const val THIRTY_DAYS = 30L * 24 * 60 * 60
    }
}
