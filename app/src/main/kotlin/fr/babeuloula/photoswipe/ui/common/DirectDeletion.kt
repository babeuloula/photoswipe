package fr.babeuloula.photoswipe.ui.common

import fr.babeuloula.photoswipe.domain.port.MediaActionRequest

/**
 * Suppression que PhotoSwipe applique lui-même, sans dialogue d'Android : sur Android 10, qui n'a ni
 * corbeille ni demande groupée, la suppression des médias marqués est directe et définitive (PS-184,
 * ADR 0042). Comme [MediaActionIntents], seul l'assemblage DI (`di`) connaît l'implémentation de
 * `:data`.
 */
interface DirectDeletion {
    /** Vrai si [request] se supprime ici, sans dialogue d'Android. */
    fun handles(request: MediaActionRequest): Boolean

    /**
     * Supprime pour de bon les médias de [request] ; renvoie la demande réduite à ceux supprimés,
     * `null` si aucun ne l'a été (ils restent marqués).
     */
    suspend fun delete(request: MediaActionRequest): MediaActionRequest?
}
