package fr.babeuloula.photoswipe.ui.deck

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import fr.babeuloula.photoswipe.domain.model.MediaType

/**
 * Feuille de partage d'Android pour le média [uri] (SPEC §2). L'application destinataire reçoit le
 * droit de lire ce seul média, le temps du partage : PhotoSwipe n'envoie rien elle-même.
 *
 * @param chooserTitle titre de la feuille de partage.
 */
fun shareIntent(
    uri: String,
    type: MediaType,
    chooserTitle: String,
): Intent {
    val content = Uri.parse(uri)
    val send =
        Intent(Intent.ACTION_SEND)
            .setType(if (type == MediaType.VIDEO) "video/*" else "image/*")
            .putExtra(Intent.EXTRA_STREAM, content)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    // Le ClipData porte l'autorisation de lecture jusqu'à l'application choisie.
    send.clipData = ClipData.newRawUri(null, content)
    return Intent.createChooser(send, chooserTitle)
}

/**
 * Lance la feuille de partage [intent] avec [start] ; renvoie `false` si Android la refuse. C'est le
 * cas d'un média que l'application ne peut pas confier à une autre : fichier disparu, média hors de
 * la sélection d'un accès partiel, données de démo sans fichier.
 */
fun launchShare(
    intent: Intent,
    start: (Intent) -> Unit,
): Boolean =
    try {
        start(intent)
        true
    } catch (_: SecurityException) {
        false
    } catch (_: ActivityNotFoundException) {
        false
    }
