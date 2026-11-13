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
    /** Garde [marked] : il quitte les médias à supprimer, pour chacune de ses années (`KeepMedia` lève les autres). */
    suspend fun keep(marked: MarkedMedia): List<SessionDecision> = keepMedia(marked.media, marked.reviewDays.first())

    /** Laisse [marked] marqué pour suppression (la décision est réécrite, pour pouvoir l'annuler). */
    suspend fun markForDeletion(marked: MarkedMedia): List<SessionDecision> =
        marked.reviewDays.flatMap { markForDeletion.withCompanion(marked.media, it) }

    /** Garde [marked] en favori : le favori sur son année la plus récente, gardé pour les autres. */
    suspend fun favorite(marked: MarkedMedia): List<SessionDecision> =
        favoriteMedia(marked.media, marked.reviewDays.first())
}
