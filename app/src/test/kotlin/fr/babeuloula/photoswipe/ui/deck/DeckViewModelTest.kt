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

/** Robolectric : la route typée se relit depuis le `SavedStateHandle` avec le `Bundle` d'Android. */
@RunWith(RobolectricTestRunner::class)
class DeckViewModelTest : DeckViewModelTestBase() {
    private fun DeckViewModel.cardIds(): List<MediaId> =
        when (val state = uiState.value) {
            is DeckUiState.Loaded -> state.cards.map { it.id }
            else -> emptyList()
        }

    @Test
    fun `reads the day of the deck from its route`() {
        assertEquals(day, deckViewModel(FakeMediaSource()).day)
    }

    @Test
    fun `shows the media left to sort, most recent years first`() =
        runTest {
            repository.save(aReviewEntry(mediaId = 3, reviewYear = 2026))
            val viewModel = deckViewModel(FakeMediaSource(listOf(takenOn(1, 2020), takenOn(2, 2023), takenOn(3, 2025))))

            val state = viewModel.uiState.value as DeckUiState.Loaded

            assertEquals(listOf(MediaId(2), MediaId(1)), state.cards.map { it.id })
            assertEquals(3, state.current.yearsAgo)
        }

    @Test
    fun `says where the top card stands among the memories of its year`() =
        runTest {
            val gallery = listOf(takenOn(1, 2025), takenOn(2, 2025), takenOn(3, 2023), takenOn(4, 2025))
            val viewModel = deckViewModel(FakeMediaSource(gallery))

            fun state() = viewModel.uiState.value as DeckUiState.Loaded
            assertEquals(Triple(2025, 1, 3), Triple(state().year, state().yearPosition, state().yearTotal))

            viewModel.onEvent(DeckEvent.Keep)
            viewModel.onEvent(DeckEvent.MarkForDeletion)
            assertEquals(Triple(2025, 3, 3), Triple(state().year, state().yearPosition, state().yearTotal))

            viewModel.onEvent(DeckEvent.Keep)
            assertEquals(Triple(2023, 1, 1), Triple(state().year, state().yearPosition, state().yearTotal))

            viewModel.onEvent(DeckEvent.Undo)
            assertEquals(Triple(2025, 3, 3), Triple(state().year, state().yearPosition, state().yearTotal))
            // La barre de progression a un tronçon par année, de la plus récente à la plus ancienne (PS-144).
            assertEquals(listOf(YearSegment(2025, 2, 3), YearSegment(2023, 0, 1)), state().yearSegments)
        }

    @Test
    fun `opens the deck of the day on the media tapped in the day overview`() =
        runTest {
            val gallery = listOf(takenOn(1, 2025), takenOn(2, 2023), takenOn(3, 2020))
            val viewModel =
                deckViewModel(
                    FakeMediaSource(gallery),
                    route = mapOf("epochDay" to day.toEpochDay(), "firstMediaId" to 3L),
                )

            assertEquals(listOf(MediaId(3), MediaId(1), MediaId(2)), viewModel.cardIds())
            assertEquals(3, (viewModel.uiState.value as DeckUiState.Loaded).total)
        }

    @Test
    fun `says which cards were taken one after another, counting the photos already sorted`() =
        runTest {
            val burst =
                (1L..3L).map {
                    takenOn(it, 2023).let { media ->
                        media.copy(
                            takenAt =
                                media.takenAt.plusSeconds(it * 5),
                        )
                    }
                }
            repository.save(aReviewEntry(mediaId = 1, reviewYear = 2026))
            val viewModel = deckViewModel(FakeMediaSource(burst + takenOn(4, 2020)))

            val cards = (viewModel.uiState.value as DeckUiState.Loaded).cards

            assertEquals(
                listOf(2 to 3, 3 to 3, null),
                cards.map { card -> card.similar?.let { it.position to it.size } },
            )
            // La photo précédente de la première carte est déjà triée : on peut quand même comparer avec elle.
            assertEquals(true, cards.first().similar?.previousUri != null)
        }

