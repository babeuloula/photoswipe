package fr.babeuloula.photoswipe.data.media

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.model.MediaId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Découpage en lots des demandes système ; la création réelle par MediaStore est vérifiée à la main. */
@RunWith(RobolectricTestRunner::class)
class MediaStoreActionRequestFactoryTest {
    private val intentSender: IntentSender =
        PendingIntent
            .getActivity(RuntimeEnvironment.getApplication(), 0, Intent(), PendingIntent.FLAG_IMMUTABLE)
            .intentSender
    private val trashBatches = mutableListOf<List<Uri>>()
    private val favoriteBatches = mutableListOf<List<Uri>>()

    /** Valeur demandée pour chaque lot de favoris : vrai pour un ajout, faux pour un retrait. */
    private val favoriteValues = mutableListOf<Boolean>()

    private fun factory(batchSize: Int = MediaStoreActionRequestFactory.BATCH_SIZE) =
        MediaStoreActionRequestFactory(
            createTrash = {
                trashBatches += it
                intentSender
            },
            createFavorite = { uris, favorite ->
                favoriteBatches += uris
                favoriteValues += favorite
                intentSender
            },
            batchSize = batchSize,
        )

    private val media = (1L..1_200L).map { aMediaItem(id = it) }

    @Test
    fun `asks for the trash in batches of 500 media`() {
        val requests = factory().trashRequests(media)

        assertEquals(listOf(500, 500, 200), requests.map { it.mediaIds.size })
        assertEquals(listOf(500, 500, 200), trashBatches.map { it.size })
        assertTrue(favoriteBatches.isEmpty())
    }

    @Test
    fun `keeps the order of the media and their uris in each batch`() {
        val requests = factory(batchSize = 2).trashRequests(media.take(3))

        assertEquals(listOf(setOf(MediaId(1), MediaId(2)), setOf(MediaId(3))), requests.map { it.mediaIds })
        assertEquals(media.take(3).map { Uri.parse(it.uri) }, trashBatches.flatten())
    }

    @Test
    fun `carries the system dialog of each batch`() {
        val request = factory().trashRequests(media.take(1)).single()

        assertSame(intentSender, (request as IntentSenderMediaActionRequest).intentSender)
    }

    @Test
    fun `asks for favorites in batches too`() {
        val requests = factory().favoriteRequests(media)

        assertEquals(listOf(500, 500, 200), requests.map { it.mediaIds.size })
        assertTrue(trashBatches.isEmpty())
    }

    @Test
    fun `adds to favorites, or removes from them`() {
        factory().favoriteRequests(media.take(1))
        val removal = factory().unfavoriteRequests(media.take(2))

        assertEquals(listOf(true, false), favoriteValues)
        assertEquals(setOf(MediaId(1), MediaId(2)), removal.single().mediaIds)
    }

    @Test
    fun `asks nothing for no media`() {
        assertTrue(factory().trashRequests(emptyList()).isEmpty())
        assertTrue(trashBatches.isEmpty())
    }
}
