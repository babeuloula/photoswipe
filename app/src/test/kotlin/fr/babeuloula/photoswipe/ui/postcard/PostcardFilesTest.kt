package fr.babeuloula.photoswipe.ui.postcard

import android.content.ContentResolver
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

/** Carte postale écrite dans le cache et partagée par le FileProvider (PS-177). */
@RunWith(RobolectricTestRunner::class)
class PostcardFilesTest {
    private val context = RuntimeEnvironment.getApplication()
    private val files = PostcardFiles(context)

    private fun aPostcard(): Bitmap = Bitmap.createBitmap(40, 30, Bitmap.Config.ARGB_8888)

    @Test
    fun `writes the postcard in the cache and shares it through the file provider`() {
        // Seul test qui passe par le FileProvider : il garde ses racines d'un test à l'autre, alors
        // que Robolectric change de dossier de cache à chaque test.
        val written = files.write(aPostcard())
        val uri = files.shareableUri(written)

        assertEquals(ContentResolver.SCHEME_CONTENT, uri.scheme)
        assertEquals("${context.packageName}.postcards", uri.authority)
        assertEquals(
            listOf(written),
            files.directory
                .listFiles()
                .orEmpty()
                .toList(),
        )
        assertTrue(written.length() > 0)
        assertTrue(written.canonicalPath.startsWith(context.cacheDir.canonicalPath))
    }

    @Test
    fun `removes the previous postcards when writing a new one`() {
        files.directory.mkdirs()
        val old = File(files.directory, "PhotoSwipe-1.jpg").apply { writeText("old") }

        files.write(aPostcard())

        assertFalse(old.exists())
        assertEquals(
            1,
            files.directory
                .listFiles()
                .orEmpty()
                .size,
        )
    }

    @Test
    fun `lets only the chosen app read the postcard`() {
        val uri = Uri.parse("content://${context.packageName}.postcards/postcards/PhotoSwipe-1.jpg")

        val chooser = postcardShareIntent(uri, "Share")

        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        val send = chooser.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)!!
        assertEquals(Intent.ACTION_SEND, send.action)
        assertEquals("image/jpeg", send.type)
        assertEquals(uri, send.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java))
        assertEquals(uri, send.clipData!!.getItemAt(0).uri)
        assertTrue(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertEquals(0, send.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }
}
