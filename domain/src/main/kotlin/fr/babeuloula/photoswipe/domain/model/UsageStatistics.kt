package fr.babeuloula.photoswipe.domain.model

import java.time.LocalDate

/**
 * Statistiques d'utilisation (PS-122), dans les réglages : depuis quand l'application sert, combien
 * de médias ont été envoyés à la corbeille et combien de place cela a libéré.
 *
 * Seules les suppressions confirmées dans le dialogue de corbeille comptent, avec le poids des
 * médias au moment de la suppression ; une restauration depuis la corbeille du téléphone n'est pas
 * suivie.
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
}
