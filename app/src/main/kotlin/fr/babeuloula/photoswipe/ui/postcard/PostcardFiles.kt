package fr.babeuloula.photoswipe.ui.postcard

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Cartes postales prêtes à partager (PS-177), écrites dans le cache de l'application et confiées
 * aux autres applications par le `FileProvider` déclaré dans le manifeste. Une seule carte est
 * gardée : les précédentes sont effacées à chaque nouvelle carte. Rien n'est écrit dans la galerie.
 */
class PostcardFiles(
    private val context: Context,
) {
    /** Dossier des cartes, dans le cache (`res/xml/file_paths.xml`). */
    val directory: File get() = File(context.cacheDir, DIRECTORY)

    /** Écrit [postcard] en JPEG après avoir effacé les cartes précédentes, et renvoie son fichier. */
    fun write(postcard: Bitmap): File {
        directory.mkdirs()
        directory.listFiles()?.forEach { it.delete() }
        val file = File(directory, "PhotoSwipe-${System.currentTimeMillis()}.jpg")
        file.outputStream().use { postcard.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        return file
    }

    /** URI `content://` de la carte [file], à confier à l'application choisie au partage. */
    fun shareableUri(file: File): Uri = FileProvider.getUriForFile(context, authority(context), file)

    companion object {
        /** Sous-dossier du cache où vivent les cartes. */
        const val DIRECTORY = "postcards"

        /** Autorité du `FileProvider` des cartes, propre à chaque variante (debug, release). */
        fun authority(context: Context): String = "${context.packageName}.postcards"

        /** Qualité JPEG : invisible à l'œil, deux fois plus léger que la qualité maximale. */
        private const val JPEG_QUALITY = 90
    }
}

/**
 * Feuille de partage d'Android pour la carte postale [uri] : l'application choisie reçoit le
 * droit de lire cette seule carte, le temps du partage.
 */
fun postcardShareIntent(
    uri: Uri,
    chooserTitle: String,
): Intent {
    val send =
        Intent(Intent.ACTION_SEND)
            .setType("image/jpeg")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    // Le ClipData porte l'autorisation de lecture jusqu'à l'application choisie.
    send.clipData = ClipData.newRawUri(null, uri)
    return Intent.createChooser(send, chooserTitle)
}
