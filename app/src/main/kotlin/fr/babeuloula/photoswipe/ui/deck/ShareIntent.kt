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
 * « Ouvrir avec » (PS-162) : le sélecteur d'Android pour voir ou retoucher le média [uri] dans une
 * autre application. Il liste les visionneuses (`ACTION_VIEW`) et, en plus, les éditeurs qui ne
 * déclarent que l'édition (`ACTION_EDIT`, intent alternatif). L'application choisie reçoit le droit
 * de lire et d'écrire ce seul média, le temps de l'ouverture.
 *
 * @param chooserTitle titre du sélecteur.
 */
fun openWithIntent(
    uri: String,
    type: MediaType,
    chooserTitle: String,
): Intent {
    val content = Uri.parse(uri)
    val mime = if (type == MediaType.VIDEO) "video/*" else "image/*"
    val grants = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    val view = Intent(Intent.ACTION_VIEW).setDataAndType(content, mime).addFlags(grants)
    val edit = Intent(Intent.ACTION_EDIT).setDataAndType(content, mime).addFlags(grants)
    return Intent
        .createChooser(view, chooserTitle)
        .putExtra(Intent.EXTRA_ALTERNATE_INTENTS, arrayOf(edit))
        .addFlags(grants)
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
