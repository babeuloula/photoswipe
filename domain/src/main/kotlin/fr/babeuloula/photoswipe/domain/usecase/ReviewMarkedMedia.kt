package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.SessionDecision

/**
 * Décisions de la pile des marqués (PS-112), appliquées à **tous** les marquages d'un média : un
 * média marqué pour deux années de revue reste sinon marqué pour l'autre, et la confirmation
 * l'enverrait à la corbeille alors qu'il a été gardé.
 *
 * Chaque fonction renvoie une décision par jour de revue, à empiler dans la session : l'undo de la
 * carte les annule toutes.
 */
class ReviewMarkedMedia(
    private val keepMedia: KeepMedia,
    private val markForDeletion: MarkForDeletion,
    private val favoriteMedia: FavoriteMedia,
) {
    /** Garde [marked] : il quitte les médias à supprimer, pour chacune de ses années. */
    suspend fun keep(marked: MarkedMedia): List<SessionDecision> = marked.reviewDays.map { keepMedia(marked.media, it) }

    /** Laisse [marked] marqué pour suppression (la décision est réécrite, pour pouvoir l'annuler). */
    suspend fun markForDeletion(marked: MarkedMedia): List<SessionDecision> =
        marked.reviewDays.map { markForDeletion(marked.media, it) }

    /** Garde [marked] en favori : le favori sur son année la plus récente, gardé pour les autres. */
    suspend fun favorite(marked: MarkedMedia): List<SessionDecision> =
        listOf(favoriteMedia(marked.media, marked.reviewDays.first())) +
            marked.reviewDays.drop(1).map { keepMedia(marked.media, it) }
}
