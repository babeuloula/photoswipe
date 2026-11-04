package fr.babeuloula.photoswipe

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Vérifie que `MainActivity` démarre comme point d'entrée Hilt. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class MainActivityTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Test
    fun `main activity starts as a hilt entry point`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()

        assertNotNull(activity)
    }
}
