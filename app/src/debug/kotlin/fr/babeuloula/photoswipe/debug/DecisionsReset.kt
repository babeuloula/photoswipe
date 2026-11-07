package fr.babeuloula.photoswipe.debug

/**
 * Remet la base des décisions à zéro (outils développeur, SPEC §10) : décisions, marquages,
 * favoris en attente, historique d'annulation et statistiques d'utilisation (PS-122). Les médias
 * de la galerie ne sont pas touchés.
 */
fun interface DecisionsReset {
    /** Efface toutes les décisions et tout l'historique d'annulation. */
    suspend fun resetAll()
}
