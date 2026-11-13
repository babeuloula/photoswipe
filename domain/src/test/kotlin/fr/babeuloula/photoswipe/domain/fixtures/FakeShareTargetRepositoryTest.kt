package fr.babeuloula.photoswipe.domain.fixtures

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** `FakeShareTargetRepository` respecte le contrat de son port. */
class FakeShareTargetRepositoryTest {
    @Test
    fun `knows no app before a first share, then the last one chosen`() =
        runTest {
            val repository = FakeShareTargetRepository()
            assertNull(repository.lastTarget())

            repository.saveLastTarget("com.whatsapp/.ContactPicker")
            repository.saveLastTarget("org.telegram.messenger/.LaunchActivity")

            assertEquals("org.telegram.messenger/.LaunchActivity", repository.lastTarget())
        }
}
