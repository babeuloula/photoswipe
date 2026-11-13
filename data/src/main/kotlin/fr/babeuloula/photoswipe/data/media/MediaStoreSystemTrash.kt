package fr.babeuloula.photoswipe.data.media

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import fr.babeuloula.photoswipe.data.di.IoDispatcher
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.MediaType
import fr.babeuloula.photoswipe.domain.model.TrashedMedia
import fr.babeuloula.photoswipe.domain.port.SystemTrash
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

/**
 * [SystemTrash] sur MediaStore (PS-168) : ne lit que les médias de la corbeille
 * (`QUERY_ARG_MATCH_TRASHED` à `MATCH_ONLY`), dans les collections des photos et des vidéos, pour
 * garder l'URI qu'attendent les demandes de restauration et de suppression définitive.
 *
 * `DATE_EXPIRES` (secondes epoch) donne l'instant où Android videra le média de sa corbeille ; une
 * valeur absente ou nulle est rendue inconnue. Ce que MediaStore renvoie d'un média en corbeille
 * (taille, date d'expiration) est vérifié par `TrashInstrumentedTest`.
 *
 * @param contentResolver accès aux fournisseurs de contenu d'Android.
 * @param ioDispatcher dispatcher des entrées-sorties, injecté pour les tests.
 */
class MediaStoreSystemTrash
    @Inject
    constructor(
        private val contentResolver: ContentResolver,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : SystemTrash {
        override suspend fun trashed(ids: Set<MediaId>): List<TrashedMedia> =
            withContext(ioDispatcher) {
                ids.map { it.value }.chunked(MAX_IDS_PER_QUERY).flatMap { batch ->
                    Collection.entries.flatMap { query(it, idsSelection(batch)) }
                }
            }

        private fun query(
            collection: Collection,
            selection: MediaStoreSelection,
        ): List<TrashedMedia> {
            val arguments =
                Bundle().apply {
                    putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection.sql)
                    putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selection.args.toTypedArray())
                    putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
                }
            return contentResolver
                .query(collection.uri, PROJECTION, arguments, null)
                ?.use { it.toTrashed(collection) }
                .orEmpty()
        }

        private fun Cursor.toTrashed(collection: Collection): List<TrashedMedia> {
            val id = getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val dateTaken = getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            val dateAdded = getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val size = getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val expires = getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_EXPIRES)
            val duration = getColumnIndexOrThrow(MediaStore.MediaColumns.DURATION)
            return buildList {
                while (moveToNext()) {
                    val mediaId = getLong(id)
                    val taken = positiveLong(dateTaken)
                    add(
                        TrashedMedia(
                            media =
                                MediaItem(
                                    id = MediaId(mediaId),
                                    uri = ContentUris.withAppendedId(collection.uri, mediaId).toString(),
                                    type = collection.type,
                                    takenAt =
                                        taken?.let(Instant::ofEpochMilli) ?: Instant.ofEpochSecond(getLong(dateAdded)),
                                    sizeBytes = positiveLong(size) ?: 0,
                                    duration =
                                        if (collection.type == MediaType.VIDEO) {
                                            positiveLong(duration)?.let(Duration::ofMillis)
                                        } else {
                                            null
                                        },
                                ),
                            purgeAt = positiveLong(expires)?.let(Instant::ofEpochSecond),
                        ),
                    )
                }
            }
        }

        private fun Cursor.positiveLong(column: Int): Long? =
            if (isNull(column)) {
                null
            } else {
                getLong(column).takeIf {
                    it >
                        0
                }
            }

        /** Collections MediaStore lues. */
        private enum class Collection(
            val uri: Uri,
            val type: MediaType,
        ) {
            IMAGES(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL), MediaType.IMAGE),
            VIDEOS(MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL), MediaType.VIDEO),
        }

        private companion object {
            /** Identifiants par requête, loin sous la limite de paramètres de SQLite. */
            const val MAX_IDS_PER_QUERY = 500

            /** Colonnes lues ; `DURATION` existe dans les deux collections depuis Android 10. */
            val PROJECTION =
                arrayOf(
                    MediaStore.MediaColumns._ID,
                    MediaStore.MediaColumns.DATE_TAKEN,
                    MediaStore.MediaColumns.DATE_ADDED,
                    MediaStore.MediaColumns.SIZE,
                    MediaStore.MediaColumns.DATE_EXPIRES,
                    MediaStore.MediaColumns.DURATION,
                )
        }
    }
