package fr.babeuloula.photoswipe.ui.deck

import android.content.Intent
import android.net.Uri
import fr.babeuloula.photoswipe.domain.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Feuille de partage d'un média : contenu, type et droit de lecture accordé au destinataire. */
@RunWith(RobolectricTestRunner::class)
class ShareIntentTest {
    private val photo = "content://media/external/images/media/7"

    private fun Intent.sent(): Intent = getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)!!

    @Test
    fun `opens the Android share sheet with its title`() {
        val chooser = shareIntent(photo, MediaType.IMAGE, "Share")

        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        assertEquals("Share", chooser.getCharSequenceExtra(Intent.EXTRA_TITLE))
    }

    @Test
    fun `sends the media uri as an image`() {
        val send = shareIntent(photo, MediaType.IMAGE, "Share").sent()

        assertEquals(Intent.ACTION_SEND, send.action)
        assertEquals("image/*", send.type)
        assertEquals(Uri.parse(photo), send.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java))
    }

    @Test
    fun `announces a video as a video`() {
        val send = shareIntent("content://media/external/video/media/8", MediaType.VIDEO, "Share").sent()

        assertEquals("video/*", send.type)
    }

    @Test
    fun `lets the receiving app read only this media`() {
        val send = shareIntent(photo, MediaType.IMAGE, "Share").sent()

        assertTrue(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertEquals(Uri.parse(photo), send.clipData!!.getItemAt(0).uri)
    }
}
