package fr.babeuloula.photoswipe.domain.port

/**
 * Notification du rappel quotidien (SPEC §3.6). Implémentée dans `:app`, qui connaît ses textes,
 * son canal de notification et l'écran à ouvrir.
 */
interface ReminderNotifier {
    /** Annonce [toSortCount] médias à trier sur la fenêtre de revue ; toujours positif. */
    fun notify(toSortCount: Int)
}
