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
import fr.babeuloula.photoswipe.domain.model.SimilarGroup
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

/** Le média du dessus confié à une autre application : partage, « Ouvrir avec », « Renvoyer avec » (PS-162, PS-163). */
@RunWith(RobolectricTestRunner::class)
class DeckElsewhereTest : DeckViewModelTestBase() {
    @Test
    fun `opens the top card in another app without deciding, then reads the deck again on return`() =
        runTest {
            val media = takenOn(1, 2020)
            val gallery = FakeMediaSource(listOf(media))
            val viewModel = deckViewModel(gallery)
            viewModel.onEvent(DeckEvent.OpenMenu)

            viewModel.onEvent(DeckEvent.OpenWith)

            assertEquals(DeckEffect.OpenWith(media.uri, media.type), viewModel.uiEffects.first())
            assertFalse((viewModel.uiState.value as DeckUiState.Loaded).menuOpen)
            assertTrue(repository.entries.isEmpty())
            // Une retouche enregistrée en copie, avec la même date, devient un nouveau souvenir du jour.
            gallery.add(takenOn(2, 2020))

            viewModel.onEvent(DeckEvent.Resumed)

            assertEquals(2, (viewModel.uiState.value as DeckUiState.Loaded).total)
        }

    @Test
    fun `does not read the deck again on return when nothing was opened elsewhere`() =
        runTest {
            val gallery = FakeMediaSource(listOf(takenOn(1, 2020)))
            val viewModel = deckViewModel(gallery)
            gallery.add(takenOn(2, 2020))

            viewModel.onEvent(DeckEvent.Resumed)

            assertEquals(1, (viewModel.uiState.value as DeckUiState.Loaded).total)
        }

    @Test
    fun `sends the top card again to the last app chosen, read again after a share`() =
        runTest {
            val media = takenOn(1, 2020)
            val viewModel = deckViewModel(FakeMediaSource(listOf(media)))
            assertEquals(null, (viewModel.uiState.value as DeckUiState.Loaded).shareTarget)

            viewModel.onEvent(DeckEvent.Share)
            shareTargets.saveLastTarget("com.whatsapp/.ContactPicker")
            viewModel.onEvent(DeckEvent.Resumed)
            assertEquals("com.whatsapp/.ContactPicker", (viewModel.uiState.value as DeckUiState.Loaded).shareTarget)

            viewModel.onEvent(DeckEvent.ShareAgain)

            val effects = mutableListOf<DeckEffect>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiEffects.collect {
                    effects +=
                        it
                }
            }
            assertEquals(
                listOf(
                    DeckEffect.Share(media.uri, media.type),
                    DeckEffect.ShareWith(media.uri, media.type, "com.whatsapp/.ContactPicker"),
                ),
                effects,
            )
            assertTrue(repository.entries.isEmpty())
        }

    @Test
    fun `shares the top card without changing it and closes the menu`() =
        runTest {
            val media = takenOn(1, 2020)
            val viewModel = deckViewModel(FakeMediaSource(listOf(media)))
            viewModel.onEvent(DeckEvent.OpenMenu)

            viewModel.onEvent(DeckEvent.Share)

            assertEquals(DeckEffect.Share(media.uri, media.type), viewModel.uiEffects.first())
            val state = viewModel.uiState.value as DeckUiState.Loaded
            assertFalse(state.menuOpen)
            assertEquals(listOf(MediaId(1)), state.cards.map { it.id })
            assertTrue(repository.entries.isEmpty())
            assertTrue(viewModel.session.isEmpty)
        }
}