    @Test
    fun `is empty when everything has been sorted`() =
        runTest {
            repository.save(aReviewEntry(mediaId = 1, reviewYear = 2026))
            val viewModel = deckViewModel(FakeMediaSource(listOf(takenOn(1, 2020))))

            assertEquals(DeckUiState.Empty(), viewModel.uiState.value)
        }

    @Test
    fun `shows an error when the gallery cannot be read, then recovers on retry`() =
        runTest {
            val viewModel = deckViewModel(FailingOnceMediaSource(listOf(takenOn(1, 2020))))

            assertEquals(DeckUiState.Error, viewModel.uiState.value)

            viewModel.onEvent(DeckEvent.Retry)

            assertEquals(listOf(MediaId(1)), (viewModel.uiState.value as DeckUiState.Loaded).cards.map { it.id })
        }

    @Test
    fun `opens the summary when finishing`() =
        runTest {
            val viewModel = deckViewModel(FakeMediaSource())

            viewModel.onEvent(DeckEvent.Finish)

            assertEquals(DeckEffect.OpenSummary, viewModel.uiEffects.first())
        }

    @Test
    fun `keeps the top card, records it for the year of the deck and shows the next one`() =
        runTest {
            val viewModel = deckViewModel(FakeMediaSource(listOf(takenOn(1, 2020), takenOn(2, 2023))))

            viewModel.onEvent(DeckEvent.Keep)

            assertEquals(listOf(MediaId(1)), viewModel.cardIds())
            val entry = repository.entries.single()
            assertEquals(MediaId(2), entry.mediaId)
            assertEquals(2026, entry.reviewYear)
            assertEquals(ReviewDecision.KEPT, entry.decision)
        }

    @Test
    fun `marks the top card for deletion without deleting it and shows the next one`() =
        runTest {
            val gallery = FakeMediaSource(listOf(takenOn(1, 2020), takenOn(2, 2023)))
            val viewModel = deckViewModel(gallery)

            viewModel.onEvent(DeckEvent.MarkForDeletion)

            assertEquals(listOf(MediaId(1)), viewModel.cardIds())
            val entry = repository.entries.single()
            assertEquals(MediaId(2), entry.mediaId)
            assertEquals(2026, entry.reviewYear)
            assertEquals(ReviewDecision.MARKED_FOR_DELETION, entry.decision)
            assertEquals(listOf(MediaId(1), MediaId(2)), gallery.media.map { it.id })
        }

    @Test
    fun `remembers kept and marked media in the session for undo`() =
        runTest {
            val viewModel = deckViewModel(FakeMediaSource(listOf(takenOn(1, 2020), takenOn(2, 2023))))

            viewModel.onEvent(DeckEvent.MarkForDeletion)
            viewModel.onEvent(DeckEvent.Keep)

            assertEquals(listOf(MediaId(2), MediaId(1)), viewModel.session.decisions.map { it.media.id })
        }

    @Test
    fun `puts a marked card back on top when the decision cannot be saved`() =
        runTest {
            val viewModel =
                deckViewModel(FakeMediaSource(listOf(takenOn(1, 2020), takenOn(2, 2023))), FailingJournal())

            viewModel.onEvent(DeckEvent.MarkForDeletion)

            assertEquals(listOf(MediaId(2), MediaId(1)), viewModel.cardIds())
        }

    @Test
    fun `remembers each kept media in the session for undo`() =
        runTest {
            val viewModel = deckViewModel(FakeMediaSource(listOf(takenOn(1, 2020), takenOn(2, 2023))))

            viewModel.onEvent(DeckEvent.Keep)
            viewModel.onEvent(DeckEvent.Keep)

            assertEquals(listOf(MediaId(2), MediaId(1)), viewModel.session.decisions.map { it.media.id })
        }

    @Test
    fun `is empty once the last card is kept`() =
        runTest {
            val viewModel = deckViewModel(FakeMediaSource(listOf(takenOn(1, 2020))))

            viewModel.onEvent(DeckEvent.Keep)

            assertEquals(DeckUiState.Empty(canUndo = true), viewModel.uiState.value)
        }

    @Test
    fun `cannot undo before any decision`() =
        runTest {
            val viewModel = deckViewModel(FakeMediaSource(listOf(takenOn(1, 2020))))

            assertFalse((viewModel.uiState.value as DeckUiState.Loaded).canUndo)
        }

