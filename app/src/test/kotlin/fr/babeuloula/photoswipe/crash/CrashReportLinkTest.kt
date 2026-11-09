package fr.babeuloula.photoswipe.crash

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Base64
import java.util.zip.GZIPInputStream

@RunWith(RobolectricTestRunner::class)
class CrashReportLinkTest {
    private val report = """{"exception":{"values":[{"type":"IllegalStateException","value":"é & #"}]}}"""

    @Test
    fun `puts the compressed report in the fragment of the sending page`() {
        val url = CrashReportLink.url(report)

        assertTrue(url.startsWith("https://babeuloula.github.io/photoswipe/crash/#gz="))
        val encoded = url.substringAfter("#gz=")
        assertTrue(encoded.matches(Regex("[A-Za-z0-9_-]+")))
        val decoded = GZIPInputStream(Base64.getUrlDecoder().decode(encoded).inputStream()).readBytes().decodeToString()
        assertEquals(report, decoded)
    }

    @Test
    fun `opens the sending page in the browser`() {
        val started = mutableListOf<Intent>()

        val opened = CrashReportLink.open(ApplicationProvider.getApplicationContext(), report) { started += it }

        assertTrue(opened)
        assertEquals(Intent.ACTION_VIEW, started.single().action)
        assertEquals(CrashReportLink.url(report), started.single().dataString)
    }

    @Test
    fun `says when no browser can open the page`() {
        val opened =
            CrashReportLink.open(ApplicationProvider.getApplicationContext(), report) {
                throw ActivityNotFoundException()
            }

        assertFalse(opened)
    }

    @Test
    fun `describes the installed app`() {
        val environment = ApplicationProvider.getApplicationContext<android.content.Context>().crashEnvironment()

        assertEquals("fr.babeuloula.photoswipe", environment.applicationId)
        assertTrue(environment.sdkInt > 0)
    }
}
