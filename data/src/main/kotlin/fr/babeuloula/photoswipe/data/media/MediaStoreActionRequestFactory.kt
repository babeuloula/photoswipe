package fr.babeuloula.photoswipe.data.media

import android.content.ContentResolver
import android.content.IntentSender
import android.net.Uri
import android.provider.MediaStore
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.port.MediaActionRequest
import fr.babeuloula.photoswipe.domain.port.MediaActionRequestFactory
import javax.inject.Inject

/**
 * Demande système de MediaStore : l'`IntentSender` du dialogue à afficher et les médias qu'il couvre.
 *
 * @property intentSender dialogue système de corbeille ou de favoris.
 */
class IntentSenderMediaActionRequest(
    override val mediaIds: Set<MediaId>,
    val intentSender: IntentSender,
) : MediaActionRequest

/**
 * [MediaActionRequestFactory] sur `MediaStore.createTrashRequest` et `createFavoriteRequest`
 * (SPEC §3.5). Les médias sont découpés en lots de [batchSize] au plus, dans l'ordre reçu, pour
 * rester sous la taille maximale d'un Intent : chaque lot donne un dialogue système.
 *
 * @param createTrash crée la demande d'envoi à la corbeille (restaurable) des URI d'un lot.
 * @param createFavorite crée la demande d'ajout aux favoris des URI d'un lot.
 */
class MediaStoreActionRequestFactory(
    private val createTrash: (List<Uri>) -> IntentSender,
    private val createFavorite: (List<Uri>) -> IntentSender,
    private val batchSize: Int = BATCH_SIZE,
) : MediaActionRequestFactory {
    @Inject
    constructor(contentResolver: ContentResolver) : this(
        createTrash = { uris -> MediaStore.createTrashRequest(contentResolver, uris, true).intentSender },
        createFavorite = { uris -> MediaStore.createFavoriteRequest(contentResolver, uris, true).intentSender },
    )

    init {
        require(batchSize > 0) { "batchSize must be positive: $batchSize" }
    }

    override fun trashRequests(media: List<MediaItem>): List<MediaActionRequest> = requests(media, createTrash)

    override fun favoriteRequests(media: List<MediaItem>): List<MediaActionRequest> = requests(media, createFavorite)

    private fun requests(
        media: List<MediaItem>,
        create: (List<Uri>) -> IntentSender,
    ): List<MediaActionRequest> =
        media.chunked(batchSize) { batch ->
            IntentSenderMediaActionRequest(
                mediaIds = batch.mapTo(LinkedHashSet()) { it.id },
                intentSender = create(batch.map { Uri.parse(it.uri) }),
            )
        }

    companion object {
        /** Médias par demande, bien en dessous de la taille maximale d'un Intent (SPEC §3.5). */
        const val BATCH_SIZE: Int = 500
    }
}
