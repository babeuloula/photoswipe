package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakeSettingsRepository
import fr.babeuloula.photoswipe.domain.model.ReleaseVersion
import fr.babeuloula.photoswipe.domain.model.Tutorial
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** `CheckWhatsNew`, `MarkWhatsNewSeen` et `RecordFirstInstall` (PS-173). */
class WhatsNewTest {
    private val v130 = ReleaseVersion(1, 3, 0)
    private val v140 = ReleaseVersion(1, 4, 0)
    private val v150 = ReleaseVersion(1, 5, 0)
    private val v160 = ReleaseVersion(1, 6, 0)
    private val releases = listOf(v140, v160, v150)

    /** Utilisateur qui a déjà vu le tutoriel, avec la dernière version vue [seen]. */
    private fun existingUser(seen: String?) =
        FakeSettingsRepository(tutorialVersionSeen = Tutorial.CURRENT_VERSION, whatsNewVersionSeen = seen)

    @Test
    fun `shows nothing on the first install and remembers the installed version`() =
        runTest {
            val settings = FakeSettingsRepository()

            assertEquals(emptyList<ReleaseVersion>(), CheckWhatsNew(settings, "1.6.0", releases)())

            assertEquals("1.6.0", settings.whatsNewVersionSeen)
        }

    @Test
    fun `shows the news of the one missed version`() =
        runTest {
            val settings = existingUser(seen = "1.5.0")

            assertEquals(listOf(v160), CheckWhatsNew(settings, "1.6.0", releases)())
        }

    @Test
    fun `shows every missed version, the most recent first`() =
        runTest {
            val settings = existingUser(seen = "1.3.0")

            assertEquals(listOf(v160, v150, v140), CheckWhatsNew(settings, "1.6.0", releases)())
        }

    @Test
    fun `shows the news to a user updating from a version that did not record what was seen`() =
        runTest {
            val settings = existingUser(seen = null)

            assertEquals(listOf(v150, v140), CheckWhatsNew(settings, "1.5.0", releases)())
        }

    @Test
    fun `keeps showing the news until they are marked as seen`() =
        runTest {
            val settings = existingUser(seen = "1.3.0")
            CheckWhatsNew(settings, "1.4.0", releases)()

            assertEquals(listOf(v140), CheckWhatsNew(settings, "1.4.0", releases)())

            MarkWhatsNewSeen(settings, "1.4.0")()

            assertEquals("1.4.0", settings.whatsNewVersionSeen)
            assertEquals(emptyList<ReleaseVersion>(), CheckWhatsNew(settings, "1.4.0", releases)())
        }

    @Test
    fun `shows nothing for an update without news and remembers it`() =
        runTest {
            val settings = existingUser(seen = "1.4.0")

            assertEquals(emptyList<ReleaseVersion>(), CheckWhatsNew(settings, "1.4.1", releases)())

            assertEquals("1.4.1", settings.whatsNewVersionSeen)
        }

    @Test
    fun `never shows the news of a version not yet published to a development build`() =
        runTest {
            val settings = existingUser(seen = "1.3.0")

            assertEquals(emptyList<ReleaseVersion>(), CheckWhatsNew(settings, "1.3.0-dev.9", releases)())
            assertEquals(emptyList<ReleaseVersion>(), CheckWhatsNew(settings, "0.0.0-dev", releases)())
            assertEquals("1.3.0", settings.whatsNewVersionSeen)
        }

    @Test
    fun `shows the published news to a development build made after them`() =
        runTest {
            val settings = existingUser(seen = "1.3.0")

            assertEquals(listOf(v140), CheckWhatsNew(settings, "1.4.0-dev.2", releases)())
        }

    @Test
    fun `shows nothing when the installed version cannot be read`() =
        runTest {
            val settings = existingUser(seen = null)

            assertEquals(emptyList<ReleaseVersion>(), CheckWhatsNew(settings, "unknown", releases)())
            assertNull(settings.whatsNewVersionSeen)
        }

    @Test
    fun `never moves the seen version back`() =
        runTest {
            val settings = existingUser(seen = "1.6.0")

            MarkWhatsNewSeen(settings, "1.4.0")()
            MarkWhatsNewSeen(settings, "0.0.0-dev")()

            assertEquals("1.6.0", settings.whatsNewVersionSeen)
        }

    @Test
    fun `records the first install only when nothing was ever seen`() =
        runTest {
            val fresh = FakeSettingsRepository()
            assertTrue(RecordFirstInstall(fresh, "1.4.0")())
            assertEquals("1.4.0", fresh.whatsNewVersionSeen)
            assertFalse(RecordFirstInstall(fresh, "1.5.0")())
            assertEquals("1.4.0", fresh.whatsNewVersionSeen)

            val updating = existingUser(seen = null)
            assertFalse(RecordFirstInstall(updating, "1.4.0")())
            assertNull(updating.whatsNewVersionSeen)
        }

    @Test
    fun `keeps a release listed twice once`() =
        runTest {
            val settings = existingUser(seen = "1.3.0")

            assertEquals(listOf(v140), CheckWhatsNew(settings, "1.4.0", listOf(v140, v140, v130))())
        }
}
