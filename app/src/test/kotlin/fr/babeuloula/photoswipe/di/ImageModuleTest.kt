package fr.babeuloula.photoswipe.di

import coil3.video.VideoFrameDecoder
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Chargeur d'images de l'application (ADR 0016). */
@RunWith(RobolectricTestRunner::class)
class ImageModuleTest {
    @Test
    fun `decodes a frame of each video for its thumbnail`() {
        val loader = ImageModule.provideImageLoader(RuntimeEnvironment.getApplication())

        assertTrue(loader.components.decoderFactories.any { it is VideoFrameDecoder.Factory })
    }
}
