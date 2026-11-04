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
import fr.babeuloula.photoswipe.domain.port.MediaSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

/**
 * [MediaSource] sur MediaStore (SPEC §5.3) : lit les photos et les vidéos de la galerie.
 *
 * Chaque lecture interroge les deux collections (images, vidéos), pour que chaque média garde l'URI
 * de sa collection, celle qu'attendent les demandes système de corbeille et de favori. Les requêtes
 * passent par un `Bundle` d'arguments, avec des sélections sans fonction SQL de date (voir
 * [takenInSelection], qui documente aussi le repli sur `DATE_ADDED`). Les médias de la corbeille
 * système sont exclus explicitement (`MATCH_EXCLUDE`, SPEC §3.1). Les listes d'identifiants sont
 * découpées en lots de [MAX_IDS_PER_QUERY]. Le travail se fait sur [ioDispatcher].
 *
 * @param contentResolver accès aux fournisseurs de contenu d'Android.
 * @param ioDispatcher dispatcher des entrées-sorties, injecté pour les tests.
 */
class MediaStoreMediaSource
    @Inject
    constructor(
        private val contentResolver: ContentResolver,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : MediaSource {
        override suspend fun mediaTakenIn(ranges: List<OpenEndRange<Instant>>): List<MediaItem> =
            if (ranges.isEmpty()) emptyList() else query(takenInSelection(ranges)).distinctBy { it.id }

        override suspend fun mediaByIds(ids: Set<MediaId>): List<MediaItem> =
            ids.map { it.value }.chunked(MAX_IDS_PER_QUERY).flatMap { batch -> query(idsSelection(batch)) }

        /** Médias des deux collections qui répondent à [selection]. */
        private suspend fun query(selection: MediaStoreSelection): List<MediaItem> =
            withContext(ioDispatcher) {
                Collection.entries.flatMap { collection -> query(collection, selection) }
            }

        private fun query(
            collection: Collection,
            selection: MediaStoreSelection,
        ): List<MediaItem> {
            val arguments =
                Bundle().apply {
                    putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection.sql)
                    putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selection.args.toTypedArray())
                    putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_EXCLUDE)
                }
            return contentResolver
                .query(collection.uri, collection.projection, arguments, null)
                ?.use {
                    it.toMedia(collection)
                }.orEmpty()
        }

        private fun Cursor.toMedia(collection: Collection): List<MediaItem> {
            val columns = Columns(this)
            return buildList {
                while (moveToNext()) {
                    val id = getLong(columns.id)
                    add(
                        MediaItem(
                            id = MediaId(id),
                            uri = ContentUris.withAppendedId(collection.uri, id).toString(),
                            type = collection.type,
                            takenAt = takenAt(columns),
                            sizeBytes = nonNegativeLong(columns.size) ?: 0,
                            duration = columns.duration?.let { nonNegativeLong(it) }?.let { Duration.ofMillis(it) },
                        ),
                    )
                }
            }
        }

        /** `DATE_TAKEN` (millisecondes) s'il est renseigné et positif, sinon `DATE_ADDED` (secondes) : SPEC §3.1. */
        private fun Cursor.takenAt(columns: Columns): Instant {
            val dateTaken = nonNegativeLong(columns.dateTaken) ?: 0
            return if (dateTaken >
                0
            ) {
                Instant.ofEpochMilli(dateTaken)
            } else {
                Instant.ofEpochSecond(getLong(columns.dateAdded))
            }
        }

        private fun Cursor.nonNegativeLong(column: Int): Long? =
            if (isNull(column)) {
                null
            } else {
                getLong(column).takeIf {
                    it >=
                        0
                }
            }

        /** Index des colonnes lues ; la durée n'existe que pour les vidéos. */
        private class Columns(
            cursor: Cursor,
        ) {
            val id = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val dateTaken = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            val dateAdded = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val size = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val duration = cursor.getColumnIndex(MediaStore.MediaColumns.DURATION).takeIf { it >= 0 }
        }

        /** Collections MediaStore lues, avec leurs colonnes. */
        private enum class Collection(
            val uri: Uri,
            val type: MediaType,
            val projection: Array<String>,
        ) {
            IMAGES(
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
                MediaType.IMAGE,
                COMMON_PROJECTION,
            ),
            VIDEOS(
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
                MediaType.VIDEO,
                COMMON_PROJECTION + MediaStore.MediaColumns.DURATION,
            ),
        }

        private companion object {
            /** Nombre maximal d'identifiants par requête, loin sous la limite de paramètres de SQLite. */
            const val MAX_IDS_PER_QUERY = 500
        }
    }

/** Colonnes lues dans les deux collections. */
private val COMMON_PROJECTION =
    arrayOf(
        MediaStore.MediaColumns._ID,
        MediaStore.MediaColumns.DATE_TAKEN,
        MediaStore.MediaColumns.DATE_ADDED,
        MediaStore.MediaColumns.SIZE,
    )
