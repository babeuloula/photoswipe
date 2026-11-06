package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakeSettingsRepository
import fr.babeuloula.photoswipe.domain.model.Tutorial
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** `ShouldShowTutorial` et `MarkTutorialSeen`. */
class TutorialUseCasesTest {
    @Test
    fun `shows the tutorial on the first launch`() =
        runTest {
            assertTrue(ShouldShowTutorial(FakeSettingsRepository())())
        }

    @Test
    fun `no longer shows the tutorial once its current version was seen`() =
        runTest {
            val settings = FakeSettingsRepository()

            MarkTutorialSeen(settings)()

            assertEquals(Tutorial.CURRENT_VERSION, settings.tutorialVersionSeen)
            assertFalse(ShouldShowTutorial(settings)())
        }

    @Test
    fun `shows the tutorial again when its version is incremented`() =
        runTest {
            val settings = FakeSettingsRepository(tutorialVersionSeen = 1)

            assertFalse(ShouldShowTutorial(settings, currentVersion = 1)())
            assertTrue(ShouldShowTutorial(settings, currentVersion = 2)())

            MarkTutorialSeen(settings, currentVersion = 2)()

            assertFalse(ShouldShowTutorial(settings, currentVersion = 2)())
        }
}
