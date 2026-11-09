package fr.babeuloula.photoswipe.crash

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Build
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.GZIPOutputStream

/**
 * Lien vers la page d'envoi des rapports de plantage (ADR 0033), publiée par GitHub Pages depuis
 * `pages/crash/`. Le rapport, compressé en gzip puis encodé en Base64 URL, voyage après le `#` :
 * le navigateur ne transmet jamais cette partie au serveur de la page. La page l'affiche, et ne
 * l'envoie à GlitchTip que si la personne appuie sur « Envoyer ». PhotoSwipe reste sans
 * permission `INTERNET` (SPEC §0.6) : c'est le navigateur qui va sur le réseau.
 */
object CrashReportLink {
    /** Page d'envoi des rapports. */
    const val PAGE_URL: String = "https://babeuloula.github.io/photoswipe/crash/"

    /** Clé du rapport dans le fragment de l'URL. */
    const val FRAGMENT_KEY: String = "gz"

    /** URL de la page d'envoi, avec [report] compressé dans son fragment. */
    fun url(report: String): String = "$PAGE_URL#$FRAGMENT_KEY=${encode(report)}"

    /** Rapport compressé en gzip puis encodé en Base64 URL, sans remplissage. */
    fun encode(report: String): String {
        val bytes = ByteArrayOutputStream()
        GZIPOutputStream(bytes).use { it.write(report.toByteArray(Charsets.UTF_8)) }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray())
    }

    /** Ouvre la page d'envoi de [report] ; renvoie `false` si aucun navigateur ne peut l'ouvrir. */
    fun open(
        context: Context,
        report: String,
        start: (Intent) -> Unit = context::startActivity,
    ): Boolean =
        try {
            start(Intent(Intent.ACTION_VIEW, Uri.parse(url(report))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
}

/** Application et téléphone tels que le rapport de plantage les décrit, lus au moment du plantage. */
fun Context.crashEnvironment(): CrashEnvironment {
    val packageInfo = packageManager.getPackageInfo(packageName, 0)
    return CrashEnvironment(
        applicationId = packageName,
        versionName = packageInfo.versionName.orEmpty(),
        versionCode = packageInfo.longVersionCode,
        debuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0,
        androidVersion = Build.VERSION.RELEASE.orEmpty(),
        sdkInt = Build.VERSION.SDK_INT,
        manufacturer = Build.MANUFACTURER.orEmpty(),
        model = Build.MODEL.orEmpty(),
    )
}
