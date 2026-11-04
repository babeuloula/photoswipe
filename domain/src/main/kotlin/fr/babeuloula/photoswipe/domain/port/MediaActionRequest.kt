package fr.babeuloula.photoswipe.domain.port

import fr.babeuloula.photoswipe.domain.model.MediaId

/**
 * Demande de confirmation système (corbeille ou favori), opaque pour le domaine (SPEC §3.5).
 *
 * `:data` l'implémente autour de l'objet Android à lancer (un `IntentSender`) ; l'écran le
 * récupère pour afficher le dialogue. Le domaine n'en connaît que les médias couverts, pour
 * enregistrer le résultat de chaque demande.
 */
interface MediaActionRequest {
    /** Médias couverts par cette demande. */
    val mediaIds: Set<MediaId>
}
