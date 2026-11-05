package fr.babeuloula.photoswipe.di

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.data.di.IoDispatcher
import fr.babeuloula.photoswipe.data.media.MediaStoreActionRequestFactory
import fr.babeuloula.photoswipe.data.review.RoomReviewRepository
import fr.babeuloula.photoswipe.data.settings.DataStoreSettingsRepository
import fr.babeuloula.photoswipe.domain.port.MediaActionRequestFactory
import fr.babeuloula.photoswipe.domain.port.ReviewRepository
import fr.babeuloula.photoswipe.domain.port.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Inject

/**
 * Vérifie que les modules Hilt de `:data` lient chaque port à son implémentation, une seule fois.
 * `MediaSource`, lié par variante, est vérifié dans `testDebug` et `testRelease`.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class DataModulesTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var reviewRepository: ReviewRepository

    @Inject lateinit var sameReviewRepository: ReviewRepository

    @Inject lateinit var settingsRepository: SettingsRepository

    @Inject lateinit var mediaActionRequestFactory: MediaActionRequestFactory

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    @Before
    fun inject() {
        hiltRule.inject()
    }

    @Test
    fun `binds each domain port to its android implementation`() {
        assertTrue(reviewRepository is RoomReviewRepository)
        assertTrue(settingsRepository is DataStoreSettingsRepository)
        assertTrue(mediaActionRequestFactory is MediaStoreActionRequestFactory)
    }

    @Test
    fun `provides a single review repository for the whole application`() {
        assertSame(reviewRepository, sameReviewRepository)
    }

    @Test
    fun `injects the io dispatcher`() {
        assertSame(Dispatchers.IO, ioDispatcher)
    }
}