    @Test
    fun `puts a kept card back on top from the right and forgets its decision`() =
        runTest {
            val viewModel = deckViewModel(FakeMediaSource(listOf(takenOn(1, 2020), takenOn(2, 2023))))
            viewModel.onEvent(DeckEvent.Keep)

            viewModel.onEvent(DeckEvent.Undo)

            val state = viewModel.uiState.value as DeckUiState.Loaded
            assertEquals(listOf(MediaId(2), MediaId(1)), state.cards.map { it.id })
            assertEquals(CardSide.RIGHT, state.returningFrom)
            assertFalse(state.canUndo)
            assertTrue(repository.entries.isEmpty())
            assertTrue(viewModel.session.isEmpty)
        }

    @Test
    fun `puts a marked card back on top from the left`() =
        runTest {
            val viewModel = deckViewModel(FakeMediaSource(listOf(takenOn(1, 2020), takenOn(2, 2023))))
            viewModel.onEvent(DeckEvent.MarkForDeletion)

            viewModel.onEvent(DeckEvent.Undo)

            assertEquals(CardSide.LEFT, (viewModel.uiState.value as DeckUiState.Loaded).returningFrom)
            assertTrue(repository.entries.isEmpty())
        }

    @Test
    fun `undoes decisions one by one, most recent first`() =
        runTest {
            val viewModel = deckViewModel(FakeMediaSource(listOf(takenOn(1, 2020), takenOn(2, 2023))))
            viewModel.onEvent(DeckEvent.MarkForDeletion)
            viewModel.onEvent(DeckEvent.Keep)

            viewModel.onEvent(DeckEvent.Undo)
            val afterFirst = viewModel.uiState.value as DeckUiState.Loaded
            assertEquals(listOf(MediaId(1)), afterFirst.cards.map { it.id })
            assertEquals(CardSide.RIGHT, afterFirst.returningFrom)
            assertTrue(afterFirst.canUndo)

            viewModel.onEvent(DeckEvent.Undo)
            val afterSecond = viewModel.uiState.value as DeckUiState.Loaded
            assertEquals(listOf(MediaId(2), MediaId(1)), afterSecond.cards.map { it.id })
            assertEquals(CardSide.LEFT, afterSecond.returningFrom)
            assertFalse(afterSecond.canUndo)
        }

    @Test
    fun `brings the last card back after the deck was emptied`() =
        runTest {
            val viewModel = deckViewModel(FakeMediaSource(listOf(takenOn(1, 2020))))
            viewModel.onEvent(DeckEvent.Keep)

            viewModel.onEvent(DeckEvent.Undo)

            assertEquals(listOf(MediaId(1)), viewModel.cardIds())
        }

    @Test
    fun `leaves the decisions of other years untouched when undoing`() =
        runTest {
            repository.save(aReviewEntry(mediaId = 1, reviewYear = 2025))
            val viewModel = deckViewModel(FakeMediaSource(listOf(takenOn(1, 2020))))
            viewModel.onEvent(DeckEvent.Keep)

            viewModel.onEvent(DeckEvent.Undo)

            assertEquals(listOf(2025), repository.entries.map { it.reviewYear })
        }

    @Test
    fun `opens and closes the menu without touching the card`() =
        runTest {
            val viewModel = deckViewModel(FakeMediaSource(listOf(takenOn(1, 2020))))

            viewModel.onEvent(DeckEvent.OpenMenu)
            assertTrue((viewModel.uiState.value as DeckUiState.Loaded).menuOpen)

            viewModel.onEvent(DeckEvent.CloseMenu)
            val state = viewModel.uiState.value as DeckUiState.Loaded
            assertFalse(state.menuOpen)
            assertEquals(listOf(MediaId(1)), state.cards.map { it.id })
            assertTrue(repository.entries.isEmpty())
        }

    @Test
    fun `favorites the top card, keeps it and shows the next one`() =
        runTest {
            val viewModel = deckViewModel(FakeMediaSource(listOf(takenOn(1, 2020), takenOn(2, 2023))))

            viewModel.onEvent(DeckEvent.Favorite)

            assertEquals(listOf(MediaId(1)), viewModel.cardIds())
            val entry = repository.entries.single()
            assertEquals(MediaId(2), entry.mediaId)
            assertEquals(ReviewDecision.KEPT, entry.decision)
            assertTrue(entry.favoritePending)
        }

