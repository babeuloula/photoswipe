package fr.babeuloula.photoswipe.ui.deck

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.MediaType
import fr.babeuloula.photoswipe.share.ShareTargetReceiver

/**
 * Feuille de partage d'Android pour le média [uri] (SPEC §2). L'application destinataire reçoit le
 * droit de lire ce seul média, le temps du partage : PhotoSwipe n'envoie rien elle-même.
 *
 * @param chooserTitle titre de la feuille de partage.
 * @param onChosen rappel qui reçoit l'application choisie (PS-163), `null` pour n'en rien retenir.
 */
fun shareIntent(
    uri: String,
    type: MediaType,
    chooserTitle: String,
    onChosen: IntentSender? = null,
): Intent {
    val content = Uri.parse(uri)
    val send =
        Intent(Intent.ACTION_SEND)
            .setType(if (type == MediaType.VIDEO) "video/*" else "image/*")
            .putExtra(Intent.EXTRA_STREAM, content)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    // Le ClipData porte l'autorisation de lecture jusqu'à l'application choisie.
    send.clipData = ClipData.newRawUri(null, content)
    return Intent.createChooser(send, chooserTitle, onChosen)
}

/**
 * « Renvoyer avec … » (PS-163) : le même envoi que [shareIntent], adressé directement à
 * l'application [component] (`paquet/classe`), sans feuille de partage. Elle s'ouvre sur son
 * propre choix de destinataire.
 */
fun shareWithIntent(
    uri: String,
    type: MediaType,
    component: String,
): Intent {
    val content = Uri.parse(uri)
    return Intent(Intent.ACTION_SEND)
        .setType(if (type == MediaType.VIDEO) "video/*" else "image/*")
        .putExtra(Intent.EXTRA_STREAM, content)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        .setComponent(ComponentName.unflattenFromString(component))
        .apply { clipData = ClipData.newRawUri(null, content) }
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

/**
 * Confie le média de [effect] à une autre application : feuille de partage (qui retient
 * l'application choisie, PS-163), envoi direct à la dernière application, ou « Ouvrir avec »
 * (PS-162). Renvoie le message à afficher si Android refuse, `null` sinon.
 */
fun launchElsewhere(
    context: Context,
    effect: DeckEffect.ElsewhereEffect,
): String? {
    val intent =
        when (effect) {
            is DeckEffect.Share -> {
                val title = context.getString(R.string.deck_share)
                shareIntent(effect.uri, effect.type, title, ShareTargetReceiver.callback(context))
            }

            is DeckEffect.ShareWith -> {
                shareWithIntent(effect.uri, effect.type, effect.component)
            }

            is DeckEffect.OpenWith -> {
                openWithIntent(effect.uri, effect.type, context.getString(R.string.deck_open_with))
            }
        }
    if (launchShare(intent, context::startActivity)) return null
    val failure = if (effect is DeckEffect.OpenWith) R.string.deck_open_with_failed else R.string.deck_share_failed
    return context.getString(failure)
}
