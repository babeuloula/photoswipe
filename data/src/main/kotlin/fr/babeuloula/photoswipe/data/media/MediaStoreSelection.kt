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
 * Les bornes sont comparées directement aux colonnes, sans aucune fonction SQL de date : MediaStore
 * restreint les sélections depuis Android 11 (SPEC §5.3). Les plages viennent du domaine
 * (`SameDayMatcher.rangesFor`, une par année).
 *
 * Date de prise de vue (SPEC §3.1) : `DATE_TAKEN`, en millisecondes. S'il est absent ou nul, on se
 * replie sur `DATE_ADDED`, en secondes : un média dont la date de prise de vue est inconnue
 * (capture d'écran, image reçue par messagerie) reste proposé à sa date d'ajout plutôt que perdu.
 * Les bornes en secondes sont arrondies au-dessus, pour que `DATE_ADDED × 1000` reste dans la plage.
 */
internal fun takenInSelection(ranges: List<OpenEndRange<Instant>>): MediaStoreSelection {
    require(ranges.isNotEmpty()) { "at least one range is required" }
    val taken = MediaStore.MediaColumns.DATE_TAKEN
    val added = MediaStore.MediaColumns.DATE_ADDED
    val clause = "(($taken >= ? AND $taken < ?) OR (($taken IS NULL OR $taken <= 0) AND $added >= ? AND $added < ?))"
    return MediaStoreSelection(
        sql = ranges.joinToString(" OR ") { clause },
        args =
            ranges.flatMap {
                listOf(
                    it.start.toEpochMilli(),
                    it.endExclusive.toEpochMilli(),
                    it.start.toEpochSecondRoundedUp(),
                    it.endExclusive.toEpochSecondRoundedUp(),
                ).map(Long::toString)
            },
    )
}

/** Secondes epoch arrondies au-dessus : la plus petite seconde `s` telle que `s × 1000 ≥ cet instant`. */
private fun Instant.toEpochSecondRoundedUp(): Long = if (nano == 0) epochSecond else epochSecond + 1

/** Sélection des médias d'identifiants [ids] (non vide, déjà découpé sous la limite de paramètres). */
internal fun idsSelection(ids: List<Long>): MediaStoreSelection {
    require(ids.isNotEmpty()) { "at least one id is required" }
    return MediaStoreSelection(
        sql = "${MediaStore.MediaColumns._ID} IN (${ids.joinToString(",") { "?" }})",
        args = ids.map { it.toString() },
    )
}