    @Test
    fun `brings a favorited card back from the top on undo`() =
        runTest {
            val viewModel = deckViewModel(FakeMediaSource(listOf(takenOn(1, 2020))))
            viewModel.onEvent(DeckEvent.Favorite)

            viewModel.onEvent(DeckEvent.Undo)

            assertEquals(CardSide.TOP, (viewModel.uiState.value as DeckUiState.Loaded).returningFrom)
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

    @Test
    fun `closes the deck when going back`() =
        runTest {
            val viewModel = deckViewModel(FakeMediaSource())

            viewModel.onEvent(DeckEvent.Back)

            assertEquals(DeckEffect.Close, viewModel.uiEffects.first())
        }

    @Test
    fun `prepares only the top card and the three below it in the stack`() =
        runTest {
            val gallery = (1L..6L).map { takenOn(it, 2014 + it.toInt()) }
            val viewModel = deckViewModel(FakeMediaSource(gallery))

            val state = viewModel.uiState.value as DeckUiState.Loaded

            assertEquals(listOf(MediaId(6), MediaId(5), MediaId(4), MediaId(3)), state.cards.map { it.id })
            assertEquals(MediaId(5), state.next?.id)
            assertEquals(6, state.remainingCount)
        }

    @Test
    fun `counts the sorted cards for the progress of the day`() =
        runTest {
            val viewModel = deckViewModel(FakeMediaSource(listOf(takenOn(1, 2020), takenOn(2, 2021), takenOn(3, 2022))))

            viewModel.onEvent(DeckEvent.Keep)

            val state = viewModel.uiState.value as DeckUiState.Loaded
            assertEquals(1, state.sortedCount)
            assertEquals(2, state.position)
            assertEquals(3, state.total)
        }

    @Test
    fun `keeps preparing two cards through a deck of thousands of media`() =
        runTest {
            val gallery = (1L..BIG_DECK).map { aMediaItem(id = it, takenAt = takenOn(it, 2020).takenAt) }
            val viewModel = deckViewModel(FakeMediaSource(gallery))

            repeat(SWIPES) { viewModel.onEvent(DeckEvent.Keep) }

            val state = viewModel.uiState.value as DeckUiState.Loaded
            assertEquals(DeckUiState.Loaded.VISIBLE_CARDS, state.cards.size)
            assertEquals(BIG_DECK.toInt() - SWIPES, state.remainingCount)
            assertEquals(MediaId(SWIPES + 1L), state.current.id)
        }

    @Test
    fun `opens the summary once the last card is sorted`() =
        runTest {
            val viewModel = deckViewModel(FakeMediaSource(listOf(takenOn(1, 2020), takenOn(2, 2023))))
            val effects = mutableListOf<DeckEffect>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiEffects.collect {
                    effects +=
                        it
                }
            }

            viewModel.onEvent(DeckEvent.Keep)
            assertTrue(effects.isEmpty())

            viewModel.onEvent(DeckEvent.MarkForDeletion)
            assertEquals(listOf<DeckEffect>(DeckEffect.OpenSummary), effects)
        }

    @Test
    fun `does not open the summary for a deck that was empty from the start`() =
        runTest {
            val viewModel = deckViewModel(FakeMediaSource())
            val effects = mutableListOf<DeckEffect>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiEffects.collect {
                    effects +=
                        it
                }
            }

            viewModel.onEvent(DeckEvent.Keep)

