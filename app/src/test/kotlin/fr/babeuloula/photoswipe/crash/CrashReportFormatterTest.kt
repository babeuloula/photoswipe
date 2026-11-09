package fr.babeuloula.photoswipe.crash

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.time.Instant

/** `org.json` vient d'Android : le test passe par Robolectric. */
@RunWith(RobolectricTestRunner::class)
class CrashReportFormatterTest {
    private val environment =
        CrashEnvironment(
            applicationId = "fr.babeuloula.photoswipe",
            versionName = "1.2.0",
            versionCode = 140,
            debuggable = false,
            androidVersion = "16",
            sdkInt = 36,
            manufacturer = "Google",
            model = "Pixel 9",
        )

    private val formatter = CrashReportFormatter(newEventId = { "0123456789abcdef0123456789abcdef" })

    private fun format(
        throwable: Throwable,
        environment: CrashEnvironment = this.environment,
    ): JSONObject = JSONObject(formatter.format(throwable, "main", Instant.parse("2026-11-09T08:30:00Z"), environment))

    private fun JSONObject.exceptions(): JSONArray = getJSONObject("exception").getJSONArray("values")

    private fun JSONObject.frames(): JSONArray = getJSONObject("stacktrace").getJSONArray("frames")

    private fun stackOf(vararg frames: Pair<String, String>): Array<StackTraceElement> =
        frames
            .mapIndexed {
                index,
                (type, method),
                ->
                StackTraceElement(type, method, "File.kt", index + 1)
            }.toTypedArray()

    @Test
    fun `describes the app, the phone and the time of a fatal crash`() {
        val event = format(IllegalStateException("boom"))

        assertEquals("0123456789abcdef0123456789abcdef", event.getString("event_id"))
        assertEquals("2026-11-09T08:30:00Z", event.getString("timestamp"))
        assertEquals("fatal", event.getString("level"))
        assertEquals("java", event.getString("platform"))
        assertEquals("fr.babeuloula.photoswipe@1.2.0+140", event.getString("release"))
        assertEquals("release", event.getString("environment"))
        assertEquals("main", event.getJSONObject("tags").getString("thread"))
        val contexts = event.getJSONObject("contexts")
        assertEquals("16", contexts.getJSONObject("os").getString("version"))
        assertEquals("Pixel 9", contexts.getJSONObject("device").getString("model"))
        assertEquals("1.2.0", contexts.getJSONObject("app").getString("app_version"))
        assertEquals("140", contexts.getJSONObject("app").getString("app_build"))
    }

    @Test
    fun `sends debug builds to the debug environment`() {
        val event = format(IllegalStateException("boom"), environment.copy(debuggable = true))

        assertEquals("debug", event.getString("environment"))
    }

    @Test
    fun `lists the oldest cause first and the thrown exception last`() {
        val thrown = IllegalStateException("could not save", IOException("disk full"))

        val values = format(thrown).exceptions()

        assertEquals(2, values.length())
        assertEquals("IOException", values.getJSONObject(0).getString("type"))
        assertEquals("java.io", values.getJSONObject(0).getString("module"))
        assertEquals("disk full", values.getJSONObject(0).getString("value"))
        assertEquals("IllegalStateException", values.getJSONObject(1).getString("type"))
    }

    @Test
    fun `orders frames from the oldest call and marks the app ones`() {
        val thrown = IllegalStateException("boom")
        thrown.stackTrace =
            stackOf("fr.babeuloula.photoswipe.ui.deck.DeckViewModel" to "keep", "android.os.Looper" to "loop")

        val frames = format(thrown).exceptions().getJSONObject(0).frames()

        assertEquals("android.os.Looper", frames.getJSONObject(0).getString("module"))
        assertFalse(frames.getJSONObject(0).getBoolean("in_app"))
        val newest = frames.getJSONObject(1)
        assertEquals("keep", newest.getString("function"))
        assertEquals("File.kt", newest.getString("filename"))
        assertEquals(1, newest.getInt("lineno"))
        assertTrue(newest.getBoolean("in_app"))
    }

    @Test
    fun `keeps the most recent frames of a very deep stack`() {
        val thrown = IllegalStateException("boom")
        thrown.stackTrace = stackOf(*Array(200) { "fr.babeuloula.photoswipe.Deep" to "call$it" })

        val frames = format(thrown).exceptions().getJSONObject(0).frames()

        assertEquals(60, frames.length())
        assertEquals("call0", frames.getJSONObject(59).getString("function"))
    }

    @Test
    fun `cuts long messages and long cause chains, even a looping one`() {
        val first = RuntimeException("x".repeat(5_000))
        var current: Throwable = first
        repeat(10) { current = RuntimeException("level $it", current) }
        val looping = RuntimeException("loop")
        val other = RuntimeException("other", looping)
        looping.initCause(other)

        val values = format(current).exceptions()

        assertEquals(5, values.length())
        assertEquals(2, format(looping).exceptions().length())
        assertEquals(
            1_000,
            format(first)
                .exceptions()
                .getJSONObject(0)
                .getString("value")
                .length,
        )
    }

    @Test
    fun `omits the value of an exception without message`() {
        val value = format(IllegalStateException()).exceptions().getJSONObject(0)

        assertFalse(value.has("value"))
    }
}
