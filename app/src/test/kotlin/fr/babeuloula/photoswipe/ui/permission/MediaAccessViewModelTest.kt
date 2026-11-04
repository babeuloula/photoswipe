package fr.babeuloula.photoswipe.ui.permission

import fr.babeuloula.photoswipe.domain.model.MediaAccess
import fr.babeuloula.photoswipe.permission.MediaAccessReader
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaAccessViewModelTest {
    /** Accès simulé, que le test change comme l'utilisateur dans les réglages. */
    private var access = MediaAccess.NONE
    private val reader = MediaAccessReader { access }

    @Test
    fun `starts on the explanation without media access`() {
        assertEquals(MediaAccessUiState.Rationale, MediaAccessViewModel(reader).uiState.value)
    }

    @Test
    fun `starts on the app when media access is already granted, even partially`() {
        listOf(MediaAccess.FULL, MediaAccess.PARTIAL).forEach { granted ->
            access = granted

            assertEquals(MediaAccessUiState.Granted, MediaAccessViewModel(reader).uiState.value)
        }
    }

    @Test
    fun `launches the system request when the user continues`() =
        runTest {
            val viewModel = MediaAccessViewModel(reader)

            viewModel.onEvent(MediaAccessEvent.RequestAccess)

            assertEquals(MediaAccessEffect.LaunchPermissionRequest, viewModel.uiEffects.first())
        }

    @Test
    fun `shows the app once access is granted`() {
        val viewModel = MediaAccessViewModel(reader)
        access = MediaAccess.FULL

        viewModel.onEvent(MediaAccessEvent.RequestAnswered(canAskAgain = false))

        assertEquals(MediaAccessUiState.Granted, viewModel.uiState.value)
    }

    @Test
    fun `explains the refusal and lets the user try again while android still asks`() {
        val viewModel = MediaAccessViewModel(reader)

        viewModel.onEvent(MediaAccessEvent.RequestAnswered(canAskAgain = true))

        assertEquals(MediaAccessUiState.Denied(canAskAgain = true), viewModel.uiState.value)
    }

    @Test
    fun `offers only the settings after a permanent refusal`() =
        runTest {
            val viewModel = MediaAccessViewModel(reader)

            viewModel.onEvent(MediaAccessEvent.RequestAnswered(canAskAgain = false))
            viewModel.onEvent(MediaAccessEvent.OpenSettings)

            assertEquals(MediaAccessUiState.Denied(canAskAgain = false), viewModel.uiState.value)
            assertEquals(MediaAccessEffect.OpenAppSettings, viewModel.uiEffects.first())
        }

    @Test
    fun `shows the app when access was granted from the settings`() {
        val viewModel = MediaAccessViewModel(reader)
        viewModel.onEvent(MediaAccessEvent.RequestAnswered(canAskAgain = false))
        access = MediaAccess.FULL

        viewModel.onEvent(MediaAccessEvent.Resumed)

        assertEquals(MediaAccessUiState.Granted, viewModel.uiState.value)
    }

    @Test
    fun `goes back to the explanation when access is revoked while using the app`() {
        access = MediaAccess.FULL
        val viewModel = MediaAccessViewModel(reader)
        access = MediaAccess.NONE

        viewModel.onEvent(MediaAccessEvent.Resumed)

        assertEquals(MediaAccessUiState.Rationale, viewModel.uiState.value)
    }

    @Test
    fun `stays on the refusal when the user comes back without granting access`() {
        val viewModel = MediaAccessViewModel(reader)
        viewModel.onEvent(MediaAccessEvent.RequestAnswered(canAskAgain = true))

        viewModel.onEvent(MediaAccessEvent.Resumed)

        assertEquals(MediaAccessUiState.Denied(canAskAgain = true), viewModel.uiState.value)
    }
}
