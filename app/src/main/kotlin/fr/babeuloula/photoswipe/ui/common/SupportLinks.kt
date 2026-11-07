package fr.babeuloula.photoswipe.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Liens vers l'extérieur de PhotoSwipe : noter l'application sur le Play Store, faire un don avec
 * PayPal (PS-120), lire son code source sur GitHub (PS-127). Ils s'ouvrent dans le Play Store ou le
 * navigateur : PhotoSwipe reste sans permission `INTERNET` (SPEC §0.6, ADR 0029).
 */
object SupportLinks {
    /** Page de don PayPal de Jérémy. */
    const val DONATION_URL: String = "https://www.paypal.com/paypalme/jeremyreynaud"

    /** Dépôt public du code source de PhotoSwipe. */
    const val SOURCE_CODE_URL: String = "https://github.com/babeuloula/photoswipe"

    /** Fiche de l'application dans l'application Play Store. */
    fun ratingIntent(packageName: String): Intent = view("market://details?id=$packageName")

    /** Fiche de l'application sur le site du Play Store, quand l'application Play Store manque. */
    fun ratingWebIntent(packageName: String): Intent =
        view("https://play.google.com/store/apps/details?id=$packageName")

    /** Page de don PayPal. */
    fun donationIntent(): Intent = view(DONATION_URL)

    /** Dépôt du code source. */
    fun sourceCodeIntent(): Intent = view(SOURCE_CODE_URL)

    /** Ouvre la fiche Play Store, ou sa page web ; renvoie `false` si rien ne peut l'ouvrir. */
    fun openRating(
        context: Context,
        start: (Intent) -> Unit = context::startActivity,
    ): Boolean =
        tryStart(ratingIntent(context.packageName), start) || tryStart(ratingWebIntent(context.packageName), start)

    /** Ouvre la page de don ; renvoie `false` si aucun navigateur ne peut l'ouvrir. */
    fun openDonation(
        context: Context,
        start: (Intent) -> Unit = context::startActivity,
    ): Boolean = tryStart(donationIntent(), start)

    /** Ouvre le dépôt du code source ; renvoie `false` si aucun navigateur ne peut l'ouvrir. */
    fun openSourceCode(
        context: Context,
        start: (Intent) -> Unit = context::startActivity,
    ): Boolean = tryStart(sourceCodeIntent(), start)

    private fun view(url: String): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun tryStart(
        intent: Intent,
        start: (Intent) -> Unit,
    ): Boolean =
        try {
            start(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
}
