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
import fr.babeuloula.photoswipe.domain.service.RawPairing
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
            // Une paire RAW + JPEG devient une seule photo (PS-166).
            if (ranges.isEmpty()) emptyList() else RawPairing.pair(query(takenInSelection(ranges)).distinctBy { it.id })

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
                    val size = displaySize(columns)
                    add(
                        MediaItem(
                            id = MediaId(id),
                            uri = ContentUris.withAppendedId(collection.uri, id).toString(),
                            type = collection.type,
                            takenAt = takenAt(columns),
                            sizeBytes = nonNegativeLong(columns.size) ?: 0,
                            duration = columns.duration?.let { nonNegativeLong(it) }?.let { Duration.ofMillis(it) },
                            isFavorite = getInt(columns.favorite) == 1,
                            fileName = columns.fileName?.takeUnless { isNull(it) }?.let { getString(it) },
                            folder = columns.folder?.takeUnless { isNull(it) }?.let { getString(it) },
                            width = size?.first,
                            height = size?.second,
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

        /**
         * Largeur et hauteur affichées : `WIDTH` et `HEIGHT` sont celles du fichier, échangées
         * quand `ORIENTATION` le tourne d'un quart de tour. `null` si l'une manque (fichier pas
         * encore indexé) : la carte prend alors la forme de l'image une fois chargée.
         */
        private fun Cursor.displaySize(columns: Columns): Pair<Int, Int>? {
            val width = positiveInt(columns.width)
            val height = positiveInt(columns.height)
            val orientation = columns.orientation?.takeUnless { isNull(it) }?.let { getInt(it) } ?: 0
            return when {
                width == null || height == null -> null
                orientation % HALF_TURN_DEGREES == QUARTER_TURN_DEGREES -> height to width
                else -> width to height
            }
        }

        private fun Cursor.positiveInt(column: Int?): Int? =
            column?.takeUnless { isNull(it) }?.let { getInt(it) }?.takeIf { it > 0 }

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
            val favorite = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.IS_FAVORITE)
            val duration = cursor.getColumnIndex(MediaStore.MediaColumns.DURATION).takeIf { it >= 0 }
            val fileName = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME).takeIf { it >= 0 }
            val folder = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH).takeIf { it >= 0 }
            val width = cursor.getColumnIndex(MediaStore.MediaColumns.WIDTH).takeIf { it >= 0 }
            val height = cursor.getColumnIndex(MediaStore.MediaColumns.HEIGHT).takeIf { it >= 0 }
            val orientation = cursor.getColumnIndex(MediaStore.MediaColumns.ORIENTATION).takeIf { it >= 0 }
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

            /** Un quart de tour, en degrés : la largeur devient la hauteur. */
            const val QUARTER_TURN_DEGREES = 90

            /** Un demi-tour, en degrés : les proportions ne changent pas. */
            const val HALF_TURN_DEGREES = 180
        }
    }

/** Colonnes lues dans les deux collections. */
private val COMMON_PROJECTION =
    arrayOf(
        MediaStore.MediaColumns._ID,
        MediaStore.MediaColumns.DATE_TAKEN,
        MediaStore.MediaColumns.DATE_ADDED,
        MediaStore.MediaColumns.SIZE,
        MediaStore.MediaColumns.IS_FAVORITE,
        MediaStore.MediaColumns.DISPLAY_NAME,
        MediaStore.MediaColumns.RELATIVE_PATH,
        MediaStore.MediaColumns.WIDTH,
        MediaStore.MediaColumns.HEIGHT,
        MediaStore.MediaColumns.ORIENTATION,
    )
