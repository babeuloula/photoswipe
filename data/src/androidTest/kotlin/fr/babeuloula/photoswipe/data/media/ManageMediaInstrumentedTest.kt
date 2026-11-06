package fr.babeuloula.photoswipe.data.media

import android.app.Activity
import android.content.ContentUris
import android.content.IntentSender
import android.os.Bundle
import android.provider.MediaStore
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.babeuloula.photoswipe.domain.model.MediaId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accès « Gestion des médias » (`MANAGE_MEDIA`, PS-84) face au vrai MediaStore : avec lui, les
 * demandes de corbeille et de favori s'appliquent sans dialogue d'Android ; sans lui, Android
 * attend la réponse de l'utilisateur.
 *
 * La photo testée appartient au shell (une capture d'écran), pas à l'application : c'est le cas
 * d'une vraie photo de la galerie. L'accès est donné ou retiré par `appops`, comme le ferait
 * l'utilisateur dans les réglages.
 */
@RunWith(AndroidJUnit4::class)
class ManageMediaInstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val resolver = context.contentResolver
    private val images = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    private var photoId = 0L

    /** Nom propre à chaque test : un scan en retard du test précédent ne peut pas le confondre. */
    private val photoName = "ps84-manage-media-${System.nanoTime()}.png"
    private val photoPath = "/sdcard/Pictures/$photoName"
    private val scenarios = mutableListOf<ActivityScenario<Activity>>()

    @Before
    fun pushPhotoOwnedByTheShell() {
        shell("pm grant ${context.packageName} android.permission.READ_MEDIA_IMAGES")
        shell("screencap -p $photoPath")
        // Le scan est relancé pendant l'attente : sur un émulateur chargé, le premier peut passer avant l'écriture.
        photoId =
            checkNotNull(
                waitFor {
                    photoIdOrNull()
                        ?: null.also {
                            shell(
                                "content call --uri content://media --method scan_volume --arg external_primary",
                            )
                        }
                },
            ) { "$photoName never reached MediaStore" }
    }

    @After
    fun removePhoto() {
        scenarios.forEach { it.close() }
        shell("appops set ${context.packageName} MANAGE_MEDIA default")
        shell("input keyevent KEYCODE_BACK")
        shell("rm -f $photoPath /sdcard/Pictures/.trashed-*$photoName")
        shell("content call --uri content://media --method scan_volume --arg external_primary")
    }

    @Test
    fun `trashes a gallery photo without any dialog once media management is allowed`() {
        allowMediaManagement(true)

        launch(MediaStore.createTrashRequest(resolver, listOf(photoUri()), true).intentSender)

        assertTrue(waitFor { column(MediaStore.MediaColumns.IS_TRASHED).takeIf { it == 1 } } == 1)
    }

    @Test
    fun `adds a gallery photo to the favorites without any dialog once media management is allowed`() {
        allowMediaManagement(true)

        launch(MediaStore.createFavoriteRequest(resolver, listOf(photoUri()), true).intentSender)

        assertTrue(waitFor { column(MediaStore.MediaColumns.IS_FAVORITE).takeIf { it == 1 } } == 1)
        // La galerie lue par l'application voit le favori (cœur de la carte, PS-86).
        val media = runBlocking { MediaStoreMediaSource(resolver, Dispatchers.IO).mediaByIds(setOf(MediaId(photoId))) }
        assertTrue(media.single().isFavorite)
    }

    @Test
    fun `waits for the user before trashing without media management`() {
        allowMediaManagement(false)

        launch(MediaStore.createTrashRequest(resolver, listOf(photoUri()), true).intentSender)
        Thread.sleep(DIALOG_WAIT_MILLIS)

        assertEquals(0, column(MediaStore.MediaColumns.IS_TRASHED))
        assertFalse(MediaStore.canManageMedia(context))
    }

    private fun allowMediaManagement(allowed: Boolean) {
        shell("appops set ${context.packageName} MANAGE_MEDIA ${if (allowed) "allow" else "deny"}")
        assertEquals(allowed, MediaStore.canManageMedia(context))
    }

    /**
     * Lance la demande depuis une activité au premier plan, comme l'application : Android 14+
     * refuse qu'un processus en arrière-plan ouvre l'activité de MediaStore.
     */
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

    /** Valeur entière de [name] pour la photo, corbeille comprise. */
    private fun column(name: String): Int {
        val arguments =
            Bundle().apply {
                putString(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION, "${MediaStore.MediaColumns._ID} = ?")
                putStringArray(
                    android.content.ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                    arrayOf(photoId.toString()),
                )
                putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE)
            }
        return resolver.query(images, arrayOf(name), arguments, null)!!.use {
            it.moveToFirst()
            it.getInt(0)
        }
    }

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
        const val DIALOG_WAIT_MILLIS = 3_000L
        const val SHELL_SETTLE_MILLIS = 300L
    }
}
