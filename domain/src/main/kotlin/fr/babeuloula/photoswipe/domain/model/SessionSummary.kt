package fr.babeuloula.photoswipe.domain.model

/**
 * Bilan affiché en fin de session, avant la confirmation système (SPEC §3.5).
 *
 * @property keptCount nombre de médias gardés.
 * @property markedForDeletionCount nombre de médias marqués pour suppression.
 * @property favoriteCount nombre de favoris en attente d'application.
 * @property estimatedFreedBytes espace libéré estimé, somme des tailles des médias marqués.
 */
data class SessionSummary(
    val keptCount: Int,
    val markedForDeletionCount: Int,
    val favoriteCount: Int,
    val estimatedFreedBytes: Long,
) {
    init {
        require(keptCount >= 0) { "keptCount must not be negative: $keptCount" }
        require(markedForDeletionCount >= 0) {
            "markedForDeletionCount must not be negative: $markedForDeletionCount"
        }
        require(favoriteCount >= 0) { "favoriteCount must not be negative: $favoriteCount" }
        require(estimatedFreedBytes >= 0) { "estimatedFreedBytes must not be negative: $estimatedFreedBytes" }
    }
}
