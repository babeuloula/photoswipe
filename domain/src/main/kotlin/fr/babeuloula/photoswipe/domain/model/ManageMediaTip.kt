package fr.babeuloula.photoswipe.domain.model

/**
 * Conseil de l'accueil qui propose l'accès « Gestion des médias » d'Android, pour ne plus valider
 * un dialogue système à chaque suppression ou favori (SPEC §3.5, ADR 0023).
 */
enum class ManageMediaTip {
    /** Jamais proposé : aucune suppression ni aucun favori n'est encore passé par un dialogue d'Android. */
    NOT_OFFERED,

    /** À proposer : un dialogue d'Android a déjà été validé. */
    OFFERED,

    /** Fermé par l'utilisateur : il ne revient plus. */
    DISMISSED,

    ;

    companion object {
        /** État d'une installation neuve. */
        val DEFAULT: ManageMediaTip = NOT_OFFERED
    }
}
