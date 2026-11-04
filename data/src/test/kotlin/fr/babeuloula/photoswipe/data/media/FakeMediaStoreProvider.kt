package fr.babeuloula.photoswipe.data.media

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.MediaStore

/**
 * Faux fournisseur MediaStore pour Robolectric : une vraie table SQLite en mémoire par collection,
 * avec les colonnes de MediaStore. La sélection envoyée par [MediaStoreMediaSource] est donc
 * réellement exécutée par SQLite, comme sur un appareil.
 */
class FakeMediaStoreProvider : ContentProvider() {
    private lateinit var database: SQLiteDatabase

    override fun onCreate(): Boolean {
        database = SQLiteDatabase.create(null)
        database.execSQL(
            "CREATE TABLE images (" +
                "${MediaStore.MediaColumns._ID} INTEGER PRIMARY KEY, " +
                "${MediaStore.MediaColumns.DATE_TAKEN} INTEGER, " +
                "${MediaStore.MediaColumns.DATE_ADDED} INTEGER NOT NULL, " +
                "${MediaStore.MediaColumns.SIZE} INTEGER)",
        )
        return true
    }

    /** Ajoute une photo ; [dateTaken] en millisecondes, [dateAdded] en secondes, comme MediaStore. */
    fun addImage(
        id: Long,
        dateTaken: Long?,
        dateAdded: Long = 0,
        size: Long? = 1_000,
    ) {
        val values =
            ContentValues().apply {
                put(MediaStore.MediaColumns._ID, id)
                put(MediaStore.MediaColumns.DATE_TAKEN, dateTaken)
                put(MediaStore.MediaColumns.DATE_ADDED, dateAdded)
                put(MediaStore.MediaColumns.SIZE, size)
            }
        database.insertOrThrow("images", null, values)
    }

    /** Clauses `WHERE` reçues, dans l'ordre des requêtes. */
    val selections = mutableListOf<String?>()

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        selections += selection
        return database.query(table(uri), projection, selection, selectionArgs, null, null, sortOrder)
    }

    private fun table(uri: Uri): String =
        when (uri) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) -> "images"
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
}
