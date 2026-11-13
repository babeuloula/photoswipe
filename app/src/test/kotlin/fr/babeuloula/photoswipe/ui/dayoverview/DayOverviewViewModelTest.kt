package fr.babeuloula.photoswipe.ui.dayoverview

import androidx.lifecycle.SavedStateHandle
import fr.babeuloula.photoswipe.MainDispatcherRule
import fr.babeuloula.photoswipe.domain.fixtures.FakeDecisionJournal
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
import fr.babeuloula.photoswipe.domain.usecase.KeepMedia
import fr.babeuloula.photoswipe.domain.usecase.MarkForDeletion
import fr.babeuloula.photoswipe.domain.usecase.SortInBatch
import fr.babeuloula.photoswipe.domain.usecase.UndoLastDecision
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
    private val journal = FakeDecisionJournal(repository)

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
            SortInBatch(
                KeepMedia(repository, journal, clock),
                MarkForDeletion(repository, journal, clock),
                UndoLastDecision(repository, journal),
            ),
            clock,
        )

    private suspend fun decisionOf(id: Long): ReviewDecision? =
        repository.entriesFor(2026, setOf(MediaId(id))).singleOrNull()?.decision

    @Test
    fun `selects memories by a long press, then a press toggles instead of opening`() =
        runTest {
            val viewModel = viewModel(FakeMediaSource(listOf(takenOn(1, 2020), takenOn(2, 2024))))

            viewModel.onEvent(DayOverviewEvent.ToggleSelection(MediaId(1)))
            viewModel.onEvent(DayOverviewEvent.Open(MediaId(2)))
            assertEquals(setOf(MediaId(1), MediaId(2)), viewModel.loaded().selected)

            viewModel.onEvent(DayOverviewEvent.Open(MediaId(1)))
            assertEquals(setOf(MediaId(2)), viewModel.loaded().selected)
        }

    @Test
    fun `selects or unselects a whole year from its header`() =
        runTest {
            val viewModel = viewModel(FakeMediaSource(listOf(takenOn(1, 2024), takenOn(2, 2024), takenOn(3, 2020))))
            viewModel.onEvent(DayOverviewEvent.ToggleSelection(MediaId(3)))

            viewModel.onEvent(DayOverviewEvent.ToggleYear(2024))
            assertEquals(setOf(MediaId(1), MediaId(2), MediaId(3)), viewModel.loaded().selected)

            viewModel.onEvent(DayOverviewEvent.ToggleYear(2024))
            assertEquals(setOf(MediaId(3)), viewModel.loaded().selected)
        }

    @Test
    fun `leaves the selection with back, without closing the screen`() =
        runTest {
            val viewModel = viewModel(FakeMediaSource(listOf(takenOn(1, 2020))))
            viewModel.onEvent(DayOverviewEvent.ToggleSelection(MediaId(1)))

            viewModel.onEvent(DayOverviewEvent.Back)

            assertEquals(emptySet<MediaId>(), viewModel.loaded().selected)
            viewModel.onEvent(DayOverviewEvent.Back)
            assertEquals(DayOverviewEffect.Close, viewModel.uiEffects.first())
        }

    @Test
    fun `keeps the selected memories, announces the batch and undoes it as a whole`() =
        runTest {
            repository.save(aReviewEntry(mediaId = 2, reviewYear = 2026, decision = ReviewDecision.MARKED_FOR_DELETION))
            val viewModel = viewModel(FakeMediaSource(listOf(takenOn(1, 2020), takenOn(2, 2024), takenOn(3, 2024))))
            viewModel.onEvent(DayOverviewEvent.ToggleSelection(MediaId(1)))
            viewModel.onEvent(DayOverviewEvent.ToggleSelection(MediaId(2)))

            viewModel.onEvent(DayOverviewEvent.KeepSelected)

            assertEquals(DayOverviewEffect.BatchSorted(count = 2, kept = true), viewModel.uiEffects.first())
            assertEquals(ReviewDecision.KEPT, decisionOf(1))
            assertEquals(ReviewDecision.KEPT, decisionOf(2))
            assertEquals(null, decisionOf(3))
            assertEquals(emptySet<MediaId>(), viewModel.loaded().selected)
            assertEquals(1, viewModel.loaded().remainingCount)

            viewModel.onEvent(DayOverviewEvent.UndoBatch)

            assertEquals(null, decisionOf(1))
            assertEquals(ReviewDecision.MARKED_FOR_DELETION, decisionOf(2))
            assertEquals(2, viewModel.loaded().remainingCount)
        }

    @Test
    fun `marks the selected memories for deletion`() =
        runTest {
            val viewModel = viewModel(FakeMediaSource(listOf(takenOn(1, 2020), takenOn(2, 2024))))
            viewModel.onEvent(DayOverviewEvent.ToggleYear(2020))

            viewModel.onEvent(DayOverviewEvent.MarkSelected)

            assertEquals(DayOverviewEffect.BatchSorted(count = 1, kept = false), viewModel.uiEffects.first())
            assertEquals(ReviewDecision.MARKED_FOR_DELETION, decisionOf(1))
            assertEquals(null, decisionOf(2))
        }

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
