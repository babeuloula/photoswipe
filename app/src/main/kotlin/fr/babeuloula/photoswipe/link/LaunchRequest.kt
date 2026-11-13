package fr.babeuloula.photoswipe.link

import java.time.LocalDate

/**
 * Écran demandé au lancement de l'application, par la notification du rappel, un lien profond
 * (`photoswipe://…`), un raccourci de l'icône ou la tuile des réglages rapides (ADR 0038).
 *
 * La demande attend que le tutoriel et l'accès aux médias soient passés, comme un lancement
 * ordinaire ; elle est ensuite résolue en écran par [LaunchRequestResolver].
 */
sealed interface LaunchRequest {
    /** L'accueil : lien invalide ou incomplet. */
    data object Home : LaunchRequest

    /** La pile du jour courant (`photoswipe://today`), l'accueil s'il n'y reste rien à trier. */
    data object TodayDeck : LaunchRequest

    /** Le résumé du tri d'aujourd'hui (`photoswipe://summary`). */
    data object Summary : LaunchRequest

    /**
     * La vue d'ensemble du jour [day] (`photoswipe://day/2026-07-15`), l'accueil s'il n'a aucun souvenir.
     *
     * @property day jour dont on veut voir les souvenirs.
     */
    data class DayOverview(
        val day: LocalDate,
    ) : LaunchRequest

    /**
     * La pile du jour [day], sans condition : celle que la notification du rappel ouvre.
     *
     * @property day jour de la pile.
     */
    data class Deck(
        val day: LocalDate,
    ) : LaunchRequest
}