            assertEquals(DeckUiState.Empty(), viewModel.uiState.value)
            assertTrue(effects.isEmpty())
        }

    @Test
    fun `does not open the summary when the last decision cannot be saved`() =
        runTest {
            val viewModel = deckViewModel(FakeMediaSource(listOf(takenOn(1, 2020))), FailingJournal())
            val effects = mutableListOf<DeckEffect>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiEffects.collect {
                    effects +=
                        it
                }
            }

            viewModel.onEvent(DeckEvent.Keep)

            assertEquals(listOf(MediaId(1)), viewModel.cardIds())
            assertTrue(effects.isEmpty())
        }

    @Test
    fun `ignores undo when there is nothing to undo`() =
        runTest {
            val viewModel = deckViewModel(FakeMediaSource(listOf(takenOn(1, 2020))))
            val before = viewModel.uiState.value

            viewModel.onEvent(DeckEvent.Undo)

            assertEquals(before, viewModel.uiState.value)
        }

    @Test
    fun `puts the card back on top when the decision cannot be saved`() =
        runTest {
            val viewModel =
                deckViewModel(FakeMediaSource(listOf(takenOn(1, 2020), takenOn(2, 2023))), FailingJournal())

            viewModel.onEvent(DeckEvent.Keep)

            assertEquals(listOf(MediaId(2), MediaId(1)), viewModel.cardIds())
            assertTrue(viewModel.session.isEmpty)
        }

    @Test
    fun `ignores a keep when there is no card`() =
        runTest {
            val viewModel = deckViewModel(FakeMediaSource())

            viewModel.onEvent(DeckEvent.Keep)

            assertEquals(DeckUiState.Empty(), viewModel.uiState.value)
            assertTrue(repository.entries.isEmpty())
        }

    /** Dépôt qui lit normalement mais refuse toute écriture. */
    @Test
    fun `resumes the undo history of the day after the process died`() =
        runTest {
            val source = FakeMediaSource(listOf(takenOn(1, 2020), takenOn(2, 2023), takenOn(3, 2025)))
            val before = deckViewModel(source)
            before.onEvent(DeckEvent.Keep)
            before.onEvent(DeckEvent.MarkForDeletion)

            val after = deckViewModel(source)

            assertEquals(listOf(MediaId(1)), after.cardIds())
            assertEquals(listOf(MediaId(3), MediaId(2)), after.session.decisions.map { it.media.id })
            assertTrue((after.uiState.value as DeckUiState.Loaded).canUndo)

            after.onEvent(DeckEvent.Undo)

            val state = after.uiState.value as DeckUiState.Loaded
            assertEquals(listOf(MediaId(2), MediaId(1)), state.cards.map { it.id })
            assertEquals(CardSide.LEFT, state.returningFrom)
            assertEquals(listOf(MediaId(3)), repository.entries.map { it.mediaId })
        }

    @Test
    fun `undoes a resumed favorite back to the top and counts only resumed kept media`() =
        runTest {
            val source = FakeMediaSource(listOf(takenOn(1, 2020), takenOn(2, 2023), takenOn(3, 2025)))
            val before = deckViewModel(source)
            before.onEvent(DeckEvent.Keep)
            before.onEvent(DeckEvent.Favorite)

            val after = deckViewModel(source)
            val effects = mutableListOf<DeckEffect>()
            backgroundScope.launch(
                UnconfinedTestDispatcher(testScheduler),
            ) { after.uiEffects.collect { effects += it } }
            after.onEvent(DeckEvent.Undo)
            assertEquals(CardSide.TOP, (after.uiState.value as DeckUiState.Loaded).returningFrom)
            after.onEvent(DeckEvent.Finish)

            assertEquals(DeckEffect.OpenSummary, effects.last())
        }

    /** Journal dont aucune écriture n'aboutit : base indisponible. */
    private class FailingJournal(
        private val delegate: FakeDecisionJournal = FakeDecisionJournal(),
    ) : DecisionJournal by delegate {
        override suspend fun record(
            day: LocalDate,
            made: ReviewEntry,
            previous: ReviewEntry?,
        ): DecisionRecord = throw IOException("database unavailable")
    }

    private companion object {
        /** Pile de plusieurs milliers de médias (SPEC §14). */
        const val BIG_DECK = 5_000L

        /** Gestes enchaînés sur la grosse pile. */
        const val SWIPES = 300
    }

    private class FailingOnceMediaSource(
        media: List<MediaItem>,
    ) : MediaSource {
        private val delegate = FakeMediaSource(media)
        private var failed = false

        override suspend fun mediaTakenIn(ranges: List<OpenEndRange<Instant>>): List<MediaItem> {
            if (!failed) {
                failed = true
                throw IOException("MediaStore unavailable")
            }
            return delegate.mediaTakenIn(ranges)
        }

        override suspend fun mediaByIds(ids: Set<MediaId>): List<MediaItem> = delegate.mediaByIds(ids)
    }

    @Test
    fun `asks the gallery to favorite the media right away`() =
        runTest {
            val viewModel = deckViewModel(FakeMediaSource(listOf(takenOn(1, 2020), takenOn(2, 2023))))

            viewModel.onEvent(DeckEvent.Favorite)

            assertEquals(DeckEffect.LaunchFavoriteRequest(intentSender), viewModel.uiEffects.first())
        }

    @Test
    fun `applies the favorite once the request is accepted`() =
        runTest {
            val viewModel = deckViewModel(FakeMediaSource(listOf(takenOn(1, 2020), takenOn(2, 2023))))
            viewModel.onEvent(DeckEvent.Favorite)

            viewModel.onEvent(DeckEvent.FavoriteRequestAnswered(confirmed = true))

            val entry = repository.entries.single()
            assertEquals(ReviewDecision.KEPT, entry.decision)
            assertEquals(false, entry.favoritePending)
            assertEquals(ManageMediaTip.OFFERED, settings.manageMediaTip)
        }

    @Test
    fun `keeps the media with its favorite pending when the request is refused`() =
        runTest {
            val viewModel = deckViewModel(FakeMediaSource(listOf(takenOn(1, 2020), takenOn(2, 2023))))
            viewModel.onEvent(DeckEvent.Favorite)

            viewModel.onEvent(DeckEvent.FavoriteRequestAnswered(confirmed = false))

            val entry = repository.entries.single()
            assertEquals(ReviewDecision.KEPT, entry.decision)
            assertTrue(entry.favoritePending)
            assertEquals(ManageMediaTip.NOT_OFFERED, settings.manageMediaTip)
        }

    @Test
    fun `does not offer media management when the favorite went through silently`() =
        runTest {
            mediaManagementGranted = true
            val viewModel = deckViewModel(FakeMediaSource(listOf(takenOn(1, 2020), takenOn(2, 2023))))
            viewModel.onEvent(DeckEvent.Favorite)

            viewModel.onEvent(DeckEvent.FavoriteRequestAnswered(confirmed = true))

            assertEquals(ManageMediaTip.NOT_OFFERED, settings.manageMediaTip)
        }

    @Test
    fun `opens the summary only once the favorite of the last card is answered`() =
        runTest {
            val viewModel = deckViewModel(FakeMediaSource(listOf(takenOn(1, 2020))))
            val effects = mutableListOf<DeckEffect>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiEffects.collect {
                    effects +=
                        it
                }
            }

            viewModel.onEvent(DeckEvent.Favorite)
            assertEquals(listOf<DeckEffect>(DeckEffect.LaunchFavoriteRequest(intentSender)), effects)

            viewModel.onEvent(DeckEvent.FavoriteRequestAnswered(confirmed = true))

            assertEquals(DeckEffect.OpenSummary, effects.last())
        }

    @Test
    fun `shows the previous decision of each card of a reopened day, again after an undo`() =
        runTest {
            repository.save(
                aReviewEntry(mediaId = 1, reviewYear = 2026, decision = ReviewDecision.KEPT, reopened = true),
            )
            repository.save(
                aReviewEntry(
                    mediaId = 2,
                    reviewYear = 2026,
                    decision = ReviewDecision.MARKED_FOR_DELETION,
                    reopened = true,
                ),
            )
            val viewModel = deckViewModel(FakeMediaSource(listOf(takenOn(1, 2020), takenOn(2, 2023), takenOn(3, 2021))))

            val cards = (viewModel.uiState.value as DeckUiState.Loaded).cards
            assertEquals(
                mapOf(
                    MediaId(2) to ReviewDecision.MARKED_FOR_DELETION,
                    MediaId(3) to null,
                    MediaId(1) to ReviewDecision.KEPT,
                ),
                cards.associate { it.id to it.previousDecision },
            )

            viewModel.onEvent(DeckEvent.Keep)
            viewModel.onEvent(DeckEvent.Undo)

            assertEquals(
                ReviewDecision.MARKED_FOR_DELETION,
                (viewModel.uiState.value as DeckUiState.Loaded).current.previousDecision,
            )
        }

    @Test
    fun `shows no previous decision on a day sorted for the first time`() =
        runTest {
            val viewModel = deckViewModel(FakeMediaSource(listOf(takenOn(1, 2020))))

            assertEquals(null, (viewModel.uiState.value as DeckUiState.Loaded).current.previousDecision)
        }

    /** Marque [media] pour suppression dans la pile du jour, comme avant d'ouvrir le résumé. */
    private suspend fun markInDayDeck(vararg media: MediaItem) {
        media.forEach { MarkForDeletion(repository, journal, clock)(it, day) }
    }

    @Test
    fun `opens the marked deck from the media tapped in the summary, each card showing its mark`() =
        runTest {
            val first = takenOn(1, 2020)
            val tapped = takenOn(2, 2023)
            markInDayDeck(first, tapped)
            val viewModel =
                deckViewModel(FakeMediaSource(listOf(first, tapped)), route = mapOf("firstMediaId" to 2L))

            val state = viewModel.uiState.value as DeckUiState.Loaded

            assertEquals(null, viewModel.day)
            assertEquals(listOf(MediaId(2), MediaId(1)), state.cards.map { it.id })
            assertTrue(state.cards.all { it.previousDecision == ReviewDecision.MARKED_FOR_DELETION })
            assertEquals(2, state.total)
        }

    @Test
    fun `keeps a marked media from the marked deck, undoes it, and goes back to the summary when done`() =
        runTest {
            val media = takenOn(1, 2020)
            markInDayDeck(media)
            val viewModel = deckViewModel(FakeMediaSource(listOf(media)), route = emptyMap())

            viewModel.onEvent(DeckEvent.Keep)
            assertEquals(emptyList<Any>(), repository.entriesMarkedForDeletion())
            assertEquals(DeckEffect.Close, viewModel.uiEffects.first())

            viewModel.onEvent(DeckEvent.Undo)
            assertEquals(listOf(MediaId(1)), repository.entriesMarkedForDeletion().map { it.mediaId })
            assertEquals(MediaId(1), (viewModel.uiState.value as DeckUiState.Loaded).current.id)
        }

    @Test
    fun `shows a heart on a media already in the gallery favorites`() =
        runTest {
            val favorite = takenOn(1, 2020).copy(isFavorite = true)
            val viewModel = deckViewModel(FakeMediaSource(listOf(favorite, takenOn(2, 2023))))

            val cards = (viewModel.uiState.value as DeckUiState.Loaded).cards

            assertEquals(mapOf(MediaId(2) to false, MediaId(1) to true), cards.associate { it.id to it.favorite })
        }

    @Test
    fun `shows a heart on a media whose favorite is still pending in the app`() =
        runTest {
            repository.save(
                aReviewEntry(mediaId = 1, reviewYear = 2025, decision = ReviewDecision.KEPT, favoritePending = true),
            )
            val viewModel = deckViewModel(FakeMediaSource(listOf(takenOn(1, 2020))))

            assertTrue((viewModel.uiState.value as DeckUiState.Loaded).current.favorite)
        }

    @Test
    fun `shows a heart on a card brought back by undo once its favorite was accepted`() =
        runTest {
            val viewModel = deckViewModel(FakeMediaSource(listOf(takenOn(1, 2020), takenOn(2, 2023))))
            viewModel.onEvent(DeckEvent.Favorite)
            viewModel.onEvent(DeckEvent.FavoriteRequestAnswered(confirmed = true))

            viewModel.onEvent(DeckEvent.Undo)

            assertTrue((viewModel.uiState.value as DeckUiState.Loaded).current.favorite)
        }

    @Test
    fun `drops the heart of a refused favorite cancelled by undo`() =
        runTest {
            val viewModel = deckViewModel(FakeMediaSource(listOf(takenOn(1, 2020), takenOn(2, 2023))))
            viewModel.onEvent(DeckEvent.Favorite)
            viewModel.onEvent(DeckEvent.FavoriteRequestAnswered(confirmed = false))

            viewModel.onEvent(DeckEvent.Undo)

            assertEquals(false, (viewModel.uiState.value as DeckUiState.Loaded).current.favorite)
        }
}
