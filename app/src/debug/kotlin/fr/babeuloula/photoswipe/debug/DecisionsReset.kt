package fr.babeuloula.photoswipe.debug

/**
 * Remet la base des décisions à zéro (outils développeur, SPEC §10) : décisions, marquages,
 * favoris en attente et historique d'annulation. Les médias de la galerie ne sont pas touchés.
 */
fun interface DecisionsReset {
    /** Efface toutes les décisions et tout l'historique d'annulation. */
    suspend fun resetAll()
}
