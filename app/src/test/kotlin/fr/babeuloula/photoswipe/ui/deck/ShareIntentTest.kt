package fr.babeuloula.photoswipe.ui.deck

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import fr.babeuloula.photoswipe.domain.model.MediaType
import fr.babeuloula.photoswipe.share.ShareTargetReceiver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Feuille de partage d'un média : contenu, type et droit de lecture accordé au destinataire. */
@RunWith(RobolectricTestRunner::class)
class ShareIntentTest {
    private val photo = "content://media/external/images/media/7"

    private fun Intent.sent(): Intent = getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)!!

    @Test
    fun `opens a chooser of viewers and editors that may read and write only this media`() {
        val chooser = openWithIntent(photo, MediaType.IMAGE, "Open with")
        val view = chooser.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)!!
        val edit = chooser.getParcelableArrayExtra(Intent.EXTRA_ALTERNATE_INTENTS, Intent::class.java)!!.single()

        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        assertEquals("Open with", chooser.getCharSequenceExtra(Intent.EXTRA_TITLE))
        assertEquals(Intent.ACTION_VIEW, view.action)
        assertEquals(Intent.ACTION_EDIT, edit.action)
        listOf(view, edit).forEach { intent ->
            assertEquals(Uri.parse(photo), intent.data)
            assertEquals("image/*", intent.type)
            assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
            assertTrue(intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION != 0)
        }
    }

    @Test
    fun `opens a video as a video`() {
        val view =
            openWithIntent("content://media/external/video/media/8", MediaType.VIDEO, "Open with")
                .getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)!!

        assertEquals("video/*", view.type)
    }

    @Test
    fun `sends the media straight to the app chosen last time, without a share sheet`() {
        val send = shareWithIntent(photo, MediaType.IMAGE, "com.whatsapp/.ContactPicker")

        assertEquals(Intent.ACTION_SEND, send.action)
        assertEquals(ComponentName("com.whatsapp", "com.whatsapp.ContactPicker"), send.component)
        assertEquals(Uri.parse(photo), send.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java))
        assertTrue(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }

    @Test
    fun `asks Android to report the app chosen in the share sheet`() {
        val callback = ShareTargetReceiver.callback(RuntimeEnvironment.getApplication())

        val chooser = shareIntent(photo, MediaType.IMAGE, "Share", callback)

        assertEquals(
            callback,
            chooser.getParcelableExtra(Intent.EXTRA_CHOSEN_COMPONENT_INTENT_SENDER, IntentSender::class.java),
        )
    }

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

    @Test
    fun `starts the share sheet when Android accepts it`() {
        val started = mutableListOf<Intent>()
        val intent = shareIntent(photo, MediaType.IMAGE, "Share")

        assertTrue(launchShare(intent) { started += it })
        assertEquals(listOf(intent), started)
    }

    @Test
    fun `reports a media that Android refuses to hand over`() {
        val refused = launchShare(shareIntent(photo, MediaType.IMAGE, "Share")) { throw SecurityException("no access") }

        assertFalse(refused)
    }

    @Test
    fun `reports a missing share sheet`() {
        val missing = launchShare(shareIntent(photo, MediaType.IMAGE, "Share")) { throw ActivityNotFoundException() }

        assertFalse(missing)
    }
}
