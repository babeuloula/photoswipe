package fr.babeuloula.photoswipe.ui.progress

import fr.babeuloula.photoswipe.MainDispatcherRule
import fr.babeuloula.photoswipe.domain.fixtures.FakeActivityHistory
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.FakeSettingsRepository
import fr.babeuloula.photoswipe.domain.fixtures.FakeUsageStatisticsRepository
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.model.ActivityReport
import fr.babeuloula.photoswipe.domain.model.DayMemories
import fr.babeuloula.photoswipe.domain.model.MonthlyActivity
import fr.babeuloula.photoswipe.domain.model.ReviewDay
import fr.babeuloula.photoswipe.domain.model.Streak
import fr.babeuloula.photoswipe.domain.model.UsageStatistics
import fr.babeuloula.photoswipe.domain.model.YearMemories
import fr.babeuloula.photoswipe.domain.port.UsageStatisticsRepository
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import fr.babeuloula.photoswipe.domain.usecase.GetActivityReport
import fr.babeuloula.photoswipe.domain.usecase.GetStreak
import fr.babeuloula.photoswipe.domain.usecase.GetUsageStatistics
import fr.babeuloula.photoswipe.domain.usecase.GetYearMemories
import fr.babeuloula.photoswipe.domain.usecase.UpdateStreak
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime.NOON
import java.time.YearMonth

class ProgressViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val since = LocalDate.of(2026, 3, 2)
    private val statistics = FakeUsageStatisticsRepository(since)

    /** Dépôt qui échoue tant que [failing] est vrai, puis délègue à [statistics]. */
    private var failing = false
    private val repository =
        object : UsageStatisticsRepository by statistics {
            override suspend fun statistics(): UsageStatistics {
                check(!failing) { "unreadable statistics" }
                return statistics.statistics()
            }
        }

    private val clock = FixedClock.at(LocalDateTime.of(2026, 7, 15, 21, 0))
    private val activity = FakeActivityHistory()
    private val settings = FakeSettingsRepository()

    private val gallery = FakeMediaSource()

    private fun progressViewModel() =
        ProgressViewModel(
            GetUsageStatistics(repository),
            GetActivityReport(activity, clock),
            GetStreak(settings),
            GetYearMemories(SameDayMatcher(), gallery, FakeReviewRepository(), clock),
            clock,
        )

    /** Statistiques [statistics] avec l'activité vide des six derniers mois et un calendrier sans souvenir. */
    private fun loaded(statistics: UsageStatistics) =
        ProgressUiState.Loaded(
            statistics,
            ActivityReport((2..7).map { MonthlyActivity(YearMonth.of(2026, it)) }),
            calendar = YearMemories(2026, emptyMap()),
            today = LocalDate.of(2026, 7, 15),
        )

    /** Premier état complet : chiffres lus et calendrier prêt, ou erreur. */
    private suspend fun ProgressViewModel.settled(): ProgressUiState =
        uiState.first { it is ProgressUiState.Error || (it as? ProgressUiState.Loaded)?.calendar != null }

    @Test
    fun `shows the usage statistics`() =
        runTest {
            statistics.recordDeletion(12, 34_000_000)

            val state = progressViewModel().settled()

            assertEquals(loaded(UsageStatistics(since, 12, 34_000_000)), state)
        }

    @Test
    fun `shows the activity of the last six months`() =
        runTest {
            activity.recordTrashed(LocalDate.of(2026, 7, 1), count = 4, bytes = 900)

            val state = progressViewModel().uiState.first { it is ProgressUiState.Loaded } as ProgressUiState.Loaded

            assertEquals(6, state.activity.months.size)
            assertEquals(
                MonthlyActivity(YearMonth.of(2026, 7), trashedCount = 4, freedBytes = 900),
                state.activity.months.last(),
            )
            assertEquals(LocalDate.of(2026, 7, 1), state.activity.since)
        }

    @Test
    fun `shows the streak last counted by the home screen`() =
        runTest {
            UpdateStreak(
                settings,
                clock,
            )(listOf(ReviewDay(LocalDate.of(2026, 7, 15), remainingCount = 0, memoryCount = 2)))

            val state = progressViewModel().uiState.first { it is ProgressUiState.Loaded } as ProgressUiState.Loaded

            assertEquals(Streak(current = 1, best = 1), state.streak)
        }

    @Test
    fun `reads the statistics again when coming back to the page`() =
        runTest {
            val viewModel = progressViewModel()
            viewModel.uiState.first { it is ProgressUiState.Loaded }
            statistics.recordDeletion(3, 9_000_000)

            viewModel.onEvent(ProgressEvent.Resumed)

            val state = viewModel.uiState.first { it == loaded(UsageStatistics(since, 3, 9_000_000)) }
            assertEquals(loaded(UsageStatistics(since, 3, 9_000_000)), state)
        }

    @Test
    fun `shows an error when the statistics cannot be read, then loads them on retry`() =
        runTest {
            failing = true
            val viewModel = progressViewModel()
            assertEquals(ProgressUiState.Error, viewModel.uiState.first { it !is ProgressUiState.Loading })

            failing = false
            viewModel.onEvent(ProgressEvent.Retry)

            assertEquals(loaded(UsageStatistics(since)), viewModel.settled())
        }

    @Test
    fun `keeps the figures on screen when a later read fails`() =
        runTest {
            val viewModel = progressViewModel()
            val loaded = viewModel.uiState.first { it is ProgressUiState.Loaded }
            failing = true

            viewModel.onEvent(ProgressEvent.Resumed)

            assertEquals(loaded, viewModel.uiState.value)
        }

    @Test
    fun `fills the calendar with the memories of every day of the year`() =
        runTest {
            gallery.add(
                aMediaItem(
                    id = 1,
                    takenAt =
                        LocalDate
                            .of(2023, 12, 25)
                            .atTime(NOON)
                            .atZone(FixedClock.PARIS)
                            .toInstant(),
                ),
            )

            val state = progressViewModel().settled() as ProgressUiState.Loaded

            assertEquals(
                DayMemories(memoryCount = 1, remainingCount = 1),
                state.calendar?.get(LocalDate.of(2026, 12, 25)),
            )
        }

    @Test
    fun `opens the overview of a day touched in the calendar`() =
        runTest {
            val viewModel = progressViewModel()

            viewModel.onEvent(ProgressEvent.OpenDay(LocalDate.of(2026, 12, 25)))

            assertEquals(ProgressEffect.OpenOverview(LocalDate.of(2026, 12, 25)), viewModel.uiEffects.first())
        }

    @Test
    fun `closes the page`() =
        runTest {
            val viewModel = progressViewModel()

            viewModel.onEvent(ProgressEvent.Back)

            assertEquals(ProgressEffect.Close, viewModel.uiEffects.first())
        }
}
