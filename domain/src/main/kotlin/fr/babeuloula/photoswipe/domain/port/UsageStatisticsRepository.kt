package fr.babeuloula.photoswipe.domain.port

import fr.babeuloula.photoswipe.domain.model.UsageStatistics

/**
 * Statistiques d'utilisation (PS-122, ADR 0030), cumulées sur l'appareil. Implémenté dans `:data`
 * sur DataStore.
 */
interface UsageStatisticsRepository {
    /**
     * Statistiques enregistrées ; tant que rien n'est supprimé, des totaux nuls depuis le premier
     * jour d'utilisation (celui de l'installation).
     */
    suspend fun statistics(): UsageStatistics

    /** Ajoute une suppression confirmée de [count] médias pesant [bytes] octets. */
    suspend fun recordDeletion(
        count: Int,
        bytes: Long,
    )

    /** Remet les totaux à zéro (outils développeur, avec la remise à zéro des décisions). */
    suspend fun clear()
}
