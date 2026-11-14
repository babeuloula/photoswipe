package fr.babeuloula.photoswipe.data.media

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaType
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * [MediaStoreMediaSource] face au vrai MediaStore d'un appareil ou d'un émulateur.
 *
 * Le test insère ses propres photos JPEG, datées par leur EXIF, puis les supprime. Une application
 * lit sans permission les médias qu'elle a elle-même ajoutés (Android 13+) : les médias de
 * l'appareil restent invisibles et ne perturbent pas le test. Les dates choisies (années 1990)
 * évitent toute confusion avec de vraies photos. Après chaque insertion, le test attend que
 * MediaStore ait indexé ses fichiers ([MediaStoreIndexing]).
 */
@RunWith(AndroidJUnit4::class)
class MediaStoreMediaSourceInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val resolver: ContentResolver = context.contentResolver
    private val source = MediaStoreMediaSource(resolver, Dispatchers.IO)
    private val zone = ZoneId.systemDefault()
    private val inserted = mutableListOf<Uri>()
    private var lastSize = 0L

    @After
    fun deleteInsertedMedia() {
        inserted.forEach { resolver.delete(it, null, null) }
    }

    @Test
    fun readsThePhotosOfADayOfPreviousYearsFromTheRealMediaStore() =
        runTest {
            val july15th1995 = insertPhoto("ps30-a.jpg", takenAt(LocalDate.of(1995, 7, 15)))
            val july15th1998 = insertPhoto("ps30-b.jpg", takenAt(LocalDate.of(1998, 7, 15)))
            val july16th1998 = insertPhoto("ps30-c.jpg", takenAt(LocalDate.of(1998, 7, 16)))
            assertEquals(3, MediaStoreIndexing.scanOwnMedia(context))

            val ranges = SameDayMatcher().rangesFor(LocalDate.of(1999, 7, 15), zone)
            val expected = setOf(july15th1995, july15th1998)
            val ids = MediaStoreIndexing.eventually(expected) { source.mediaTakenIn(ranges).map { it.id }.toSet() }

            assertTrue(ids.containsAll(listOf(july15th1995, july15th1998)))
            assertTrue(july16th1998 !in ids)
        }

    @Test
    fun mapsAPhotoOfTheRealMediaStoreToAMediaItem() =
        runTest {
            val takenAt = takenAt(LocalDate.of(1996, 3, 2))
            val id = insertPhoto("ps30-d.jpg", takenAt)
            assertEquals(1, MediaStoreIndexing.scanOwnMedia(context))

            MediaStoreIndexing.eventually(takenAt) { source.mediaByIds(setOf(id)).singleOrNull()?.takenAt }

            val media = source.mediaByIds(setOf(id)).single()

            assertEquals(MediaType.IMAGE, media.type)
            assertEquals(takenAt, media.takenAt)
            assertEquals(lastSize, media.sizeBytes)
            assertEquals(ContentUrisOfImages.withId(id.value), media.uri)
        }

    private fun takenAt(day: LocalDate): Instant = day.atTime(12, 0).atZone(zone).toInstant()

    /**
     * Insère une petite photo JPEG prise à [takenAt] et renvoie son identifiant MediaStore.
     *
     * Comme pour une vraie photo, la date vient de l'EXIF (`DateTimeOriginal` et son décalage
     * horaire) : MediaStore recalcule `DATE_TAKEN` depuis le fichier quand il l'indexe.
     */
    private fun insertPhoto(
        name: String,
        takenAt: Instant,
    ): MediaId {
        val bytes = jpegTakenAt(takenAt)
        val values =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        val uri = checkNotNull(resolver.insert(ContentUrisOfImages.collection, values)) { "insert of $name failed" }
        inserted += uri
        resolver.openOutputStream(uri).use { checkNotNull(it).write(bytes) }
        resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        lastSize = bytes.size.toLong()
        return MediaId(uri.lastPathSegment!!.toLong())
    }

    /** JPEG d'un pixel dont l'EXIF indique une prise de vue à [takenAt], dans le fuseau du test. */
    private fun jpegTakenAt(takenAt: Instant): ByteArray {
        val file =
            File.createTempFile(
                "ps30",
                ".jpg",
                InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            )
        try {
            file.outputStream().use {
                Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.JPEG, 90, it)
            }
            val local = takenAt.atZone(zone)
            ExifInterface(file).apply {
                setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, EXIF_DATE.format(local))
                setAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL, EXIF_OFFSET.format(local))
                saveAttributes()
            }
            return file.readBytes()
        } finally {
            file.delete()
        }
    }

    private object ContentUrisOfImages {
        val collection: Uri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        fun withId(id: Long): String = "content://media/external/images/media/$id"
    }

    private companion object {
        val EXIF_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
        val EXIF_OFFSET: DateTimeFormatter = DateTimeFormatter.ofPattern("xxx")
    }
}
