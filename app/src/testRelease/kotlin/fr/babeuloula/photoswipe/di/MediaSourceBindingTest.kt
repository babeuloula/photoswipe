package fr.babeuloula.photoswipe.di

import fr.babeuloula.photoswipe.data.media.MediaStoreMediaSource
import fr.babeuloula.photoswipe.domain.port.MediaSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Variante release : la galerie vient toujours de MediaStore, sans outils développeur.
 *
 * Le test lit la liaison du module release par réflexion, sans graphe Hilt : le composant de test
 * Hilt de la variante release se perdait parfois au cache de build en CI (`ClassNotFoundException`).
 */
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
        ).forEach { name -> assertTrue(name, runCatching { Class.forName(name) }.isFailure) }
    }
}
