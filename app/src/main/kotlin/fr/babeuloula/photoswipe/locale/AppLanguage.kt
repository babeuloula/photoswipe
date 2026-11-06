package fr.babeuloula.photoswipe.locale

import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import java.util.Locale

/**
 * Langue choisie pour l'application dans le réglage de langue par application d'Android 13+
 * (SPEC §9), `null` si elle suit la langue du téléphone.
 */
fun appLanguage(context: Context): Locale? =
    context
        .getSystemService(LocaleManager::class.java)
        .applicationLocales
        .takeUnless { it.isEmpty }
        ?.get(0)

/** Nom d'une langue dans cette langue même, avec une majuscule : « Français », « English ». */
fun Locale.nativeName(): String = getDisplayLanguage(this).replaceFirstChar { it.titlecase(this) }

/** Intent du réglage de langue de PhotoSwipe dans les réglages d'Android 13+. */
fun appLanguageSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APP_LOCALE_SETTINGS, Uri.fromParts("package", context.packageName, null))
