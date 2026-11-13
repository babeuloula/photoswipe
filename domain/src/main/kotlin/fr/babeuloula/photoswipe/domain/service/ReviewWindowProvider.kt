package fr.babeuloula.photoswipe.domain.service

import fr.babeuloula.photoswipe.domain.model.CatchUpWindow
import fr.babeuloula.photoswipe.domain.port.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate

/**
 * Fournit la fenêtre de revue : aujourd'hui et les jours précédents proposés au rattrapage
 * (SPEC §3.1). Sa durée se choisit dans les réglages (PS-169) : 7 jours par défaut, 14 ou 30.
 *
 * Les jours sont des dates de calendrier : la fenêtre passe naturellement d'une année à l'autre
 * (le 2 janvier 2027, sur 7 jours, elle remonte jusqu'au 27 décembre 2026) et contient le
 * 29 février les années bissextiles. L'appariement des médias de chaque jour reste l'affaire de
 * [SameDayMatcher].
 *
 * « Aujourd'hui » est lu sur la [Clock] injectée, dans son fuseau, à chaque appel : la fenêtre
 * suit donc le passage à minuit sans recréer le service. La durée est relue à chaque appel de
 * [days], et [observeDays] suit chaque nouveau choix.
 *
 * @param clock horloge de l'appareil, injectée (SPEC §5.2).
 * @param window durée de la fenêtre, observée.
 */
class ReviewWindowProvider internal constructor(
    private val clock: Clock,
    private val window: Flow<CatchUpWindow>,
) {
    /** Fenêtre qui suit la durée choisie dans les réglages. */
    constructor(clock: Clock, settings: SettingsRepository) : this(clock, settings.observeCatchUpWindow())

    /** Fenêtre de durée fixe [window], une semaine par défaut (tests, outils développeur). */
    constructor(clock: Clock, window: CatchUpWindow = CatchUpWindow.DEFAULT) : this(clock, flowOf(window))

    /** Jours de la fenêtre actuelle, du plus récent (aujourd'hui) au plus ancien. */
    suspend fun days(): List<LocalDate> = days(window.first())

    /** Jours de la fenêtre, recalculés à chaque nouvelle durée choisie. */
    fun observeDays(): Flow<List<LocalDate>> = window.distinctUntilChanged().map { days(it) }

    /** Jours d'une fenêtre de durée [window], du plus récent (aujourd'hui) au plus ancien. */
    fun days(window: CatchUpWindow): List<LocalDate> {
        val today = LocalDate.now(clock)
        return List(window.days) { offset -> today.minusDays(offset.toLong()) }
    }

    companion object {
        /** Taille par défaut de la fenêtre : aujourd'hui et les 6 jours précédents. */
        val DEFAULT_WINDOW_SIZE: Int = CatchUpWindow.DEFAULT.days
    }
}
