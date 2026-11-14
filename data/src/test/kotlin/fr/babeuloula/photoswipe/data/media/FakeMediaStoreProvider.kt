package fr.babeuloula.photoswipe.data.media

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.provider.MediaStore

/**
 * Faux fournisseur MediaStore pour Robolectric : une vraie table SQLite en mémoire par collection
 * (images, vidéos), avec les colonnes de MediaStore. La sélection envoyée par
 * [MediaStoreMediaSource] est donc réellement exécutée par SQLite, comme sur un appareil.
 *
 * Comme MediaStore, il exclut les médias de la corbeille (`is_trashed`) sauf si la requête demande
 * `MATCH_INCLUDE` ou `MATCH_ONLY`.
 */
class FakeMediaStoreProvider : ContentProvider() {
    private lateinit var database: SQLiteDatabase

    /** Arguments des requêtes reçues, dans l'ordre. */
    val queries = mutableListOf<Bundle>()

    override fun onCreate(): Boolean {
        database = SQLiteDatabase.create(null)
        listOf(IMAGES, VIDEOS).forEach { table ->
            database.execSQL(
                "CREATE TABLE $table (" +
                    "${MediaStore.MediaColumns._ID} INTEGER PRIMARY KEY, " +
                    "${MediaStore.MediaColumns.DATE_TAKEN} INTEGER, " +
                    "${MediaStore.MediaColumns.DATE_ADDED} INTEGER NOT NULL, " +
                    "${MediaStore.MediaColumns.SIZE} INTEGER, " +
                    "${MediaStore.MediaColumns.DURATION} INTEGER, " +
                    "${MediaStore.MediaColumns.IS_TRASHED} INTEGER NOT NULL DEFAULT 0, " +
                    "${MediaStore.MediaColumns.IS_FAVORITE} INTEGER NOT NULL DEFAULT 0, " +
                    "${MediaStore.MediaColumns.DISPLAY_NAME} TEXT, " +
                    "${MediaStore.MediaColumns.RELATIVE_PATH} TEXT, " +
                    "${MediaStore.MediaColumns.WIDTH} INTEGER, " +
                    "${MediaStore.MediaColumns.HEIGHT} INTEGER, " +
                    "${MediaStore.MediaColumns.ORIENTATION} INTEGER)",
            )
        }
        return true
    }

    /** Ajoute une photo ; [dateTaken] en millisecondes, [dateAdded] en secondes, comme MediaStore. */
    fun addImage(
        id: Long,
        dateTaken: Long?,
        dateAdded: Long = 0,
        size: Long? = 1_000,
        trashed: Boolean = false,
        favorite: Boolean = false,
        fileName: String? = null,
        folder: String? = null,
    ) {
        insert(IMAGES, id, dateTaken, dateAdded, size, duration = null, trashed, favorite)
        if (fileName != null || folder != null) {
            database.execSQL(
                "UPDATE $IMAGES SET ${MediaStore.MediaColumns.DISPLAY_NAME} = ?, " +
                    "${MediaStore.MediaColumns.RELATIVE_PATH} = ? WHERE ${MediaStore.MediaColumns._ID} = ?",
                arrayOf<Any?>(fileName, folder, id),
            )
        }
    }

    /** Donne au média [id] (photo ou vidéo) les dimensions de son fichier et sa rotation. */
    fun setDimensions(
        id: Long,
        width: Int?,
        height: Int?,
        orientation: Int? = null,
    ) {
        listOf(IMAGES, VIDEOS).forEach { table ->
            database.execSQL(
                "UPDATE $table SET ${MediaStore.MediaColumns.WIDTH} = ?, ${MediaStore.MediaColumns.HEIGHT} = ?, " +
                    "${MediaStore.MediaColumns.ORIENTATION} = ? WHERE ${MediaStore.MediaColumns._ID} = ?",
                arrayOf<Any?>(width, height, orientation, id),
            )
        }
    }

    /** Ajoute une vidéo ; [duration] en millisecondes. */
    fun addVideo(
        id: Long,
        dateTaken: Long?,
        dateAdded: Long = 0,
        size: Long? = 1_000,
        duration: Long? = 5_000,
        trashed: Boolean = false,
    ) {
        insert(VIDEOS, id, dateTaken, dateAdded, size, duration, trashed, favorite = false)
    }

    @Suppress("LongParameterList")
    private fun insert(
        table: String,
        id: Long,
        dateTaken: Long?,
        dateAdded: Long,
        size: Long?,
        duration: Long?,
        trashed: Boolean,
        favorite: Boolean,
    ) {
        val values =
            ContentValues().apply {
                put(MediaStore.MediaColumns._ID, id)
                put(MediaStore.MediaColumns.DATE_TAKEN, dateTaken)
                put(MediaStore.MediaColumns.DATE_ADDED, dateAdded)
                put(MediaStore.MediaColumns.SIZE, size)
                put(MediaStore.MediaColumns.DURATION, duration)
                put(MediaStore.MediaColumns.IS_TRASHED, if (trashed) 1 else 0)
                put(MediaStore.MediaColumns.IS_FAVORITE, if (favorite) 1 else 0)
            }
        database.insertOrThrow(table, null, values)
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        queryArgs: Bundle?,
        cancellationSignal: CancellationSignal?,
    ): Cursor {
        val arguments = queryArgs ?: Bundle()
        queries += arguments
        val trashed = arguments.getInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_EXCLUDE)
        val trashFilter =
            when (trashed) {
                MediaStore.MATCH_INCLUDE -> null
                MediaStore.MATCH_ONLY -> "${MediaStore.MediaColumns.IS_TRASHED} = 1"
                else -> "${MediaStore.MediaColumns.IS_TRASHED} = 0"
            }
        val selection =
            listOfNotNull(arguments.getString(ContentResolver.QUERY_ARG_SQL_SELECTION), trashFilter)
                .joinToString(" AND ") { "($it)" }
                .ifEmpty { null }
        val selectionArgs = arguments.getStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS)
        return database.query(table(uri), projection, selection, selectionArgs, null, null, null)
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor = throw UnsupportedOperationException("queries go through a Bundle of arguments")

    private fun table(uri: Uri): String =
        when (uri) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) -> IMAGES
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) -> VIDEOS
            else -> error("unexpected uri $uri")
        }

    override fun getType(uri: Uri): String? = null

    override fun insert(
        uri: Uri,
        values: ContentValues?,
    ): Uri? = throw UnsupportedOperationException()

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = throw UnsupportedOperationException()

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = throw UnsupportedOperationException()

    private companion object {
        const val IMAGES = "images"
        const val VIDEOS = "video"
    }
}
