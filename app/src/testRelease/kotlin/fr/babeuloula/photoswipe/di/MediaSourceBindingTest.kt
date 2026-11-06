package fr.babeuloula.photoswipe.di

import fr.babeuloula.photoswipe.data.media.MediaStoreMediaSource
import fr.babeuloula.photoswipe.domain.port.MediaSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Variante release : la galerie vient toujours de MediaStore, sans outils développeur.
 *
 * Le test lit la liaison du module release par réflexion, sans graphe Hilt : le composant de test
 * Hilt de la variante release se perdait parfois au cache de build en CI (`ClassNotFoundException`).
 */
@RunWith(RobolectricTestRunner::class)
class MediaSourceBindingTest {
    @Test
    fun `binds MediaStore as the media source in release`() {
        val binding =
            MediaSourceModule::class.java.getDeclaredMethod(
                "bindMediaSource",
                MediaStoreMediaSource::class.java,
            )

        assertEquals(MediaSource::class.java, binding.returnType)
    }

    @Test
    fun `ships no developer tools in release`() {
        listOf(
            "fr.babeuloula.photoswipe.debug.DeveloperToolsActivity",
            "fr.babeuloula.photoswipe.debug.DemoMediaCatalog",
            "fr.babeuloula.photoswipe.di.SwitchableMediaSource",
            "fr.babeuloula.photoswipe.debug.DeveloperSettings",
            "fr.babeuloula.photoswipe.debug.DecisionsReset",
        ).forEach { name -> assertTrue(name, runCatching { Class.forName(name) }.isFailure) }
    }

    @Test
    fun `offers no developer tools entry in the settings in release`() {
        val entry = DeveloperToolsModule.provideDeveloperToolsEntry()

        assertNull(entry.intent(RuntimeEnvironment.getApplication()))
    }
}
