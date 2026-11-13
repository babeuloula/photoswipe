package fr.babeuloula.photoswipe.domain.model

import java.time.LocalDate
import java.time.YearMonth

/**
 * Activité de tri d'une journée (PS-154, ADR 0037) : ce que l'utilisateur a décidé ce jour-là,
 * quel que soit le jour de revue trié.
 *
 * @property day jour où les décisions ont été prises.
 * @property keptCount médias gardés, favoris compris.
 * @property favoriteCount médias gardés et mis en favori.
 * @property trashedCount médias envoyés à la corbeille par une confirmation.
 * @property freedBytes poids de ces médias, en octets.
 */
data class DailyActivity(
    val day: LocalDate,
    val keptCount: Int = 0,
    val favoriteCount: Int = 0,
    val trashedCount: Int = 0,
    val freedBytes: Long = 0,
)

/**
 * Activité de tri d'un mois, somme de ses jours.
 *
 * @property month mois concerné.
 * @property keptCount médias gardés, favoris compris.
 * @property favoriteCount médias gardés et mis en favori.
 * @property trashedCount médias envoyés à la corbeille.
 * @property freedBytes poids de ces médias, en octets.
 */
data class MonthlyActivity(
    val month: YearMonth,
    val keptCount: Int = 0,
    val favoriteCount: Int = 0,
    val trashedCount: Int = 0,
    val freedBytes: Long = 0,
) {
    /** Médias triés ce mois-là : gardés ou envoyés à la corbeille. */
    val sortedCount: Int get() = keptCount + trashedCount
}

/**
 * Activité des derniers mois, pour les graphiques de la page « Progression ».
 *
 * @property months mois du plus ancien au plus récent, sans trou : un mois sans tri a des compteurs nuls.
 * @property since premier jour enregistré dans l'historique, `null` s'il est vide : l'historique
 * ne commence qu'à la version qui l'a introduit.
 */
data class ActivityReport(
    val months: List<MonthlyActivity>,
    val since: LocalDate? = null,
) {
    /** Médias gardés sur la période. */
    val keptCount: Int get() = months.sumOf { it.keptCount }

    /** Médias envoyés à la corbeille sur la période. */
    val trashedCount: Int get() = months.sumOf { it.trashedCount }

    /** Vrai si rien n'a été trié sur la période. */
    val isEmpty: Boolean get() = keptCount + trashedCount == 0

    /** Part des médias gardés parmi ceux triés, en pourcentage ; `null` si rien n'est trié. */
    val keptPercent: Int? get() = if (isEmpty) null else keptCount * PERCENT / (keptCount + trashedCount)

    private companion object {
        const val PERCENT = 100
    }
}
