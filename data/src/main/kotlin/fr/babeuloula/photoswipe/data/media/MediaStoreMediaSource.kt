package fr.babeuloula.photoswipe.data.media

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.MediaType
import fr.babeuloula.photoswipe.domain.port.MediaSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.Instant

/**
 * [MediaSource] sur MediaStore (SPEC §5.3) : lit les photos de la galerie.
 *
 * Les requêtes passent par un `Bundle` d'arguments (`QUERY_ARG_SQL_SELECTION`), avec des
 * sélections sans fonction SQL de date (voir [takenInSelection]). Les listes d'identifiants sont
 * découpées en lots de [MAX_IDS_PER_QUERY]. Le travail se fait sur [ioDispatcher], jamais sur le
 * thread principal.
 *
 * Date de prise de vue : `DATE_TAKEN` (millisecondes). Si elle est absente ou nulle, on se replie
 * sur `DATE_ADDED` (secondes), la date d'ajout à la galerie (SPEC §3.1) : mieux vaut une date
 * approximative qu'un souvenir perdu.
 *
 * @param contentResolver accès aux fournisseurs de contenu d'Android.
 * @param ioDispatcher dispatcher des entrées-sorties, injecté pour les tests.
 */
class MediaStoreMediaSource(
    private val contentResolver: ContentResolver,
    private val ioDispatcher: CoroutineDispatcher,
) : MediaSource {
    override suspend fun mediaTakenIn(ranges: List<OpenEndRange<Instant>>): List<MediaItem> =
        if (ranges.isEmpty()) emptyList() else query(takenInSelection(ranges)).distinctBy { it.id }

    override suspend fun mediaByIds(ids: Set<MediaId>): List<MediaItem> =
        ids.map { it.value }.chunked(MAX_IDS_PER_QUERY).flatMap { batch -> query(idsSelection(batch)) }

    private suspend fun query(selection: MediaStoreSelection): List<MediaItem> =
        withContext(ioDispatcher) {
            val arguments =
                Bundle().apply {
                    putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection.sql)
                    putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selection.args.toTypedArray())
                }
            contentResolver.query(IMAGES, PROJECTION, arguments, null)?.use { it.toImages() }.orEmpty()
        }

    private fun Cursor.toImages(): List<MediaItem> {
        val id = getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
        val dateTaken = getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
        val dateAdded = getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
        val size = getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
        return buildList {
            while (moveToNext()) {
                val mediaId = getLong(id)
                add(
                    MediaItem(
                        id = MediaId(mediaId),
                        uri = ContentUris.withAppendedId(IMAGES, mediaId).toString(),
                        type = MediaType.IMAGE,
                        takenAt = takenAt(dateTaken, dateAdded),
                        sizeBytes = if (isNull(size)) 0 else getLong(size).coerceAtLeast(0),
                    ),
                )
            }
        }
    }

    /** `DATE_TAKEN` s'il est renseigné et non nul, sinon `DATE_ADDED` (SPEC §3.1). */
    private fun Cursor.takenAt(
        dateTakenColumn: Int,
        dateAddedColumn: Int,
    ): Instant {
        val dateTaken = if (isNull(dateTakenColumn)) 0 else getLong(dateTakenColumn)
        return if (dateTaken > 0) Instant.ofEpochMilli(dateTaken) else Instant.ofEpochSecond(getLong(dateAddedColumn))
    }

    private companion object {
        /** Nombre maximal d'identifiants par requête, loin sous la limite de paramètres de SQLite. */
        const val MAX_IDS_PER_QUERY = 500

        val IMAGES: Uri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

        val PROJECTION =
            arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DATE_TAKEN,
                MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.SIZE,
            )
    }
}
