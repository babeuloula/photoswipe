package fr.babeuloula.photoswipe.domain.port

import fr.babeuloula.photoswipe.domain.model.MediaItem

/**
 * Prépare les demandes de confirmation système de fin de session (SPEC §3.5). Implémenté dans
 * `:data` avec `MediaStore.createTrashRequest` et `MediaStore.createFavoriteRequest`.
 *
 * L'implémentation découpe les médias en lots (par exemple 500) pour rester sous la taille
 * maximale d'un Intent : chaque lot donne une demande. Une liste vide donne une liste vide.
 */
interface MediaActionRequestFactory {
    /** Demandes d'envoi de [media] à la corbeille système, restaurable environ 30 jours. */
    fun trashRequests(media: List<MediaItem>): List<MediaActionRequest>

    /** Demandes d'ajout de [media] aux favoris système. */
    fun favoriteRequests(media: List<MediaItem>): List<MediaActionRequest>
}
