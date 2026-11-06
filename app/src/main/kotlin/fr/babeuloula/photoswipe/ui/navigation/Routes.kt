package fr.babeuloula.photoswipe.ui.navigation

import fr.babeuloula.photoswipe.domain.model.MediaId
import kotlinx.serialization.Serializable
import java.time.LocalDate

/** Accueil : le jour courant et les jours à rattraper (SPEC §4.2). Destination de départ. */
@Serializable
data object HomeRoute

/**
 * Pile de swipe (SPEC §4.3) : celle d'un jour cible, ou celle des médias marqués pour suppression,
 * ouverte depuis le résumé (PS-112).
 *
 * @property epochDay jour cible en jours depuis le 1er janvier 1970 (les routes typées ne
 * transportent que des types simples) ; `null` pour la pile des marqués.
 * @property firstMarkedMediaId pile des marqués : média touché dans le résumé, montré en premier ;
 * `null` pour commencer par le plus récemment marqué.
 */
@Serializable
data class DeckRoute(
    val epochDay: Long? = null,
    val firstMarkedMediaId: Long? = null,
) {
    /** Jour cible, `null` pour la pile des marqués. */
    val day: LocalDate? get() = epochDay?.let(LocalDate::ofEpochDay)

    /** Vrai pour la pile des médias marqués pour suppression. */
    val isMarked: Boolean get() = epochDay == null

    companion object {
        /** Route de la pile du jour [day]. */
        fun of(day: LocalDate): DeckRoute = DeckRoute(day.toEpochDay())

        /** Route de la pile des médias marqués, en commençant par [first] s'il est donné. */
        fun marked(first: MediaId? = null): DeckRoute = DeckRoute(firstMarkedMediaId = first?.value)
    }
}

/** Résumé avant confirmation (SPEC §3.5) : il lit le tri du jour lui-même (ADR 0024). */
@Serializable
data object SummaryRoute

/** Réglages (SPEC §4.5). */
@Serializable
data object SettingsRoute

/** Licences de l'application et de ses bibliothèques (« À propos » des réglages). */
@Serializable
data object LicensesRoute
