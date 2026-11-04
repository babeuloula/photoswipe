package fr.babeuloula.photoswipe.di

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.data.media.MediaStoreMediaSource
import fr.babeuloula.photoswipe.domain.port.MediaSource
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Inject

/** Variante release : la galerie vient toujours de MediaStore, sans données de démo. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class MediaSourceBindingTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var mediaSource: MediaSource

    @Before
    fun inject() {
        hiltRule.inject()
    }

    @Test
    fun `reads the gallery from MediaStore in release`() {
        assertTrue(mediaSource is MediaStoreMediaSource)
    }

    @Test
    fun `ships no developer tools in release`() {
        assertTrue(runCatching { Class.forName("fr.babeuloula.photoswipe.debug.DeveloperToolsActivity") }.isFailure)
    }
}
