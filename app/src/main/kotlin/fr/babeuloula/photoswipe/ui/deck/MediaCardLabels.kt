package fr.babeuloula.photoswipe.ui.deck

import android.content.Context
import android.text.format.Formatter
import fr.babeuloula.photoswipe.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Étiquette d'un souvenir, « Il y a 3 ans · 14 mai 2023 » (SPEC §4.3) : pluriel et date dans la
 * langue de [context], sans concaténation (SPEC §9).
 */
fun memoryLabel(
    context: Context,
    yearsAgo: Int,
    takenOn: LocalDate,
): String {
    val resources = context.resources
    return resources.getString(
        R.string.deck_memory_label,
        resources.getQuantityString(R.plurals.deck_years_ago, yearsAgo, yearsAgo),
        localizedDate(context, takenOn),
    )
}

/** Date longue de [date] dans la langue de [context], par exemple « 14 mai 2023 ». */
fun localizedDate(
    context: Context,
    date: LocalDate,
): String =
    date.format(
        DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(context.resources.configuration.locales[0]),
    )

/** Poids d'un fichier, arrondi et localisé par Android (« 3,5 Mo », « 3.5 MB »). */
fun fileSizeLabel(
    context: Context,
    sizeBytes: Long,
): String = Formatter.formatShortFileSize(context, sizeBytes)
