package fr.babeuloula.photoswipe.data.media

import android.content.ContentResolver
import android.content.IntentSender
import android.net.Uri
import android.os.Build
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
 * [MediaActionRequestFactory] sur `MediaStore.createTrashRequest`, `createFavoriteRequest` et
 * `createDeleteRequest` (SPEC §3.5, PS-168). Les médias sont découpés en lots de [batchSize] au plus,
 * dans l'ordre reçu, pour rester sous la taille maximale d'un Intent : chaque lot donne un dialogue
 * système. Avec l'accès « Gestion des médias », Android applique chaque demande sans dialogue
 * (ADR 0023, vérifié par `ManageMediaInstrumentedTest` et `TrashInstrumentedTest`).
 *
 * @param createTrash crée la demande d'envoi à la corbeille (vrai) ou de sortie de la corbeille
 * (faux) des URI d'un lot.
 * @param createFavorite crée la demande d'ajout aux favoris (vrai) ou de retrait (faux) des URI d'un lot.
 * @param createDelete crée la demande de suppression définitive des URI d'un lot.
 */
class MediaStoreActionRequestFactory(
    private val createTrash: (List<Uri>, Boolean) -> IntentSender,
    private val createFavorite: (List<Uri>, Boolean) -> IntentSender,
    private val createDelete: (List<Uri>) -> IntentSender,
    private val batchSize: Int = BATCH_SIZE,
) : MediaActionRequestFactory {
    @Inject
    constructor(contentResolver: ContentResolver) : this(
        createTrash = { uris, trashed ->
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) throw unsupportedBeforeAndroid11()
            MediaStore.createTrashRequest(contentResolver, uris, trashed).intentSender
        },
        createFavorite = { uris, favorite ->
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) throw unsupportedBeforeAndroid11()
            MediaStore.createFavoriteRequest(contentResolver, uris, favorite).intentSender
        },
        createDelete = { uris ->
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) throw unsupportedBeforeAndroid11()
            MediaStore.createDeleteRequest(contentResolver, uris).intentSender
        },
    )

    init {
        require(batchSize > 0) { "batchSize must be positive: $batchSize" }
    }

    override fun trashRequests(media: List<MediaItem>): List<MediaActionRequest> =
        requests(media) { uris -> createTrash(uris, true) }

    override fun favoriteRequests(media: List<MediaItem>): List<MediaActionRequest> =
        requests(media) { uris -> createFavorite(uris, true) }

    override fun unfavoriteRequests(media: List<MediaItem>): List<MediaActionRequest> =
        requests(media) { uris -> createFavorite(uris, false) }

    override fun restoreRequests(media: List<MediaItem>): List<MediaActionRequest> =
        requests(media) { uris -> createTrash(uris, false) }

    override fun deleteRequests(media: List<MediaItem>): List<MediaActionRequest> = requests(media, createDelete)

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
        /**
         * Ces demandes n'existent qu'à partir d'Android 11 : avant, la préparation échoue, ce que
         * l'appelant traite comme une demande impossible (rien n'est perdu). Les tickets de l'epic 14
         * ne les proposent pas sur Android 10 (PS-181).
         */
        private fun unsupportedBeforeAndroid11() = UnsupportedOperationException("MediaStore requests need Android 11")

        /** Médias par demande, bien en dessous de la taille maximale d'un Intent (SPEC §3.5). */
        const val BATCH_SIZE: Int = 500
    }
}
