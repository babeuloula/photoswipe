package fr.babeuloula.photoswipe.ui.dayoverview

import androidx.lifecycle.SavedStateHandle
import fr.babeuloula.photoswipe.MainDispatcherRule
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.MediaType
import fr.babeuloula.photoswipe.domain.model.ReviewDecision
import fr.babeuloula.photoswipe.domain.port.MediaSource
import fr.babeuloula.photoswipe.domain.service.RecentYearsFirstOrdering
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import fr.babeuloula.photoswipe.domain.usecase.GetDayOverview
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime.NOON

/** Robolectric : la route typée se relit depuis le `SavedStateHandle` avec le `Bundle` d'Android. */
@RunWith(RobolectricTestRunner::class)
class DayOverviewViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val day = LocalDate.of(2026, 5, 14)
    private val clock = FixedClock.at(day.atTime(10, 0))
    private val repository = FakeReviewRepository()

    private fun takenOn(
        id: Long,
        year: Int,
        type: MediaType = MediaType.IMAGE,
    ): MediaItem =
        aMediaItem(
            id = id,
            type = type,
            duration = if (type == MediaType.VIDEO) Duration.ofSeconds(5) else null,
            takenAt =
                day
                    .withYear(year)
                    .atTime(NOON)
                    .atZone(clock.zone)
                    .toInstant(),
        )

    private fun viewModel(source: MediaSource): DayOverviewViewModel =
        DayOverviewViewModel(
            SavedStateHandle(mapOf("epochDay" to day.toEpochDay())),
            GetDayOverview(SameDayMatcher(), source, repository, RecentYearsFirstOrdering(), clock),
            clock,
        )

    private fun DayOverviewViewModel.loaded() = uiState.value as DayOverviewUiState.Loaded

    @Test
    fun `shows every memory of the day by year, with how many are left to sort`() =
        runTest {
            repository.save(aReviewEntry(mediaId = 2, reviewYear = 2026, decision = ReviewDecision.KEPT))
            val viewModel = viewModel(FakeMediaSource(listOf(takenOn(1, 2020), takenOn(2, 2024), takenOn(3, 2024))))

            assertEquals(day, viewModel.day)
            assertEquals(
                listOf(2024, 2020),
                viewModel
                    .loaded()
                    .overview.years
                    .map { it.year },
            )
            assertEquals(2, viewModel.loaded().remainingCount)
        }

    @Test
    fun `opens the deck on a memory left to sort, or the whole deck from the sort button`() =
        runTest {
            val viewModel = viewModel(FakeMediaSource(listOf(takenOn(1, 2020), takenOn(2, 2024))))

            viewModel.onEvent(DayOverviewEvent.Open(MediaId(1)))
            assertEquals(DayOverviewEffect.OpenDeck(day, MediaId(1)), viewModel.uiEffects.first())

            viewModel.onEvent(DayOverviewEvent.StartSorting)
            assertEquals(DayOverviewEffect.OpenDeck(day), viewModel.uiEffects.first())
        }

    @Test
    fun `opens a photo already sorted in full, but not a sorted video`() =
        runTest {
            repository.save(aReviewEntry(mediaId = 1, reviewYear = 2026, decision = ReviewDecision.KEPT))
            repository.save(aReviewEntry(mediaId = 2, reviewYear = 2026, decision = ReviewDecision.KEPT))
            val viewModel =
                viewModel(FakeMediaSource(listOf(takenOn(1, 2020), takenOn(2, 2024, MediaType.VIDEO))))

            viewModel.onEvent(DayOverviewEvent.Open(MediaId(2)))
            assertEquals(null, viewModel.loaded().openPhoto)

            viewModel.onEvent(DayOverviewEvent.Open(MediaId(1)))
            assertEquals(MediaId(1), viewModel.loaded().openPhoto?.id)

            viewModel.onEvent(DayOverviewEvent.ClosePhoto)
            assertEquals(null, viewModel.loaded().openPhoto)
        }

    @Test
    fun `reads the decisions again when coming back from the deck`() =
        runTest {
            val viewModel = viewModel(FakeMediaSource(listOf(takenOn(1, 2020))))
            repository.save(aReviewEntry(mediaId = 1, reviewYear = 2026, decision = ReviewDecision.MARKED_FOR_DELETION))

            viewModel.onEvent(DayOverviewEvent.Resumed)

            assertEquals(
                ReviewDecision.MARKED_FOR_DELETION,
                viewModel
                    .loaded()
                    .overview.years
                    .single()
                    .memories
                    .single()
                    .decision,
            )
        }

    @Test
    fun `shows an error when the gallery cannot be read, then recovers on retry`() =
        runTest {
            val gallery = listOf(takenOn(1, 2020))
            var fail = true
            val source =
                object : MediaSource by FakeMediaSource(gallery) {
                    override suspend fun mediaTakenIn(ranges: List<OpenEndRange<Instant>>): List<MediaItem> {
                        if (fail) throw IOException("unreadable")
                        return gallery
                    }
                }
            val viewModel = viewModel(source)
            assertEquals(DayOverviewUiState.Error, viewModel.uiState.value)

            fail = false
            viewModel.onEvent(DayOverviewEvent.Retry)

            assertEquals(1, viewModel.loaded().overview.memoryCount)
        }

    @Test
    fun `goes back with the back arrow`() =
        runTest {
            val viewModel = viewModel(FakeMediaSource())

            viewModel.onEvent(DayOverviewEvent.Back)

            assertEquals(DayOverviewEffect.Close, viewModel.uiEffects.first())
        }
}
