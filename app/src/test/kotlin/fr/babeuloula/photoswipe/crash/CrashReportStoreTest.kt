package fr.babeuloula.photoswipe.crash

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CrashReportStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun TestScope.store(directory: File = File(folder.root, "crash")): CrashReportStore =
        CrashReportStore(directory, StandardTestDispatcher(testScheduler))

    @Test
    fun `has no report before any crash`() =
        runTest {
            assertNull(store().pending())
        }

    @Test
    fun `keeps the last report until it is cleared`() =
        runTest {
            val store = store()
            store.save("first")
            store.save("second")

            assertEquals("second", store.pending())
            assertEquals("second", store().pending())

            store.clear()

            assertNull(store.pending())
        }

    @Test
    fun `ignores a report it cannot write`() =
        runTest {
            val blocked = folder.newFile("crash")
            val store = store(blocked)

            store.save("report")

            assertNull(store.pending())
        }
}
