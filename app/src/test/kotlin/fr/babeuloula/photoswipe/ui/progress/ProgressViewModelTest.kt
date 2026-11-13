package fr.babeuloula.photoswipe.ui.progress

import fr.babeuloula.photoswipe.MainDispatcherRule
import fr.babeuloula.photoswipe.domain.fixtures.FakeUsageStatisticsRepository
import fr.babeuloula.photoswipe.domain.model.UsageStatistics
import fr.babeuloula.photoswipe.domain.port.UsageStatisticsRepository
import fr.babeuloula.photoswipe.domain.usecase.GetUsageStatistics
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

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

    private fun progressViewModel() = ProgressViewModel(GetUsageStatistics(repository))

    @Test
    fun `shows the usage statistics`() =
        runTest {
            statistics.recordDeletion(12, 34_000_000)

            val state = progressViewModel().uiState.first { it !is ProgressUiState.Loading }

            assertEquals(ProgressUiState.Loaded(UsageStatistics(since, 12, 34_000_000)), state)
        }

    @Test
    fun `reads the statistics again when coming back to the page`() =
        runTest {
            val viewModel = progressViewModel()
            viewModel.uiState.first { it is ProgressUiState.Loaded }
            statistics.recordDeletion(3, 9_000_000)

            viewModel.onEvent(ProgressEvent.Resumed)

            val state = viewModel.uiState.first { it == ProgressUiState.Loaded(UsageStatistics(since, 3, 9_000_000)) }
            assertEquals(ProgressUiState.Loaded(UsageStatistics(since, 3, 9_000_000)), state)
        }

    @Test
    fun `shows an error when the statistics cannot be read, then loads them on retry`() =
        runTest {
            failing = true
            val viewModel = progressViewModel()
            assertEquals(ProgressUiState.Error, viewModel.uiState.first { it !is ProgressUiState.Loading })

            failing = false
            viewModel.onEvent(ProgressEvent.Retry)

            assertEquals(
                ProgressUiState.Loaded(UsageStatistics(since)),
                viewModel.uiState.first { it is ProgressUiState.Loaded },
            )
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
    fun `closes the page`() =
        runTest {
            val viewModel = progressViewModel()

            viewModel.onEvent(ProgressEvent.Back)

            assertEquals(ProgressEffect.Close, viewModel.uiEffects.first())
        }
}
