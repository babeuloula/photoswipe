package fr.babeuloula.photoswipe.di

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.ZoneId
import javax.inject.Inject

/** Vérifie que le module d'exemple est bien branché dans le graphe Hilt et s'injecte. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class TimeModuleTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var clock: Clock

    @Inject
    lateinit var sameClock: Clock

    @Before
    fun inject() {
        hiltRule.inject()
    }

    @Test
    fun `injects the system clock in the default time zone`() {
        assertEquals(ZoneId.systemDefault(), clock.zone)
    }

    @Test
    fun `provides a single clock for the whole application`() {
        assertSame(clock, sameClock)
    }
}
