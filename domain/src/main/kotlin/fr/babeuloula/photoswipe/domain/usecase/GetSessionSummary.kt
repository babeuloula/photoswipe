package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.DecisionRecord
import fr.babeuloula.photoswipe.domain.model.ReviewDecision
import fr.babeuloula.photoswipe.domain.model.SessionSummary
import fr.babeuloula.photoswipe.domain.port.DecisionJournal
import fr.babeuloula.photoswipe.domain.port.DeletionHistory
import fr.babeuloula.photoswipe.domain.port.ReviewRepository
import fr.babeuloula.photoswipe.domain.service.ReviewWindowProvider
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate

/**
 * Résumé affiché avant de confirmer (SPEC §3.5) : gardés, favoris, marqués, espace libéré estimé.
 *
 * Gardés et favoris : ce qui a été trié **aujourd'hui** (ADR 0024), toutes piles confondues
 * (aujourd'hui et jours rattrapés), lu dans le journal des décisions : il survit à la fermeture de
 * l'app, et une décision annulée en sort. Un favori compte même une fois appliqué à la galerie.
 *
 * Marqués et espace libéré : toutes les demandes de corbeille en attente en base, quel que soit le
 * jour du tri, car ce sont elles que la confirmation enverra (ADR 0011). Un média marqué pour deux
 * années de revue ne compte qu'une fois, comme dans la demande système ; l'espace libéré additionne
 * la taille de chaque média mémorisée au moment de sa décision.
 *
 * Supprimés aujourd'hui : les suppressions confirmées aujourd'hui, lues dans l'historique
 * (ADR 0034), un média ne comptant qu'une fois ; elles ne sont plus en base une fois confirmées.
 *
 * Résultat ponctuel (`suspend`), comme le paquet : l'écran de résumé le lit une fois.
 *
 * @param journal journal des décisions des jours de la fenêtre (ADR 0019).
 * @param history suppressions confirmées des derniers jours.
 * @param window jours de la fenêtre, dont le journal garde les décisions.
 * @param clock horloge de l'appareil : elle dit quel jour est « aujourd'hui ».
 */
class GetSessionSummary(
    private val reviewRepository: ReviewRepository,
    private val journal: DecisionJournal,
    private val history: DeletionHistory,
    private val window: ReviewWindowProvider,
    private val clock: Clock,
) {
    /** Résumé du tri et des suppressions d'aujourd'hui, et des demandes de corbeille en attente. */
    suspend operator fun invoke(): SessionSummary {
        val marked = reviewRepository.entriesMarkedForDeletion().distinctBy { it.mediaId }
        val sortedToday = decisionsMadeToday()
        val today = LocalDate.now(clock)
        val trashedToday =
            history
                .observe()
                .first()
                .filter { it.confirmedAt.atZone(clock.zone).toLocalDate() == today }
                .distinctBy { it.mediaId }
        return SessionSummary(
            keptCount = sortedToday.count { !it.made.favoritePending },
            markedForDeletionCount = marked.size,
            favoriteCount = sortedToday.count { it.made.favoritePending },
            estimatedFreedBytes = marked.sumOf { it.sizeBytes },
            trashedTodayCount = trashedToday.size,
            trashedTodayBytes = trashedToday.sumOf { it.sizeBytes },
        )
    }

    /** Décisions « garder » ou « favori » prises aujourd'hui, dans toutes les piles de la fenêtre. */
    private suspend fun decisionsMadeToday(): List<DecisionRecord> {
        val today = LocalDate.now(clock)
        return window
            .days()
            .flatMap { day -> journal.recordsFor(day) }
            .filter { it.made.decision == ReviewDecision.KEPT }
            .filter {
                it.made.decidedAt
                    .atZone(clock.zone)
                    .toLocalDate() == today
            }
    }
}
