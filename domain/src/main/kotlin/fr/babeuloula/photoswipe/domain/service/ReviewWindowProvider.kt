package fr.babeuloula.photoswipe.domain.service

import java.time.Clock
import java.time.LocalDate

/**
 * Fournit la fenêtre de revue : aujourd'hui et les jours précédents proposés au rattrapage
 * (SPEC §3.1). Par défaut, 7 jours : aujourd'hui et les 6 jours qui le précèdent.
 *
 * Les jours sont des dates de calendrier : la fenêtre passe naturellement d'une année à l'autre
 * (le 2 janvier 2027, elle remonte jusqu'au 27 décembre 2026) et contient le 29 février les
 * années bissextiles. L'appariement des médias de chaque jour reste l'affaire de [SameDayMatcher].
 *
 * « Aujourd'hui » est lu sur la [Clock] injectée, dans son fuseau, à chaque appel : la fenêtre
 * suit donc le passage à minuit sans recréer le service.
 *
 * @param clock horloge de l'appareil, injectée (SPEC §5.2).
 * @param windowSize nombre de jours de la fenêtre, aujourd'hui compris. Paramètre interne au
 * domaine : le reste de l'application utilise la valeur par défaut.
 */
class ReviewWindowProvider internal constructor(
    private val clock: Clock,
    private val windowSize: Int,
) {
    constructor(clock: Clock) : this(clock, DEFAULT_WINDOW_SIZE)

    init {
        require(windowSize > 0) { "windowSize must be positive: $windowSize" }
    }

    /** Jours de la fenêtre, du plus récent (aujourd'hui) au plus ancien. */
    fun days(): List<LocalDate> {
        val today = LocalDate.now(clock)
        return List(windowSize) { offset -> today.minusDays(offset.toLong()) }
    }

    companion object {
        /** Taille par défaut de la fenêtre : aujourd'hui et les 6 jours précédents. */
        const val DEFAULT_WINDOW_SIZE: Int = 7
    }
}
