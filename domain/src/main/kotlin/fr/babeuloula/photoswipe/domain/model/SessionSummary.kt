package fr.babeuloula.photoswipe.domain.model

/**
 * Bilan affiché en fin de session, avant la confirmation système (SPEC §3.5).
 *
 * Les marqués et l'espace libéré décrivent ce que la confirmation enverra à la corbeille :
 * **toutes** les demandes en attente, y compris celles d'une session précédente non confirmée
 * (SPEC §3.4, ADR 0011). Les gardés et les favoris, eux, sont ceux du tri d'aujourd'hui (ADR 0024).
 *
 * @property keptCount médias gardés aujourd'hui, sans les favoris.
 * @property markedForDeletionCount médias marqués pour suppression et pas encore confirmés.
 * @property favoriteCount médias mis en favori aujourd'hui.
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
