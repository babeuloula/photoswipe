package fr.babeuloula.photoswipe.ui.summary

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import androidx.lifecycle.SavedStateHandle
import fr.babeuloula.photoswipe.MainDispatcherRule
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaActionRequestFactory
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.ReviewDecision
import fr.babeuloula.photoswipe.domain.model.ReviewEntry
import fr.babeuloula.photoswipe.domain.model.SessionSummary
import fr.babeuloula.photoswipe.domain.port.ReviewRepository
import fr.babeuloula.photoswipe.domain.usecase.GetSessionSummary
import fr.babeuloula.photoswipe.domain.usecase.PrepareTrash
import fr.babeuloula.photoswipe.domain.usecase.RecordTrashResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.IOException

/** Robolectric : la route typée se relit depuis le `SavedStateHandle` avec le `Bundle` d'Android. */
@RunWith(RobolectricTestRunner::class)
class SummaryViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = FakeReviewRepository()

    /** Galerie : les médias 1 à 5 existent encore. */
    private val gallery = FakeMediaSource((1L..5L).map { aMediaItem(id = it) })

    /** Dialogue système factice : un vrai `IntentSender`, qui ne sera jamais lancé ici. */
    private val intentSender: IntentSender =
        PendingIntent
            .getActivity(
                RuntimeEnvironment.getApplication(),
                0,
                Intent(),
                PendingIntent.FLAG_IMMUTABLE,
            ).intentSender

    private fun summaryViewModel(
        keptCount: Int = 0,
        reviews: ReviewRepository = repository,
    ): SummaryViewModel =
        SummaryViewModel(
            savedStateHandle = SavedStateHandle(mapOf("keptCount" to keptCount)),
            getSessionSummary = GetSessionSummary(reviews),
            prepareTrash = PrepareTrash(reviews, gallery, FakeMediaActionRequestFactory(batchSize = 2)),
            recordTrashResult = RecordTrashResult(reviews),
            mediaActionIntents = { intentSender },
        )

    private suspend fun mark(vararg ids: Long) {
        ids.forEach { repository.save(aReviewEntry(mediaId = it, decision = ReviewDecision.MARKED_FOR_DELETION)) }
    }

    private fun TestScope.collectEffects(viewModel: SummaryViewModel): List<SummaryEffect> {
        val effects = mutableListOf<SummaryEffect>()
        backgroundScope.launch(
            UnconfinedTestDispatcher(testScheduler),
        ) { viewModel.uiEffects.collect { effects += it } }
        return effects
    }

    private fun decisions() = repository.entries.sortedBy { it.mediaId.value }.map { it.decision }

    @Test
    fun `shows the media kept during the session and all pending requests`() =
        runTest {
            repository.save(
                aReviewEntry(mediaId = 1, decision = ReviewDecision.MARKED_FOR_DELETION, sizeBytes = 3_000_000),
            )
            repository.save(
                aReviewEntry(mediaId = 2, decision = ReviewDecision.MARKED_FOR_DELETION, sizeBytes = 45_000_000),
            )
            repository.save(aReviewEntry(mediaId = 3, decision = ReviewDecision.KEPT, favoritePending = true))

            val viewModel = summaryViewModel(keptCount = 5)

            assertEquals(
                SummaryUiState.Loaded(
                    SessionSummary(
                        keptCount = 5,
                        markedForDeletionCount = 2,
                        favoriteCount = 1,
                        estimatedFreedBytes = 48_000_000,
                    ),
                ),
                viewModel.uiState.value,
            )
        }

    @Test
    fun `shows an empty summary when nothing is waiting`() =
        runTest {
            assertEquals(
                SummaryUiState.Loaded(SessionSummary(0, 0, 0, 0)),
                summaryViewModel().uiState.value,
            )
        }

    @Test
    fun `shows an error when the database cannot be read, then recovers on retry`() =
        runTest {
            val reviews = FailingOnceRepository()
            val viewModel = summaryViewModel(reviews = reviews)
            assertEquals(SummaryUiState.Error, viewModel.uiState.value)

            viewModel.onEvent(SummaryEvent.Retry)

            assertEquals(SummaryUiState.Loaded(SessionSummary(0, 0, 0, 0)), viewModel.uiState.value)
        }

    @Test
    fun `goes home later without changing anything`() =
        runTest {
            repository.save(aReviewEntry(mediaId = 1, decision = ReviewDecision.MARKED_FOR_DELETION))
            val viewModel = summaryViewModel()

            viewModel.onEvent(SummaryEvent.Later)

            assertEquals(SummaryEffect.GoHome, viewModel.uiEffects.first())
            assertEquals(ReviewDecision.MARKED_FOR_DELETION, repository.entries.single().decision)
        }

    @Test
    fun `presents one system dialog per batch of marked media`() =
        runTest {
            mark(1, 2, 3)
            val viewModel = summaryViewModel()
            val effects = collectEffects(viewModel)

            viewModel.onEvent(SummaryEvent.Confirm)

            assertEquals(listOf<SummaryEffect>(SummaryEffect.LaunchSystemRequest(intentSender)), effects)
            assertTrue((viewModel.uiState.value as SummaryUiState.Loaded).confirming)

            viewModel.onEvent(SummaryEvent.SystemRequestAnswered(confirmed = true))

            assertEquals(2, effects.count { it is SummaryEffect.LaunchSystemRequest })
        }

    @Test
    fun `moves the confirmed media to the trash and updates the summary`() =
        runTest {
            mark(1, 2, 3)
            val viewModel = summaryViewModel()
            val effects = collectEffects(viewModel)

            viewModel.onEvent(SummaryEvent.Confirm)
            viewModel.onEvent(SummaryEvent.SystemRequestAnswered(confirmed = true))
            viewModel.onEvent(SummaryEvent.SystemRequestAnswered(confirmed = true))

            assertEquals(List(3) { ReviewDecision.TRASHED }, decisions())
            assertEquals(SummaryEffect.Trashed(trashedCount = 3), effects.last())
            val state = viewModel.uiState.value as SummaryUiState.Loaded
            assertEquals(0, state.summary.markedForDeletionCount)
            assertFalse(state.confirming)
        }

    @Test
    fun `keeps the marks of a refused batch`() =
        runTest {
            mark(1, 2, 3)
            val viewModel = summaryViewModel()
            val effects = collectEffects(viewModel)

            viewModel.onEvent(SummaryEvent.Confirm)
            viewModel.onEvent(SummaryEvent.SystemRequestAnswered(confirmed = false))
            viewModel.onEvent(SummaryEvent.SystemRequestAnswered(confirmed = true))

            assertEquals(
                listOf(ReviewDecision.MARKED_FOR_DELETION, ReviewDecision.MARKED_FOR_DELETION, ReviewDecision.TRASHED),
                decisions(),
            )
            assertEquals(SummaryEffect.Trashed(trashedCount = 1), effects.last())
            assertEquals(2, (viewModel.uiState.value as SummaryUiState.Loaded).summary.markedForDeletionCount)
        }

    @Test
    fun `announces nothing when every batch is refused`() =
        runTest {
            mark(1)
            val viewModel = summaryViewModel()
            val effects = collectEffects(viewModel)

            viewModel.onEvent(SummaryEvent.Confirm)
            viewModel.onEvent(SummaryEvent.SystemRequestAnswered(confirmed = false))

            assertTrue(effects.none { it is SummaryEffect.Trashed })
            assertEquals(listOf(ReviewDecision.MARKED_FOR_DELETION), decisions())
        }

    @Test
    fun `cannot confirm when nothing is marked`() =
        runTest {
            val viewModel = summaryViewModel()
            val effects = collectEffects(viewModel)

            assertFalse((viewModel.uiState.value as SummaryUiState.Loaded).canConfirm)
            viewModel.onEvent(SummaryEvent.Confirm)

            assertTrue(effects.isEmpty())
        }

    @Test
    fun `ignores an answer when no dialog is shown`() =
        runTest {
            mark(1)
            val viewModel = summaryViewModel()

            viewModel.onEvent(SummaryEvent.SystemRequestAnswered(confirmed = true))

            assertEquals(listOf(ReviewDecision.MARKED_FOR_DELETION), decisions())
        }

    @Test
    fun `goes back with the back arrow`() =
        runTest {
            val viewModel = summaryViewModel()

            viewModel.onEvent(SummaryEvent.Back)

            assertEquals(SummaryEffect.Close, viewModel.uiEffects.first())
        }

    /** Dépôt dont la première lecture des marquages échoue. */
    private class FailingOnceRepository(
        private val delegate: FakeReviewRepository = FakeReviewRepository(),
    ) : ReviewRepository by delegate {
        private var failed = false

        override suspend fun entriesMarkedForDeletion(): List<ReviewEntry> {
            if (!failed) {
                failed = true
                throw IOException("database unavailable")
            }
            return delegate.entriesMarkedForDeletion()
        }
    }
}
