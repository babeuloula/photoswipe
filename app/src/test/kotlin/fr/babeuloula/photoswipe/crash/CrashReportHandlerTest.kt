package fr.babeuloula.photoswipe.crash

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
class CrashReportHandlerTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val clock = Clock.fixed(Instant.parse("2026-11-09T08:30:00Z"), ZoneOffset.UTC)
    private val environment =
        CrashEnvironment("fr.babeuloula.photoswipe", "1.2.0", 140, false, "16", 36, "Google", "Pixel 9")
    private val forwarded = mutableListOf<Throwable>()
    private val next = Thread.UncaughtExceptionHandler { _, throwable -> forwarded += throwable }

    @Test
    fun `saves the report, then lets Android close the app`() =
        runTest {
            val store = CrashReportStore(File(folder.root, "crash"), StandardTestDispatcher(testScheduler))
            val crash = IllegalStateException("boom")

            CrashReportHandler(store, CrashReportFormatter(), clock, { environment }, next)
                .uncaughtException(Thread.currentThread(), crash)

            val report = JSONObject(checkNotNull(store.pending()))
            assertEquals("2026-11-09T08:30:00Z", report.getString("timestamp"))
            assertEquals(listOf<Throwable>(crash), forwarded)
        }

    @Test
    fun `still lets Android close the app when the report fails`() =
        runTest {
            val store = CrashReportStore(File(folder.root, "crash"), StandardTestDispatcher(testScheduler))
            val crash = IllegalStateException("boom")

            CrashReportHandler(store, CrashReportFormatter(), clock, { error("no package info") }, next)
                .uncaughtException(Thread.currentThread(), crash)

            assertNull(store.pending())
            assertEquals(listOf<Throwable>(crash), forwarded)
        }
}
