package fr.babeuloula.photoswipe.ui.permission

import fr.babeuloula.photoswipe.MainDispatcherRule
import fr.babeuloula.photoswipe.domain.fixtures.FakeDecisionJournal
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.MediaAccess
import fr.babeuloula.photoswipe.domain.usecase.CleanOrphanEntries
import fr.babeuloula.photoswipe.permission.MediaAccessReader
import fr.babeuloula.photoswipe.permission.OrphanCleanup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MediaAccessViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    /** Accès simulé, que le test change comme l'utilisateur dans les réglages. */
    private var access = MediaAccess.NONE
    private val reader = MediaAccessReader { access }

    /** Décisions : le média 1 est dans la galerie, le média 2 a disparu (orphelin). */
    private val reviews = FakeReviewRepository(listOf(aReviewEntry(mediaId = 1), aReviewEntry(mediaId = 2)))
    private val gallery = FakeMediaSource(listOf(aMediaItem(id = 1)))

    /** Faux tant que le test simule les données de démo du debug. */
    private var realGallery = true

    /** Nettoyage partagé par les ViewModels d'un test, comme le singleton du process. */
    private val cleanup =
        OrphanCleanup(CleanOrphanEntries(reviews, gallery, FakeDecisionJournal(reviews)), reader) { realGallery }

    private fun viewModel() = MediaAccessViewModel(reader, cleanup)

    private fun knownIds() = reviews.entries.map { it.mediaId.value }

    @Test
    fun `cleans the orphan entries at startup once access is granted`() {
        access = MediaAccess.FULL

        viewModel()

        assertEquals(listOf(1L), knownIds())
    }

    @Test
    fun `waits for the access before cleaning at startup`() {
        val viewModel = viewModel()
        assertEquals(listOf(1L, 2L), knownIds())

        access = MediaAccess.FULL
        viewModel.onEvent(MediaAccessEvent.RequestAnswered(canAskAgain = true))

        assertEquals(listOf(1L), knownIds())
    }

    @Test
    fun `cleans at startup only once per process`() =
        runTest {
            access = MediaAccess.FULL
            viewModel()
            reviews.save(aReviewEntry(mediaId = 3))

            viewModel()

            assertEquals(listOf(1L, 3L), knownIds())
        }

    @Test
    fun `keeps every entry while the demo data replace the gallery`() {
        access = MediaAccess.FULL
        realGallery = false

        viewModel()

        assertEquals(listOf(1L, 2L), knownIds())
    }

    @Test
    fun `keeps every entry with a partial access`() {
        access = MediaAccess.PARTIAL

        viewModel()

        assertEquals(listOf(1L, 2L), knownIds())
    }

    @Test
    fun `starts on the explanation without media access`() {
        assertEquals(MediaAccessUiState.Rationale, viewModel().uiState.value)
    }

    @Test
    fun `starts on the app when media access is already granted, even partially`() {
        access = MediaAccess.FULL
        assertEquals(MediaAccessUiState.Granted(partial = false), viewModel().uiState.value)

        access = MediaAccess.PARTIAL
        assertEquals(MediaAccessUiState.Granted(partial = true), viewModel().uiState.value)
    }

    @Test
    fun `launches the system request when the user continues`() =
        runTest {
            val viewModel = viewModel()

            viewModel.onEvent(MediaAccessEvent.RequestAccess)

            assertEquals(MediaAccessEffect.LaunchPermissionRequest, viewModel.uiEffects.first())
        }

    @Test
    fun `shows the app once access is granted`() {
        val viewModel = viewModel()
        access = MediaAccess.FULL

        viewModel.onEvent(MediaAccessEvent.RequestAnswered(canAskAgain = false))

        assertEquals(MediaAccessUiState.Granted(partial = false), viewModel.uiState.value)
    }

    @Test
    fun `explains the refusal and lets the user try again while android still asks`() {
        val viewModel = viewModel()

        viewModel.onEvent(MediaAccessEvent.RequestAnswered(canAskAgain = true))

        assertEquals(MediaAccessUiState.Denied(canAskAgain = true), viewModel.uiState.value)
    }

    @Test
    fun `offers only the settings after a permanent refusal`() =
        runTest {
            val viewModel = viewModel()

            viewModel.onEvent(MediaAccessEvent.RequestAnswered(canAskAgain = false))
            viewModel.onEvent(MediaAccessEvent.OpenSettings)

            assertEquals(MediaAccessUiState.Denied(canAskAgain = false), viewModel.uiState.value)
            assertEquals(MediaAccessEffect.OpenAppSettings, viewModel.uiEffects.first())
        }

    @Test
    fun `shows the app when access was granted from the settings`() {
        val viewModel = viewModel()
        viewModel.onEvent(MediaAccessEvent.RequestAnswered(canAskAgain = false))
        access = MediaAccess.FULL

        viewModel.onEvent(MediaAccessEvent.Resumed)

        assertEquals(MediaAccessUiState.Granted(partial = false), viewModel.uiState.value)
    }

    @Test
    fun `goes back to the explanation when access is revoked while using the app`() {
        access = MediaAccess.FULL
        val viewModel = viewModel()
        access = MediaAccess.NONE

        viewModel.onEvent(MediaAccessEvent.Resumed)

        assertEquals(MediaAccessUiState.Rationale, viewModel.uiState.value)
    }

    @Test
    fun `stays on the refusal when the user comes back without granting access`() {
        val viewModel = viewModel()
        viewModel.onEvent(MediaAccessEvent.RequestAnswered(canAskAgain = true))

        viewModel.onEvent(MediaAccessEvent.Resumed)

        assertEquals(MediaAccessUiState.Denied(canAskAgain = true), viewModel.uiState.value)
    }

    @Test
    fun `relaunches the system request to extend a limited selection`() =
        runTest {
            access = MediaAccess.PARTIAL
            val viewModel = viewModel()

            viewModel.onEvent(MediaAccessEvent.ExtendSelection)

            assertEquals(MediaAccessEffect.LaunchPermissionRequest, viewModel.uiEffects.first())
        }

    @Test
    fun `drops the limited access banner once full access is granted`() {
        access = MediaAccess.PARTIAL
        val viewModel = viewModel()
        access = MediaAccess.FULL

        viewModel.onEvent(MediaAccessEvent.RequestAnswered(canAskAgain = false))

        assertEquals(MediaAccessUiState.Granted(partial = false), viewModel.uiState.value)
    }

    @Test
    fun `shows the limited access banner when the selection is reduced from the settings`() {
        access = MediaAccess.FULL
        val viewModel = viewModel()
        access = MediaAccess.PARTIAL

        viewModel.onEvent(MediaAccessEvent.Resumed)

        assertEquals(MediaAccessUiState.Granted(partial = true), viewModel.uiState.value)
    }
}
