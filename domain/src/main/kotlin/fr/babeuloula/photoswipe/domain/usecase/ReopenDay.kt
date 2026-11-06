package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.ReviewDecision
import fr.babeuloula.photoswipe.domain.port.DecisionJournal
import fr.babeuloula.photoswipe.domain.port.MediaSource
import fr.babeuloula.photoswipe.domain.port.ReviewRepository
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import java.time.Clock
import java.time.LocalDate

/**
 * Rouvre un jour déjà trié pour le retrier (PS-108, ADR 0026) : ses souvenirs encore dans la
 * galerie, gardés, mis en favori ou marqués pour suppression, redeviennent à trier.
 *
 * Les décisions ne sont pas effacées : chaque entrée est seulement marquée rouverte
 * (`ReviewEntry.reopened`), pour que la pile montre la décision précédente sur la carte, jusqu'à ce
 * qu'une nouvelle décision la remplace. Un marquage rouvert n'est plus une demande de corbeille en
 * attente. Les médias déjà envoyés à la corbeille (`TRASHED`) ne sont pas touchés ; un favori déjà
 * appliqué à la galerie le reste. Le journal du jour est oublié : ses décisions ne sont plus à
 * annuler, et ne comptent plus dans le résumé.
 *
 * @param clock horloge de l'appareil : son fuseau sert à trouver les souvenirs du jour.
 */
class ReopenDay(
    private val sameDayMatcher: SameDayMatcher,
    private val mediaSource: MediaSource,
    private val reviewRepository: ReviewRepository,
    private val journal: DecisionJournal,
    private val clock: Clock,
) {
    /**
     * Rouvre le jour [day].
     *
     * @return nombre de médias rouverts, qui reviennent à trier.
     */
    suspend operator fun invoke(day: LocalDate): Int {
        val memoryIds =
            mediaSource
                .mediaTakenIn(sameDayMatcher.rangesFor(day, clock.zone))
                .mapTo(HashSet()) { it.id }
        if (memoryIds.isEmpty()) return 0

        val reopened =
            reviewRepository
                .entriesFor(day.year, memoryIds)
                .filter { it.decision != ReviewDecision.TRASHED && !it.reopened }
        reopened.forEach { reviewRepository.save(it.copy(reopened = true)) }
        journal.forget(memoryIds)
        return reopened.size
    }
}
