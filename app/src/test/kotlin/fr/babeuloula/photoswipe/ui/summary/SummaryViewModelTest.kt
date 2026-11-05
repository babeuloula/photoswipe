package fr.babeuloula.photoswipe.ui.summary

import androidx.lifecycle.SavedStateHandle
import fr.babeuloula.photoswipe.MainDispatcherRule
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.ReviewDecision
import fr.babeuloula.photoswipe.domain.model.ReviewEntry
import fr.babeuloula.photoswipe.domain.model.SessionSummary
import fr.babeuloula.photoswipe.domain.port.ReviewRepository
import fr.babeuloula.photoswipe.domain.usecase.GetSessionSummary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

/** Robolectric : la route typée se relit depuis le `SavedStateHandle` avec le `Bundle` d'Android. */
@RunWith(RobolectricTestRunner::class)
class SummaryViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = FakeReviewRepository()

    private fun summaryViewModel(
        keptCount: Int = 0,
        reviews: ReviewRepository = repository,
    ): SummaryViewModel =
        SummaryViewModel(
            savedStateHandle = SavedStateHandle(mapOf("keptCount" to keptCount)),
            getSessionSummary = GetSessionSummary(reviews),
        )

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
