package fr.babeuloula.photoswipe.ui.crash

import fr.babeuloula.photoswipe.MainDispatcherRule
import fr.babeuloula.photoswipe.crash.CrashReportStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CrashReportViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    @get:Rule
    val folder = TemporaryFolder()

    private val store by lazy { CrashReportStore(File(folder.root, "crash"), Dispatchers.Unconfined) }

    @Test
    fun `asks nothing when the app did not crash`() {
        assertNull(CrashReportViewModel(store).pendingReport.value)
    }

    @Test
    fun `offers the report of the previous crash, then opens the sending page and forgets it`() =
        runTest {
            store.save("report")
            val viewModel = CrashReportViewModel(store)
            val effects = mutableListOf<CrashReportEffect>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEffects.toList(effects) }

            assertEquals("report", viewModel.pendingReport.value)
            viewModel.onEvent(CrashReportEvent.Send)

            assertNull(viewModel.pendingReport.value)
            assertEquals(listOf<CrashReportEffect>(CrashReportEffect.OpenReport("report")), effects)
            assertNull(store.pending())
        }

    @Test
    fun `forgets an ignored report without opening anything`() =
        runTest {
            store.save("report")
            val viewModel = CrashReportViewModel(store)
            val effects = mutableListOf<CrashReportEffect>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEffects.toList(effects) }

            viewModel.onEvent(CrashReportEvent.Dismiss)
            viewModel.onEvent(CrashReportEvent.Send)

            assertNull(viewModel.pendingReport.value)
            assertEquals(emptyList<CrashReportEffect>(), effects)
            assertNull(store.pending())
        }
}
