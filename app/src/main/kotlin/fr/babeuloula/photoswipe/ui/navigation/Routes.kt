package fr.babeuloula.photoswipe.ui.navigation

import kotlinx.serialization.Serializable
import java.time.LocalDate

/** Accueil : le jour courant et les jours à rattraper (SPEC §4.2). Destination de départ. */
@Serializable
data object HomeRoute

/**
 * Pile de swipe d'un jour cible (SPEC §4.3).
 *
 * @property epochDay jour cible en jours depuis le 1er janvier 1970 : les routes typées ne
 * transportent que des types simples.
 */
@Serializable
data class DeckRoute(
    val epochDay: Long,
) {
    /** Jour cible. */
    val day: LocalDate get() = LocalDate.ofEpochDay(epochDay)

    companion object {
        /** Route de la pile du jour [day]. */
        fun of(day: LocalDate): DeckRoute = DeckRoute(day.toEpochDay())
    }
}

/**
 * Résumé avant confirmation (SPEC §3.5).
 *
 * @property keptCount médias gardés pendant la session qui mène au résumé ; 0 en venant de
 * l'accueil, sans session (ADR 0017).
 */
@Serializable
data class SummaryRoute(
    val keptCount: Int = 0,
)

/** Réglages (SPEC §4.5). */
@Serializable
data object SettingsRoute

/** Licences de l'application et de ses bibliothèques (« À propos » des réglages). */
@Serializable
data object LicensesRoute
