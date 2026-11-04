package fr.babeuloula.photoswipe.data.media

import android.provider.MediaStore
import java.time.Instant

/**
 * Clause `WHERE` d'une requête MediaStore et ses arguments, passés séparément pour ne jamais
 * insérer de valeur dans le SQL.
 */
internal data class MediaStoreSelection(
    val sql: String,
    val args: List<String>,
)

/**
 * Sélection des médias pris dans l'une des [ranges], chacune ouverte à droite (`début..<fin`).
 *
 * Les bornes sont des millisecondes epoch comparées directement à `DATE_TAKEN`, sans aucune
 * fonction SQL de date : MediaStore restreint les sélections depuis Android 11 (SPEC §5.3). Les
 * plages viennent du domaine (`SameDayMatcher.rangesFor`, une par année).
 */
internal fun takenInSelection(ranges: List<OpenEndRange<Instant>>): MediaStoreSelection {
    require(ranges.isNotEmpty()) { "at least one range is required" }
    val clause = "(${MediaStore.MediaColumns.DATE_TAKEN} >= ? AND ${MediaStore.MediaColumns.DATE_TAKEN} < ?)"
    return MediaStoreSelection(
        sql = ranges.joinToString(" OR ") { clause },
        args = ranges.flatMap { listOf(it.start.toEpochMilli().toString(), it.endExclusive.toEpochMilli().toString()) },
    )
}

/** Sélection des médias d'identifiants [ids] (non vide, déjà découpé sous la limite de paramètres). */
internal fun idsSelection(ids: List<Long>): MediaStoreSelection {
    require(ids.isNotEmpty()) { "at least one id is required" }
    return MediaStoreSelection(
        sql = "${MediaStore.MediaColumns._ID} IN (${ids.joinToString(",") { "?" }})",
        args = ids.map { it.toString() },
    )
}
