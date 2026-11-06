package fr.babeuloula.photoswipe.data.media

import android.content.Context
import android.media.MediaScannerConnection
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Attente de l'indexation MediaStore dans les tests instrumentés (PS-103).
 *
 * MediaStore recalcule `DATE_TAKEN` depuis le fichier (EXIF d'une photo, `mvhd` d'une vidéo) en
 * l'indexant, de façon asynchrone après l'insertion. Sur l'émulateur logiciel de la CI, plus lent,
 * une lecture faite trop tôt ne trouvait pas le média à sa date. Les tests demandent donc eux-mêmes
 * l'indexation de leurs fichiers et attendent qu'elle soit finie ([scanOwnMedia]), puis relisent
 * avec un délai borné ([eventually]) : jamais de pause arbitraire.
 */
object MediaStoreIndexing {
    /** Délai total accordé à une lecture répétée. */
    private const val TIMEOUT_MILLIS = 30_000L

    /** Intervalle entre deux lectures. */
    private const val POLL_MILLIS = 200L

    /**
     * Fait indexer par MediaStore tous les médias ajoutés par le test (son paquet en est le
     * propriétaire), photos et vidéos, et attend la fin de l'indexation de chacun ; renvoie le
     * nombre de fichiers indexés.
     */
    suspend fun scanOwnMedia(context: Context): Int {
        val paths = ownPaths(context)
        if (paths.isEmpty()) return 0
        suspendCancellableCoroutine { continuation ->
            var remaining = paths.size
            MediaScannerConnection.scanFile(context, paths.toTypedArray(), null) { _, _ ->
                val done = synchronized(this) { --remaining == 0 }
                if (done && continuation.isActive) continuation.resume(Unit)
            }
        }
        return paths.size
    }

    /**
     * Lit [read] jusqu'à obtenir [expected], au plus [TIMEOUT_MILLIS], et renvoie la dernière valeur
     * lue. L'attente se fait en temps réel, hors de l'horloge virtuelle de `runTest`.
     */
    suspend fun <T> eventually(
        expected: T,
        read: suspend () -> T,
    ): T {
        var value = read()
        var waited = 0L
        while (value != expected && waited < TIMEOUT_MILLIS) {
            withContext(Dispatchers.IO) { delay(POLL_MILLIS) }
            waited += POLL_MILLIS
            value = read()
        }
        return value
    }

    /** Chemins des fichiers dont le test est propriétaire, dans les collections de photos et de vidéos. */
    @Suppress("DEPRECATION") // `DATA` : seul moyen d'obtenir le chemin à faire indexer.
    private fun ownPaths(context: Context): List<String> =
        listOf(
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
        ).flatMap { collection ->
            context.contentResolver
                .query(
                    collection,
                    arrayOf(MediaStore.MediaColumns.DATA),
                    "${MediaStore.MediaColumns.OWNER_PACKAGE_NAME} = ?",
                    arrayOf(context.packageName),
                    null,
                )?.use { cursor -> buildList { while (cursor.moveToNext()) cursor.getString(0)?.let(::add) } }
                .orEmpty()
        }
}
