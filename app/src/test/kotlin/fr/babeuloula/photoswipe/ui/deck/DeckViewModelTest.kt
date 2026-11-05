package fr.babeuloula.photoswipe.ui.deck

import androidx.lifecycle.SavedStateHandle
import fr.babeuloula.photoswipe.MainDispatcherRule
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.ReviewDecision
import fr.babeuloula.photoswipe.domain.model.ReviewEntry
import fr.babeuloula.photoswipe.domain.port.MediaSource
import fr.babeuloula.photoswipe.domain.port.ReviewRepository
import fr.babeuloula.photoswipe.domain.service.RecentYearsFirstOrdering
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import fr.babeuloula.photoswipe.domain.usecase.FavoriteMedia
import fr.babeuloula.photoswipe.domain.usecase.GetDeckForDay
import fr.babeuloula.photoswipe.domain.usecase.KeepMedia
import fr.babeuloula.photoswipe.domain.usecase.MarkForDeletion
import fr.babeuloula.photoswipe.domain.usecase.UndoLastDecision
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime.NOON

/** Robolectric : la route typée se relit depuis le `SavedStateHandle` avec le `Bundle` d'Android. */
@RunWith(RobolectricTestRunner::class)
class DeckViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    /** Pile rattrapée : le 14 mai, consulté le 16. */
    private val day = LocalDate.of(2026, 5, 14)
    private val clock = FixedClock.at(LocalDate.of(2026, 5, 16).atTime(10, 0))
    private val repository = FakeReviewRepository()

    private fun takenOn(
        id: Long,
        year: Int,
    ): MediaItem =
        aMediaItem(
            id = id,
            takenAt =
                day
                    .withYear(year)
                    .atTime(NOON)
                    .atZone(clock.zone)
                    .toInstant(),
        )

    private fun deckViewModel(
        source: MediaSource,
        reviews: ReviewRepository = repository,
    ): DeckViewModel =
        DeckViewModel(
            savedStateHandle = SavedStateHandle(mapOf("epochDay" to day.toEpochDay())),
            getDeckForDay = GetDeckForDay(SameDayMatcher(), source, reviews, RecentYearsFirstOrdering(), clock),
            decisions =
                DeckDecisions(
                    keepMedia = KeepMedia(reviews, clock),
                    markForDeletion = MarkForDeletion(reviews, clock),
                    favoriteMedia = FavoriteMedia(reviews, clock),
                    undoLastDecision = UndoLastDecision(reviews),
                ),
            clock = clock,
        )

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
                deckViewModel(FakeMediaSource(listOf(takenOn(1, 2020), takenOn(2, 2023))), FailingSaveRepository())

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
                deckViewModel(FakeMediaSource(listOf(takenOn(1, 2020), takenOn(2, 2023))), FailingSaveRepository())

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
    private class FailingSaveRepository(
        private val delegate: FakeReviewRepository = FakeReviewRepository(),
    ) : ReviewRepository by delegate {
        override suspend fun save(entry: ReviewEntry): Unit = throw IOException("database unavailable")
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
}
