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
import fr.babeuloula.photoswipe.domain.port.MediaSource
import fr.babeuloula.photoswipe.domain.service.RecentYearsFirstOrdering
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import fr.babeuloula.photoswipe.domain.usecase.GetDeckForDay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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

    private fun deckViewModel(source: MediaSource): DeckViewModel =
        DeckViewModel(
            savedStateHandle = SavedStateHandle(mapOf("epochDay" to day.toEpochDay())),
            getDeckForDay = GetDeckForDay(SameDayMatcher(), source, repository, RecentYearsFirstOrdering(), clock),
            clock = clock,
        )

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

            assertEquals(DeckUiState.Empty, viewModel.uiState.value)
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
