package fr.babeuloula.photoswipe.domain.port

import java.time.LocalDate

/**
 * Notification du rappel quotidien (SPEC §3.6). Implémentée dans `:app`, qui connaît ses textes,
 * son canal de notification et l'écran à ouvrir.
 */
interface ReminderNotifier {
    /**
     * Annonce [toSortCount] médias à trier sur la fenêtre de revue (toujours positif) ; un appui
     * ouvre la pile du jour [dayToSort], le plus récent qui en a encore.
     */
    fun notify(
        toSortCount: Int,
        dayToSort: LocalDate,
    )
}
