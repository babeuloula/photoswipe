package fr.babeuloula.photoswipe.data.media

import android.app.Activity
import android.content.ContentResolver
import android.content.ContentUris
import android.content.IntentSender
import android.os.Bundle
import android.provider.MediaStore
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.time.Instant

/**
 * Page « Corbeille » (PS-168) face au vrai MediaStore, sur une photo du shell envoyée à la corbeille :
 *
 * - ce qu'on lit d'un média en corbeille avec `QUERY_ARG_MATCH_TRASHED` : identifiant, taille et
 *   date d'expiration (`DATE_EXPIRES`, environ 30 jours plus tard), par [MediaStoreSystemTrash] ;
 * - la restauration par `createTrashRequest(…, false)` et la suppression définitive par
 *   `createDeleteRequest`, appliquées sans dialogue avec l'accès « Gestion des médias » (ADR 0023).
 *
 * La vignette d'un média en corbeille n'est pas vérifiée ici : l'écran se replie sur un aplat si
 * elle ne se charge pas.
 */
@RunWith(AndroidJUnit4::class)
class TrashInstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val resolver = context.contentResolver
    private val images = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    private val photoName = "ps168-trash-${System.nanoTime()}.png"
    private val photoPath = "/sdcard/Pictures/$photoName"
    private val scenarios = mutableListOf<ActivityScenario<Activity>>()
    private val systemTrash = MediaStoreSystemTrash(resolver, Dispatchers.IO)
    private var photoId = 0L

    @Before
    fun trashPhotoOwnedByTheShell() {
        shell("pm grant ${context.packageName} android.permission.READ_MEDIA_IMAGES")
        shell("appops set ${context.packageName} MANAGE_MEDIA allow")
        shell("screencap -p $photoPath")
        photoId =
            checkNotNull(
                waitFor {
                    photoIdOrNull()
                        ?: null.also {
                            shell("content call --uri content://media --method scan_volume --arg external_primary")
                        }
                },
            ) { "$photoName never reached MediaStore" }
        launch(MediaStore.createTrashRequest(resolver, listOf(photoUri()), true).intentSender)
        checkNotNull(waitFor { trashedFlag().takeIf { it == 1 } }) { "$photoName never reached the trash" }
    }

    @After
    fun removePhoto() {
        scenarios.forEach { it.close() }
        shell("appops set ${context.packageName} MANAGE_MEDIA default")
        shell("rm -f $photoPath /sdcard/Pictures/.trashed-*$photoName")
        shell("content call --uri content://media --method scan_volume --arg external_primary")
    }

    @Test
    fun readsTheSizeAndThePurgeDateOfATrashedPhotoThatTheGalleryNoLongerSees() {
        val trashed = runBlocking { systemTrash.trashed(setOf(MediaId(photoId))) }.single()

        assertEquals(MediaId(photoId), trashed.media.id)
        assertTrue(trashed.media.sizeBytes > 0)
        val purgeAt = assertNotNullAndReturn(trashed.purgeAt)
        val inDays = Duration.between(Instant.now(), purgeAt).toDays()
        assertTrue("purge in $inDays days", inDays in PURGE_DAYS_MIN..PURGE_DAYS_MAX)
        val gallery =
            runBlocking { MediaStoreMediaSource(resolver, Dispatchers.IO).mediaByIds(setOf(MediaId(photoId))) }
        assertTrue(gallery.isEmpty())
    }

    @Test
    fun restoresATrashedPhotoByItsIdentifierWithoutAnyDialogWithMediaManagement() {
        val request = factory().restoreRequests(listOf(trashedPhoto())).single()

        launch((request as IntentSenderMediaActionRequest).intentSender)

        assertEquals(0, waitFor { trashedFlag().takeIf { it == 0 } })
        assertTrue(runBlocking { systemTrash.trashed(setOf(MediaId(photoId))) }.isEmpty())
        val gallery =
            runBlocking { MediaStoreMediaSource(resolver, Dispatchers.IO).mediaByIds(setOf(MediaId(photoId))) }
        assertEquals(listOf(MediaId(photoId)), gallery.map { it.id })
    }

    @Test
    fun deletesATrashedPhotoForGoodWithoutAnyDialogWithMediaManagement() {
        val request = factory().deleteRequests(listOf(trashedPhoto())).single()

        launch((request as IntentSenderMediaActionRequest).intentSender)

        assertEquals(true, waitFor { photoExists().takeIf { !it }?.let { true } })
        assertTrue(runBlocking { systemTrash.trashed(setOf(MediaId(photoId))) }.isEmpty())
    }

    @Test
    fun leavesOutWithoutErrorAPhotoThatIsNoLongerInTheTrash() {
        assertNull(runBlocking { systemTrash.trashed(setOf(MediaId(Long.MAX_VALUE))) }.firstOrNull())
    }

    private fun factory() = MediaStoreActionRequestFactory(resolver)

    private fun trashedPhoto() = MediaItem(MediaId(photoId), photoUri().toString(), MediaType.IMAGE, Instant.now(), 0)

    private fun <T> assertNotNullAndReturn(value: T?): T {
        assertNotNull(value)
        return value!!
    }

    /** Lance la demande depuis une activité au premier plan, comme l'application. */
    private fun launch(sender: IntentSender) {
        val scenario = ActivityScenario.launch(Activity::class.java)
        scenarios += scenario
        scenario.onActivity { it.startIntentSenderForResult(sender, 0, null, 0, 0, 0) }
    }

    private fun photoUri() = ContentUris.withAppendedId(images, photoId)

    private fun photoIdOrNull(): Long? =
        resolver
            .query(
                images,
                arrayOf(MediaStore.MediaColumns._ID),
                "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
                arrayOf(photoName),
                null,
            )?.use { if (it.moveToFirst()) it.getLong(0) else null }

    private fun includingTrash(): Bundle =
        Bundle().apply {
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, "${MediaStore.MediaColumns._ID} = ?")
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, arrayOf(photoId.toString()))
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE)
        }

    /** `IS_TRASHED` de la photo, `null` si elle n'existe plus. */
    private fun trashedFlag(): Int? =
        resolver.query(images, arrayOf(MediaStore.MediaColumns.IS_TRASHED), includingTrash(), null)?.use {
            if (it.moveToFirst()) it.getInt(0) else null
        }

    private fun photoExists(): Boolean =
        resolver.query(images, arrayOf(MediaStore.MediaColumns._ID), includingTrash(), null)?.use { it.moveToFirst() }
            ?: false

    private fun <T> waitFor(read: () -> T?): T? {
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
        const val PURGE_DAYS_MIN = 25L
        const val PURGE_DAYS_MAX = 31L
    }
}
