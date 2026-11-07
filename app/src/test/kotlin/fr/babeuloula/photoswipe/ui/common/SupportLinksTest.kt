package fr.babeuloula.photoswipe.ui.common

import android.content.ActivityNotFoundException
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SupportLinksTest {
    private val context = RuntimeEnvironment.getApplication()
    private val started = mutableListOf<String>()

    private fun startingOnly(vararg schemes: String): (Intent) -> Unit =
        { intent ->
            if (intent.data?.scheme !in schemes) throw ActivityNotFoundException()
            started += intent.dataString.orEmpty()
        }

    @Test
    fun `opens the app page in the Play Store app`() {
        assertTrue(SupportLinks.openRating(context, startingOnly("market", "https")))

        assertEquals(listOf("market://details?id=fr.babeuloula.photoswipe"), started)
        assertEquals(Intent.ACTION_VIEW, SupportLinks.ratingIntent("x").action)
    }

    @Test
    fun `falls back on the Play Store website without the Play Store app`() {
        assertTrue(SupportLinks.openRating(context, startingOnly("https")))

        assertEquals(listOf("https://play.google.com/store/apps/details?id=fr.babeuloula.photoswipe"), started)
    }

    @Test
    fun `opens the PayPal donation page`() {
        assertTrue(SupportLinks.openDonation(context, startingOnly("https")))

        assertEquals(listOf("https://www.paypal.com/paypalme/jeremyreynaud"), started)
    }

    @Test
    fun `reports when no app can open the link`() {
        assertFalse(SupportLinks.openRating(context, startingOnly()))
        assertFalse(SupportLinks.openDonation(context, startingOnly()))
        assertTrue(started.isEmpty())
    }
}
