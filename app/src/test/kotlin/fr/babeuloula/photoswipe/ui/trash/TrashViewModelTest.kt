package fr.babeuloula.photoswipe.ui.trash

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import fr.babeuloula.photoswipe.MainDispatcherRule
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
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.ReviewDecision
import fr.babeuloula.photoswipe.domain.port.MediaActionRequest
import fr.babeuloula.photoswipe.domain.usecase.GetTrash
import fr.babeuloula.photoswipe.domain.usecase.RecordRestoreResult
import fr.babeuloula.photoswipe.domain.usecase.RequestPermanentDeletion
import fr.babeuloula.photoswipe.domain.usecase.RequestRestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.time.Instant
import java.time.LocalDateTime

/** Robolectric : les dialogues système passent par un vrai `IntentSender`, jamais lancé ici. */
@RunWith(RobolectricTestRunner::class)
class TrashViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val clock = FixedClock.at(LocalDateTime.of(2026, 7, 15, 21, 0))
    private val confirmedAt = Instant.parse("2026-07-14T10:00:00Z")
    private val history =
        FakeDeletionHistory(
            listOf(
                aConfirmedDeletion(mediaId = 1, sizeBytes = 1_000, confirmedAt = confirmedAt),
                aConfirmedDeletion(mediaId = 2, sizeBytes = 2_000, confirmedAt = confirmedAt),
            ),
        )
    private val systemTrash =
        FakeSystemTrash().apply {
            add(aMediaItem(id = 1, sizeBytes = 1_000))
            add(aMediaItem(id = 2, sizeBytes = 2_000))
        }
    private val reviews = FakeReviewRepository(listOf(aReviewEntry(mediaId = 1, decision = ReviewDecision.TRASHED)))
    private val statistics = FakeUsageStatisticsRepository()

    /** Demandes passées au pont des dialogues, dans l'ordre. */
    private val launched = mutableListOf<MediaActionRequest>()
    private var mediaManagementGranted = false
    private val intentSender: IntentSender =
        PendingIntent
            .getActivity(RuntimeEnvironment.getApplication(), 0, Intent(), PendingIntent.FLAG_IMMUTABLE)
            .intentSender

    private fun viewModel(): TrashViewModel {
        val factory = FakeMediaActionRequestFactory()
        return TrashViewModel(
            TrashActions(
                getTrash = GetTrash(history, systemTrash),
                requestRestore = RequestRestore(factory),
                recordRestoreResult =
                    RecordRestoreResult(
                        history,
                        reviews,
                        FakeDecisionJournal(),
                        statistics,
                        FakeActivityHistory(),
                        clock,
                    ),
                requestPermanentDeletion = RequestPermanentDeletion(factory),
            ),
            mediaActionIntents = {
                launched += it
                intentSender
            },
            mediaManagement = { mediaManagementGranted },
        )
    }

    private fun TestScope.collectEffects(viewModel: TrashViewModel): List<TrashEffect> {
        val effects = mutableListOf<TrashEffect>()
        backgroundScope.launch(
            UnconfinedTestDispatcher(testScheduler),
        ) { viewModel.uiEffects.collect { effects += it } }
        return effects
    }

    private fun TrashViewModel.loaded() = uiState.value as TrashUiState.Loaded

    @Test
    fun `lists the media deleted by PhotoSwipe still in the trash and the space they take`() =
        runTest {
            val state = viewModel().loaded()

            assertEquals(listOf(MediaId(1), MediaId(2)), state.trash.items.map { it.media.id })
            assertEquals(3_000L, state.trash.occupiedBytes)
        }

    @Test
    fun `restores a media, which leaves the list and becomes a memory to sort again`() =
        runTest {
            val viewModel = viewModel()
            val effects = collectEffects(viewModel)

            viewModel.onEvent(TrashEvent.Restore(MediaId(1)))
            assertTrue(viewModel.loaded().busy)
            systemTrash.remove(listOf(MediaId(1)))
            viewModel.onEvent(TrashEvent.SystemRequestAnswered(confirmed = true))

            assertEquals(listOf(FakeMediaActionRequest(Kind.RESTORE, setOf(MediaId(1)))), launched)
            assertEquals(
                listOf(MediaId(2)),
                viewModel
                    .loaded()
                    .trash.items
                    .map { it.media.id },
            )
            assertTrue(reviews.entries.isEmpty())
            assertEquals(listOf(MediaId(2)), history.deletions.map { it.mediaId })
            assertEquals(TrashEffect.Restored, effects.last())
        }

    @Test
    fun `keeps everything when the restore is refused`() =
        runTest {
            val viewModel = viewModel()
            val effects = collectEffects(viewModel)

            viewModel.onEvent(TrashEvent.Restore(MediaId(1)))
            viewModel.onEvent(TrashEvent.SystemRequestAnswered(confirmed = false))

            assertEquals(
                2,
                viewModel
                    .loaded()
                    .trash.items.size,
            )
            assertEquals(2, history.deletions.size)
            assertEquals(TrashEffect.NothingChanged, effects.last())
        }

    @Test
    fun `deletes a media for good through the system dialog, keeping it in the history`() =
        runTest {
            val viewModel = viewModel()
            val effects = collectEffects(viewModel)

            viewModel.onEvent(TrashEvent.DeleteForever(MediaId(2)))
            systemTrash.remove(listOf(MediaId(2)))
            viewModel.onEvent(TrashEvent.SystemRequestAnswered(confirmed = true))

            assertEquals(listOf(FakeMediaActionRequest(Kind.DELETE, setOf(MediaId(2)))), launched)
            assertEquals(
                listOf(MediaId(1)),
                viewModel
                    .loaded()
                    .trash.items
                    .map { it.media.id },
            )
            assertEquals(2, history.deletions.size)
            assertEquals(TrashEffect.DeletedForever, effects.last())
        }

    @Test
    fun `asks for its own confirmation before deleting for good with media management`() =
        runTest {
            mediaManagementGranted = true
            val viewModel = viewModel()

            viewModel.onEvent(TrashEvent.DeleteForever(MediaId(2)))
            assertEquals(
                MediaId(2),
                viewModel
                    .loaded()
                    .confirmingDeletion
                    ?.media
                    ?.id,
            )
            assertTrue(launched.isEmpty())

            viewModel.onEvent(TrashEvent.DeletionDismissed)
            assertEquals(null, viewModel.loaded().confirmingDeletion)
            assertTrue(launched.isEmpty())

            viewModel.onEvent(TrashEvent.DeleteForever(MediaId(2)))
            viewModel.onEvent(TrashEvent.DeletionConfirmed)
            assertEquals(listOf(FakeMediaActionRequest(Kind.DELETE, setOf(MediaId(2)))), launched)
        }

    @Test
    fun `drops without error a media emptied from the trash elsewhere`() =
        runTest {
            val viewModel = viewModel()
            systemTrash.remove(listOf(MediaId(1)))

            viewModel.onEvent(TrashEvent.Resumed)

            assertEquals(
                listOf(MediaId(2)),
                viewModel
                    .loaded()
                    .trash.items
                    .map { it.media.id },
            )
        }

    @Test
    fun `goes back`() =
        runTest {
            val viewModel = viewModel()
            val effects = collectEffects(viewModel)

            viewModel.onEvent(TrashEvent.Back)

            assertEquals(listOf(TrashEffect.Close), effects)
        }
}
