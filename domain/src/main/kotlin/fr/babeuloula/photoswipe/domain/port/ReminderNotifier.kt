package fr.babeuloula.photoswipe.domain.port

import fr.babeuloula.photoswipe.domain.model.MediaItem
import java.time.LocalDate

/**
 * Notification du rappel quotidien (SPEC §3.6). Implémentée dans `:app`, qui connaît ses textes,
 * son canal de notification et l'écran à ouvrir.
 */
interface ReminderNotifier {
    /**
     * Annonce [toSortCount] médias à trier sur la fenêtre de revue (toujours positif) ; un appui
     * ouvre la pile du jour [dayToSort], le plus récent qui en a encore. [years] (années des
     * souvenirs de ce jour) et [preview] (un souvenir à montrer en vignette) enrichissent le texte
     * quand ils sont connus (ADR 0025).
     */
    fun notify(
        toSortCount: Int,
        dayToSort: LocalDate,
        years: IntRange? = null,
        preview: MediaItem? = null,
    )
}
