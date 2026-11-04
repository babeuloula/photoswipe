package fr.babeuloula.photoswipe.ui.home

import fr.babeuloula.photoswipe.MainDispatcherRule
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.ReviewDay
import fr.babeuloula.photoswipe.domain.port.MediaSource
import fr.babeuloula.photoswipe.domain.service.ReviewWindowProvider
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import fr.babeuloula.photoswipe.domain.usecase.GetReviewDays
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime.NOON

class HomeViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val today = LocalDate.of(2026, 7, 15)
    private val clock = FixedClock.at(today.atTime(10, 0))
    private val repository = FakeReviewRepository()

    @Test
    fun `shows how many media are left to sort today`() =
        runTest {
            val viewModel = homeViewModel(FakeMediaSource(listOf(takenOn(1, 2025), takenOn(2, 2020), takenOn(3, 2019))))
            repository.save(aReviewEntry(mediaId = 2, reviewYear = 2026))

            assertEquals(HomeUiState.Loaded(ReviewDay(today, remainingCount = 2)), loaded(viewModel))
        }

    @Test
    fun `updates the count after a decision`() =
        runTest {
            val viewModel = homeViewModel(FakeMediaSource(listOf(takenOn(1, 2025), takenOn(2, 2020))))
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

            repository.save(aReviewEntry(mediaId = 1, reviewYear = 2026))

            assertEquals(HomeUiState.Loaded(ReviewDay(today, remainingCount = 1)), viewModel.uiState.value)
        }

    @Test
    fun `starts loading`() {
        assertEquals(HomeUiState.Loading, homeViewModel(FakeMediaSource()).uiState.value)
    }

    @Test
    fun `shows an error when the gallery cannot be read, then recovers on retry`() =
        runTest {
            val source = FailingOnceMediaSource(listOf(takenOn(1, 2025)))
            val viewModel = homeViewModel(source)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

            assertEquals(HomeUiState.Error, viewModel.uiState.value)

            viewModel.onEvent(HomeEvent.Retry)

            assertEquals(HomeUiState.Loaded(ReviewDay(today, remainingCount = 1)), viewModel.uiState.value)
        }

    @Test
    fun `counts media added to the gallery when the app comes back to the foreground`() =
        runTest {
            val gallery = FakeMediaSource(listOf(takenOn(1, 2025)))
            val viewModel = homeViewModel(gallery)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
            gallery.add(takenOn(2, 2024))

            viewModel.onEvent(HomeEvent.Resumed)

            assertEquals(HomeUiState.Loaded(ReviewDay(today, remainingCount = 2)), viewModel.uiState.value)
        }

    @Test
    fun `opens the deck of today from its card`() =
        runTest {
            val viewModel = homeViewModel(FakeMediaSource(listOf(takenOn(1, 2025))))
            loaded(viewModel)

            viewModel.onEvent(HomeEvent.OpenToday)

            assertEquals(HomeEffect.OpenDeck(ReviewDay(today, remainingCount = 1)), viewModel.uiEffects.first())
        }

    @Test
    fun `opens the settings`() =
        runTest {
            val viewModel = homeViewModel(FakeMediaSource())

            viewModel.onEvent(HomeEvent.OpenSettings)

            assertEquals(HomeEffect.OpenSettings, viewModel.uiEffects.first())
        }

    private fun homeViewModel(source: MediaSource): HomeViewModel =
        HomeViewModel(GetReviewDays(ReviewWindowProvider(clock), SameDayMatcher(), source, repository, clock))

    private suspend fun loaded(viewModel: HomeViewModel): HomeUiState =
        viewModel.uiState.first { it is HomeUiState.Loaded }

    private fun takenOn(
        id: Long,
        year: Int,
    ): MediaItem =
        aMediaItem(id = id, takenAt = LocalDateTime.of(today.withYear(year), NOON).atZone(FixedClock.PARIS).toInstant())

    /** Galerie qui échoue à la première lecture, comme un MediaStore indisponible un instant. */
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
    fun `keeps showing the counts while they are recomputed on return`() =
        runTest {
            val viewModel = homeViewModel(FakeMediaSource(listOf(takenOn(1, 2025))))
            val states = mutableListOf<HomeUiState>()
            backgroundScope.launch(
                UnconfinedTestDispatcher(testScheduler),
            ) { viewModel.uiState.collect { states += it } }

            viewModel.onEvent(HomeEvent.Resumed)

            assertEquals(listOf(HomeUiState.Loading, HomeUiState.Loaded(ReviewDay(today, remainingCount = 1))), states)
        }
}
