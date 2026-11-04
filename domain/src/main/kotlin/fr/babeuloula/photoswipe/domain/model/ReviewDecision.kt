package fr.babeuloula.photoswipe.domain.model

/**
 * Décision prise sur un couple (média, année de revue) (SPEC §3.3).
 *
 * L'absence de décision signifie que le média est encore à trier. Le favori n'est pas une
 * décision : c'est une demande en attente stockée à part (`favorite_pending`, SPEC §6).
 */
enum class ReviewDecision {
    /** Gardé : n'est plus proposé cette année pour ce jour, mais redevient à trier l'année suivante. */
    KEPT,

    /** Marqué pour suppression : persiste tant que la suppression n'est ni confirmée ni annulée. */
    MARKED_FOR_DELETION,

    /** Envoyé à la corbeille système après confirmation ; la ligne est purgée avec les orphelins. */
    TRASHED,
}
