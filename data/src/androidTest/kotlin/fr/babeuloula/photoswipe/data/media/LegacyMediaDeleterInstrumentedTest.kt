package fr.babeuloula.photoswipe.data.media

import android.content.ContentUris
import android.media.MediaScannerConnection
import android.os.Build
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.babeuloula.photoswipe.domain.model.MediaId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Suppression directe d'Android 10 (PS-184, ADR 0042) face au vrai MediaStore : une photo du shell,
 * donc d'une autre application, supprimée par [LegacyMediaDeleter] sans aucun dialogue, grâce au
 * stockage hérité et à `WRITE_EXTERNAL_STORAGE`. Seulement sur Android 10 (PS-186).
 */
@RunWith(AndroidJUnit4::class)
class LegacyMediaDeleterInstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val resolver = context.contentResolver
    private val images = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    private val photoName = "ps184-delete-${System.nanoTime()}.png"
    private val photoPath = "/sdcard/Pictures/$photoName"
    private var photoId = 0L

    @Before
    fun pushPhotoOwnedByTheShell() {
        assumeTrue(Build.VERSION.SDK_INT == Build.VERSION_CODES.Q)
        shell("pm grant ${context.packageName} android.permission.READ_EXTERNAL_STORAGE")
        shell("pm grant ${context.packageName} android.permission.WRITE_EXTERNAL_STORAGE")
        shell("screencap -p $photoPath")
        val scanned = CountDownLatch(1)
        MediaScannerConnection.scanFile(context, arrayOf(photoPath), null) { _, _ -> scanned.countDown() }
        scanned.await(SCAN_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        photoId = checkNotNull(waitFor { photoIdOrNull() }) { "$photoName never reached MediaStore" }
    }

    @After
    fun removePhoto() {
        shell("rm -f $photoPath")
    }

    @Test
    fun deletesAPhotoOfAnotherAppForGoodWithoutAnyDialogOnAndroid10() {
        val uri = ContentUris.withAppendedId(images, photoId)
        val deleter = LegacyMediaDeleter(resolver, Dispatchers.IO)

        val deleted = runBlocking { deleter.delete(DirectDeletionRequest(setOf(MediaId(photoId)), listOf(uri))) }

        assertEquals(setOf(MediaId(photoId)), deleted?.mediaIds)
        assertNull(photoIdOrNull())
    }

    private fun photoIdOrNull(): Long? =
        resolver
            .query(
                images,
                arrayOf(MediaStore.MediaColumns._ID),
                "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
                arrayOf(photoName),
                null,
            )?.use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else null }

    private fun <T : Any> waitFor(read: () -> T?): T? {
        repeat(POLL_ATTEMPTS) {
            read()?.let { return it }
            Thread.sleep(POLL_MILLIS)
        }
        return null
    }

    private fun shell(command: String) {
        instrumentation.uiAutomation.executeShellCommand(command).close()
        Thread.sleep(SHELL_SETTLE_MILLIS)
    }

    private companion object {
        const val POLL_ATTEMPTS = 50
        const val POLL_MILLIS = 200L
        const val SHELL_SETTLE_MILLIS = 300L
        const val SCAN_TIMEOUT_SECONDS = 30L
    }
}
