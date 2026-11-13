package fr.babeuloula.photoswipe.domain.model

/**
 * Place d'une photo dans un groupe de photos proches, prises à la suite (PS-140, ADR 0035) : « 2 sur 3 ».
 *
 * @property position rang de la photo dans le groupe, dans l'ordre de prise de vue, à partir de 1.
 * @property size nombre de photos du groupe, au moins deux.
 * @property previousUri adresse de la photo prise juste avant dans le groupe, pour la comparer en
 * maintenant le doigt sur la carte (PS-160) ; `null` pour la première.
 */
data class SimilarGroup(
    val position: Int,
    val size: Int,
    val previousUri: String? = null,
) {
    init {
        require(size >= 2) { "a group of similar photos has at least two photos: $size" }
        require(position in 1..size) { "position must be between 1 and $size: $position" }
    }
}
