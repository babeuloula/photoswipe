package fr.babeuloula.photoswipe.domain.model

import java.time.LocalDate

/**
 * Statistiques d'utilisation (PS-122), dans les réglages : depuis quand l'application sert, combien
 * de médias ont été envoyés à la corbeille et combien de place cela a libéré.
 *
 * Seules les suppressions confirmées dans le dialogue de corbeille comptent, avec le poids des
 * médias au moment de la suppression ; une restauration depuis la page « Corbeille » de PhotoSwipe
 * les en retire (PS-168), pas une restauration faite depuis la galerie.
 *
 * @property since premier jour d'utilisation.
 * @property deletedCount médias envoyés à la corbeille.
 * @property freedBytes place libérée, en octets.
 */
data class UsageStatistics(
    val since: LocalDate,
    val deletedCount: Int = 0,
    val freedBytes: Long = 0,
) {
    /** Statistiques après une suppression confirmée de [count] médias pesant [bytes] octets. */
    fun afterDeletion(
        count: Int,
        bytes: Long,
    ): UsageStatistics = copy(deletedCount = deletedCount + count, freedBytes = freedBytes + bytes)

    /** Statistiques après la restauration de [count] médias pesant [bytes] octets, jamais sous zéro. */
    fun afterRestore(
        count: Int,
        bytes: Long,
    ): UsageStatistics =
        copy(
            deletedCount = (deletedCount - count).coerceAtLeast(0),
            freedBytes = (freedBytes - bytes).coerceAtLeast(0),
        )
}
