package fr.babeuloula.photoswipe.domain.model

import java.time.LocalDate

/**
 * Ce que montre le widget « Ce jour-là » de l'écran d'accueil (PS-170) : les souvenirs
 * d'aujourd'hui qui restent à trier, ou l'état qui en tient lieu.
 */
sealed interface TodayWidget {
    /** Sans accès aux médias : le widget invite à ouvrir l'application. */
    data object NoAccess : TodayWidget

    /**
     * Des souvenirs du jour restent à trier : un appui ouvre la pile du jour.
     *
     * @property date jour affiché (aujourd'hui).
     * @property remainingCount nombre de souvenirs qui restent à trier (« 23 à trier »).
     * @property years années des souvenirs du jour, de la plus ancienne à la plus récente.
     * @property preview souvenir à montrer en vignette, à trier en priorité ; `null` s'il n'y en a pas.
     */
    data class ToSort(
        val date: LocalDate,
        val remainingCount: Int,
        val years: IntRange?,
        val preview: MediaItem?,
    ) : TodayWidget {
        init {
            require(remainingCount > 0) { "remainingCount must be positive: $remainingCount" }
        }
    }

    /**
     * Tous les souvenirs du jour sont triés (ou ont été supprimés) : un appui ouvre l'accueil.
     *
     * @property date jour affiché (aujourd'hui).
     */
    data class AllSorted(
        val date: LocalDate,
    ) : TodayWidget

    /**
     * Aucun souvenir n'a été pris ce jour-là les années passées : un appui ouvre l'accueil.
     *
     * @property date jour affiché (aujourd'hui).
     */
    data class NoMemories(
        val date: LocalDate,
    ) : TodayWidget
}
