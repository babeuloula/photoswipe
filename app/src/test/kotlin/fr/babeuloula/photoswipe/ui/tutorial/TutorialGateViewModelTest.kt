package fr.babeuloula.photoswipe.ui.tutorial

import fr.babeuloula.photoswipe.MainDispatcherRule
import fr.babeuloula.photoswipe.domain.fixtures.FakeSettingsRepository
import fr.babeuloula.photoswipe.domain.model.Tutorial
import fr.babeuloula.photoswipe.domain.port.SettingsRepository
import fr.babeuloula.photoswipe.domain.usecase.MarkTutorialSeen
import fr.babeuloula.photoswipe.domain.usecase.RecordFirstInstall
import fr.babeuloula.photoswipe.domain.usecase.ShouldShowTutorial
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TutorialGateViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private fun viewModel(settings: SettingsRepository) =
        TutorialGateViewModel(
            ShouldShowTutorial(settings),
            MarkTutorialSeen(settings),
            RecordFirstInstall(settings, "1.4.0"),
        )

    @Test
    fun `shows the tutorial on the first launch, without marking it seen yet`() {
        val settings = FakeSettingsRepository()

        val viewModel = viewModel(settings)

        assertEquals(TutorialGateState.SHOWING, viewModel.uiState.value)
        assertEquals(Tutorial.NEVER_SEEN, settings.tutorialVersionSeen)
    }

    @Test
    fun `goes straight to the app once the tutorial was seen`() {
        val viewModel = viewModel(FakeSettingsRepository(tutorialVersionSeen = Tutorial.CURRENT_VERSION))

        assertEquals(TutorialGateState.DONE, viewModel.uiState.value)
    }

    @Test
    fun `remembers the tutorial as seen once skipped or finished, and goes on to the app`() {
        val settings = FakeSettingsRepository()
        val viewModel = viewModel(settings)

        viewModel.onTutorialDone()

        assertEquals(TutorialGateState.DONE, viewModel.uiState.value)
        assertEquals(Tutorial.CURRENT_VERSION, settings.tutorialVersionSeen)
    }

    @Test
    fun `records the installed version before the first tutorial, so a first install announces nothing`() {
        val settings = FakeSettingsRepository()

        val viewModel = viewModel(settings)
        viewModel.onTutorialDone()

        assertEquals("1.4.0", settings.whatsNewVersionSeen)
    }

    @Test
    fun `leaves the news of an update to the home screen when the tutorial was already seen`() {
        val settings = FakeSettingsRepository(tutorialVersionSeen = Tutorial.CURRENT_VERSION)

        viewModel(settings)

        assertEquals(null, settings.whatsNewVersionSeen)
    }

    @Test
    fun `does not block the app when the settings cannot be read`() {
        val broken =
            object : SettingsRepository by FakeSettingsRepository() {
                override suspend fun tutorialVersionSeen(): Int = error("Fichier illisible")
            }

        assertEquals(TutorialGateState.DONE, viewModel(broken).uiState.value)
    }
}
