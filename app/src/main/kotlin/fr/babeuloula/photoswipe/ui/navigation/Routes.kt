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
 * @property firstMediaId média montré en premier : celui touché dans le résumé (pile des marqués) ou
 * dans la vue d'ensemble du jour (PS-139) ;
 * `null` pour commencer par le plus récemment marqué.
 */
@Serializable
data class DeckRoute(
    val epochDay: Long? = null,
    val firstMediaId: Long? = null,
) {
    /** Jour cible, `null` pour la pile des marqués. */
    val day: LocalDate? get() = epochDay?.let(LocalDate::ofEpochDay)

    /** Vrai pour la pile des médias marqués pour suppression. */
    val isMarked: Boolean get() = epochDay == null

    companion object {
        /** Route de la pile du jour [day], en commençant par [first] s'il reste à trier. */
        fun of(
            day: LocalDate,
            first: MediaId? = null,
        ): DeckRoute = DeckRoute(day.toEpochDay(), first?.value)

        /** Route de la pile des médias marqués, en commençant par [first] s'il est donné. */
        fun marked(first: MediaId? = null): DeckRoute = DeckRoute(firstMediaId = first?.value)
    }
}

/**
 * Vue d'ensemble d'un jour (PS-139) : tous ses souvenirs, triés ou non, rangés par année.
 *
 * @property epochDay jour cible en jours depuis le 1er janvier 1970.
 */
@Serializable
data class DayOverviewRoute(
    val epochDay: Long,
) {
    /** Jour cible. */
    val day: LocalDate get() = LocalDate.ofEpochDay(epochDay)

    companion object {
        /** Route de la vue d'ensemble du jour [day]. */
        fun of(day: LocalDate): DayOverviewRoute = DayOverviewRoute(day.toEpochDay())
    }
}

/** Résumé avant confirmation (SPEC §3.5) : il lit le tri du jour lui-même (ADR 0024). */
@Serializable
data object SummaryRoute

/** Corbeille (PS-168) : les suppressions de PhotoSwipe encore restaurables, ouverte depuis le résumé. */
@Serializable
data object TrashRoute

/** Réglages (SPEC §4.5). */
@Serializable
data object SettingsRoute

/** Progression (PS-153) : statistiques d'utilisation, ouverte depuis l'accueil ou les réglages. */
@Serializable
data object ProgressRoute

/** Licences de l'application et de ses bibliothèques (« À propos » des réglages). */
@Serializable
data object LicensesRoute

/** Tutoriel revu depuis les réglages (SPEC §4) : le même parcours, sans changer l'état « tutoriel vu ». */
@Serializable
data object TutorialRoute
