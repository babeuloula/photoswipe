package fr.babeuloula.photoswipe.ui.deck

import android.content.Context
import android.text.format.DateUtils
import android.text.format.Formatter
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.MediaType
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Repère d'un souvenir, « Il y a 3 ans · 14 mai 2023 » (SPEC §4.3), en date abrégée pour tenir sur
 * le bord de film (« 14 mai 2023 » en français, « May 14, 2023 » en anglais) : pluriel et date dans
 * la langue de [context], sans concaténation (SPEC §9).
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
        takenOn.format(
            DateTimeFormatter
                .ofLocalizedDate(
                    FormatStyle.MEDIUM,
                ).withLocale(context.resources.configuration.locales[0]),
        ),
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

/**
 * Poids d'un fichier. Sous un kilo-octet, le nombre d'octets au pluriel de la langue (« 0 octet »,
 * « 512 octets »), là où Android écrirait « 0 octet(s) » ; au-delà, arrondi et localisé par Android
 * (« 3,5 Mo », « 3.5 MB »).
 */
fun fileSizeLabel(
    context: Context,
    sizeBytes: Long,
): String =
    if (sizeBytes < BYTES_PER_KILOBYTE) {
        val count = sizeBytes.toInt()
        context.resources.getQuantityString(R.plurals.file_size_bytes, count, count)
    } else {
        Formatter.formatShortFileSize(context, sizeBytes)
    }

/** Un kilo-octet pour Android, qui compte en puissances de 10 (SI) depuis Android 8. */
private const val BYTES_PER_KILOBYTE = 1000L

/**
 * Durée d'une vidéo au format des lecteurs, avec les chiffres de la langue de l'appareil :
 * « 00:32 », « 25:00 », « 1:02:03 ».
 */
fun durationLabel(duration: Duration): String = DateUtils.formatElapsedTime(duration.seconds)

/** Description TalkBack de l'image d'une carte, selon son type : « Photo du 6 novembre 2023 ». */
internal fun cardDescription(type: MediaType): Int =
    if (type == MediaType.VIDEO) R.string.deck_video_description else R.string.deck_photo_description
