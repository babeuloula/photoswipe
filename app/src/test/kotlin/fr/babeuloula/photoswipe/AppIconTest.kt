package fr.babeuloula.photoswipe

import android.graphics.drawable.AdaptiveIconDrawable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Icône de l'application (ADR 0025) : adaptative, avec sa couche monochrome pour l'icône à thème. */
@RunWith(RobolectricTestRunner::class)
class AppIconTest {
    private val application = RuntimeEnvironment.getApplication()

    @Test
    fun `declares the stack of prints as the app icon`() {
        assertEquals(R.mipmap.ic_launcher, application.applicationInfo.icon)
    }

    @Test
    fun `offers a monochrome layer for the themed icon`() {
        val icon = application.getDrawable(R.mipmap.ic_launcher) as AdaptiveIconDrawable

        assertNotNull(icon.foreground)
        assertNotNull(icon.monochrome)
    }
}
