package fr.babeuloula.photoswipe.domain.port

/**
 * Dernière application choisie dans la feuille de partage (PS-163), pour proposer « Renvoyer
 * avec … ». On retient l'application, jamais le contact. Implémenté dans `:data` sur DataStore.
 */
interface ShareTargetRepository {
    /** Composant Android de la dernière application choisie (`paquet/classe`), `null` avant un premier partage. */
    suspend fun lastTarget(): String?

    /** Retient [component], application que l'utilisateur vient de choisir. */
    suspend fun saveLastTarget(component: String)
}
