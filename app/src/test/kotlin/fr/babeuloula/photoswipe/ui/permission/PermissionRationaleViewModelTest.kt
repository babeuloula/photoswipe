package fr.babeuloula.photoswipe.ui.permission

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionRationaleViewModelTest {
    @Test
    fun `asks to request media access when the user continues`() =
        runTest {
            val viewModel = PermissionRationaleViewModel()

            viewModel.onEvent(PermissionRationaleEvent.Continue)

            assertEquals(PermissionRationaleEffect.RequestMediaAccess, viewModel.uiEffects.first())
        }

    @Test
    fun `delivers each request once`() =
        runTest {
            val viewModel = PermissionRationaleViewModel()
            val effects = mutableListOf<PermissionRationaleEffect>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEffects.toList(effects) }

            viewModel.onEvent(PermissionRationaleEvent.Continue)
            viewModel.onEvent(PermissionRationaleEvent.Continue)

            assertEquals(List(2) { PermissionRationaleEffect.RequestMediaAccess }, effects)
        }

    @Test
    fun `emits nothing before the user continues`() =
        runTest {
            val viewModel = PermissionRationaleViewModel()
            val effects = mutableListOf<PermissionRationaleEffect>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEffects.toList(effects) }

            assertEquals(emptyList<PermissionRationaleEffect>(), effects)
        }
}
