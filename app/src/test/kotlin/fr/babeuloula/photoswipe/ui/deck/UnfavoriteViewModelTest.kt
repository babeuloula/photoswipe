package fr.babeuloula.photoswipe.ui.deck

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import androidx.lifecycle.SavedStateHandle
import fr.babeuloula.photoswipe.MainDispatcherRule
import fr.babeuloula.photoswipe.domain.fixtures.FakeDecisionJournal
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaActionRequestFactory
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.FakeSettingsRepository
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.DecisionRecord
import fr.babeuloula.photoswipe.domain.model.ManageMediaTip
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.ReviewDecision
import fr.babeuloula.photoswipe.domain.model.ReviewEntry
import fr.babeuloula.photoswipe.domain.port.DecisionJournal
import fr.babeuloula.photoswipe.domain.port.MediaSource
import fr.babeuloula.photoswipe.domain.service.RecentYearsFirstOrdering
import fr.babeuloula.photoswipe.domain.service.ReviewWindowProvider
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import fr.babeuloula.photoswipe.domain.usecase.DropPendingFavorite
import fr.babeuloula.photoswipe.domain.usecase.FavoriteMedia
import fr.babeuloula.photoswipe.domain.usecase.GetDaySession
import fr.babeuloula.photoswipe.domain.usecase.GetDeckForDay
import fr.babeuloula.photoswipe.domain.usecase.GetMarkedDeck
import fr.babeuloula.photoswipe.domain.usecase.GetPendingFavorites
import fr.babeuloula.photoswipe.domain.usecase.GetPreviousDecisions
import fr.babeuloula.photoswipe.domain.usecase.KeepMedia
import fr.babeuloula.photoswipe.domain.usecase.MarkForDeletion
import fr.babeuloula.photoswipe.domain.usecase.OfferManageMediaTip
import fr.babeuloula.photoswipe.domain.usecase.RecordFavoriteResult
import fr.babeuloula.photoswipe.domain.usecase.RequestFavorite
import fr.babeuloula.photoswipe.domain.usecase.RequestUnfavorite
import fr.babeuloula.photoswipe.domain.usecase.ReviewMarkedMedia
import fr.babeuloula.photoswipe.domain.usecase.UndoLastDecision
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime.NOON

/** Retrait d'un favori depuis la pile (PS-124). */
@RunWith(RobolectricTestRunner::class)
class UnfavoriteViewModelTest : DeckViewModelTestBase() {
    private fun topCard(viewModel: DeckViewModel): MediaCardUiState =
        (viewModel.uiState.value as DeckUiState.Loaded).current

    @Test
    fun `removes a gallery favorite once the gallery accepts, leaving the card in place`() =
        runTest {
            val viewModel = deckViewModel(FakeMediaSource(listOf(takenOn(1, 2020).copy(isFavorite = true))))
            assertTrue(topCard(viewModel).favorite)

            viewModel.onEvent(DeckEvent.Unfavorite)
            assertEquals(DeckEffect.LaunchUnfavoriteRequest(intentSender), viewModel.uiEffects.first())
            viewModel.onEvent(DeckEvent.UnfavoriteRequestAnswered(confirmed = true))

            assertFalse(topCard(viewModel).favorite)
            assertEquals(MediaId(1), topCard(viewModel).id)
            assertTrue(repository.entries.isEmpty())
            assertTrue(viewModel.session.isEmpty)
        }

    @Test
    fun `keeps the gallery favorite when its removal is refused`() =
        runTest {
            val viewModel = deckViewModel(FakeMediaSource(listOf(takenOn(1, 2020).copy(isFavorite = true))))

            viewModel.onEvent(DeckEvent.Unfavorite)
            viewModel.onEvent(DeckEvent.UnfavoriteRequestAnswered(confirmed = false))

            assertTrue(topCard(viewModel).favorite)
        }

    @Test
    fun `drops a favorite only pending in the app without asking the gallery`() =
        runTest {
            val media = takenOn(1, 2020)
            repository.save(
                aReviewEntry(
                    mediaId = 1,
                    reviewYear = 2025,
                    decision = ReviewDecision.KEPT,
                    favoritePending = true,
                    reopened = true,
                ),
            )
            val viewModel = deckViewModel(FakeMediaSource(listOf(media)))
            val effects = mutableListOf<DeckEffect>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiEffects.collect {
                    effects +=
                        it
                }
            }
            assertTrue(topCard(viewModel).favorite)

            viewModel.onEvent(DeckEvent.Unfavorite)

            assertFalse(topCard(viewModel).favorite)
            assertTrue(effects.isEmpty())
            assertTrue(repository.entries.none { it.favoritePending })
        }
}
