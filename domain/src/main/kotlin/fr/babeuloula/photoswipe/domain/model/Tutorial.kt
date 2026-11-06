package fr.babeuloula.photoswipe.domain.model

/**
 * Tutoriel de première utilisation (SPEC §4) : sa version courante, comparée à la version déjà vue
 * pour le proposer une seule fois, puis de nouveau quand son contenu change fortement.
 */
object Tutorial {
    /**
     * Version du contenu du tutoriel. À incrémenter quand son contenu change fortement (nouvelle
     * fonctionnalité importante) pour le proposer de nouveau aux utilisateurs qui l'ont déjà vu.
     */
    const val CURRENT_VERSION: Int = 1

    /** Version vue d'une installation neuve : aucune. */
    const val NEVER_SEEN: Int = 0
}
