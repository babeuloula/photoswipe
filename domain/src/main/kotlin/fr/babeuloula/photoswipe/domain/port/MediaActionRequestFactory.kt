package fr.babeuloula.photoswipe.domain.port

import fr.babeuloula.photoswipe.domain.model.MediaItem

/**
 * Prépare les demandes de confirmation système de fin de session (SPEC §3.5) et celles de la page
 * « Corbeille » (PS-168). Implémenté dans `:data` avec `MediaStore.createTrashRequest` (envoi ou
 * restauration), `MediaStore.createFavoriteRequest` (ajout ou retrait) et
 * `MediaStore.createDeleteRequest`.
 *
 * L'implémentation découpe les médias en lots (par exemple 500) pour rester sous la taille
 * maximale d'un Intent : chaque lot donne une demande. Une liste vide donne une liste vide.
 */
interface MediaActionRequestFactory {
    /** Demandes d'envoi de [media] à la corbeille système, restaurable environ 30 jours. */
    fun trashRequests(media: List<MediaItem>): List<MediaActionRequest>

    /** Demandes d'ajout de [media] aux favoris système. */
    fun favoriteRequests(media: List<MediaItem>): List<MediaActionRequest>

    /** Demandes de retrait de [media] des favoris système (PS-124). */
    fun unfavoriteRequests(media: List<MediaItem>): List<MediaActionRequest>

    /** Demandes de sortie de [media] de la corbeille système : ils reviennent dans la galerie (PS-168). */
    fun restoreRequests(media: List<MediaItem>): List<MediaActionRequest>

    /** Demandes de suppression définitive de [media], sans passer par la corbeille (PS-168). */
    fun deleteRequests(media: List<MediaItem>): List<MediaActionRequest>
}
