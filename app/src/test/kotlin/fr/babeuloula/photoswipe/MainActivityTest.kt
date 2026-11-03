package fr.babeuloula.photoswipe

import android.app.Activity
import org.junit.Assert.assertTrue
import org.junit.Test

/** Test de fumée du module application. */
class MainActivityTest {
    @Test
    fun `main activity is an android activity`() {
        assertTrue(Activity::class.java.isAssignableFrom(MainActivity::class.java))
    }
}
