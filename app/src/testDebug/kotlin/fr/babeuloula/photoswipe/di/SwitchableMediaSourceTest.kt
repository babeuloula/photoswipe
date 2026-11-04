package fr.babeuloula.photoswipe.di

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.debug.DemoMediaCatalog
import fr.babeuloula.photoswipe.debug.DeveloperSettings
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.port.MediaSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import javax.inject.Inject

/** Variante debug : MediaStore par défaut, données de démo une fois activées. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class SwitchableMediaSourceTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var mediaSource: MediaSource

    @Inject lateinit var settings: DeveloperSettings

    @Inject lateinit var clock: Clock

    @Before
    fun inject() {
        hiltRule.inject()
    }

    @Test
    fun `binds the switchable source in debug`() {
        assertTrue(mediaSource is SwitchableMediaSource)
    }

    @Test
    fun `keeps demo data off by default`() {
        assertEquals(false, settings.demoDataEnabled)
    }

    @Test
    fun `reads the demo data once enabled and the gallery once disabled`() =
        runTest {
            val demoIds = DemoMediaCatalog.generate(clock).mapTo(HashSet()) { it.id }
            settings.demoDataEnabled = true

            assertEquals(demoIds, mediaSource.mediaByIds(demoIds).mapTo(HashSet()) { it.id })

            settings.demoDataEnabled = false

            // Le faux MediaStore de Robolectric est vide : plus aucun média de démo n'est lu.
            assertEquals(
                emptyList<MediaId>(),
                runCatching {
                    mediaSource.mediaByIds(demoIds)
                }.getOrDefault(emptyList()).map { it.id },
            )
        }
}
