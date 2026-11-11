package fr.babeuloula.photoswipe.domain.model

/**
 * Place d'une photo dans un groupe de photos proches, prises à la suite (PS-140, ADR 0035) : « 2 sur 3 ».
 *
 * @property position rang de la photo dans le groupe, dans l'ordre de prise de vue, à partir de 1.
 * @property size nombre de photos du groupe, au moins deux.
 */
data class SimilarGroup(
    val position: Int,
    val size: Int,
) {
    init {
        require(size >= 2) { "a group of similar photos has at least two photos: $size" }
        require(position in 1..size) { "position must be between 1 and $size: $position" }
    }
}
