package fr.babeuloula.photoswipe.share

import android.content.ComponentName
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Application choisie dans la feuille de partage, retenue pour « Renvoyer avec … » (PS-163). */
@RunWith(RobolectricTestRunner::class)
class ShareTargetReceiverTest {
    @Test
    fun `reads the app chosen in the share sheet`() {
        val chosen = ComponentName("com.whatsapp", "com.whatsapp.ContactPicker")

        assertEquals(
            "com.whatsapp/com.whatsapp.ContactPicker",
            ShareTargetReceiver.chosenTarget(Intent().putExtra(Intent.EXTRA_CHOSEN_COMPONENT, chosen)),
        )
    }

    @Test
    fun `reads nothing when Android does not say which app was chosen`() {
        assertNull(ShareTargetReceiver.chosenTarget(Intent()))
    }
}
