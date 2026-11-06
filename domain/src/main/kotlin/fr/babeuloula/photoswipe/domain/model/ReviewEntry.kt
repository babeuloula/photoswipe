package fr.babeuloula.photoswipe.domain.model

import java.time.Instant

/**
 * Décision enregistrée pour un couple (média, année de revue), telle que la stocke
 * `ReviewRepository` (SPEC §3.3 et §6).
 *
 * L'absence d'entrée signifie que le média est encore à trier pour cette année.
 *
 * @property mediaId média concerné.
 * @property reviewYear année du jour cible pendant lequel la décision a été prise (SPEC §3.3).
 * @property decision décision prise.
 * @property favoritePending favori système demandé, pas encore appliqué (SPEC §3.5). Un favori
 * est toujours gardé (SPEC §2) : il n'existe qu'avec la décision [ReviewDecision.KEPT].
 * @property sizeBytes taille du média au moment de la décision, pour l'espace libéré estimé.
 * @property decidedAt instant de la décision, qui sert à ordonner l'undo de la session.
 * @property reopened jour rouvert pour être retrié (PS-108, ADR 0026) : le média redevient à trier,
 * mais sa décision reste lisible, pour que la pile la montre, jusqu'à ce qu'une nouvelle décision la
 * remplace. Une entrée rouverte n'est plus une demande de corbeille en attente. Jamais [ReviewDecision.TRASHED].
 */
data class ReviewEntry(
    val mediaId: MediaId,
    val reviewYear: Int,
    val decision: ReviewDecision,
    val sizeBytes: Long,
    val decidedAt: Instant,
    val favoritePending: Boolean = false,
    val reopened: Boolean = false,
) {
    init {
        require(sizeBytes >= 0) { "sizeBytes must not be negative: $sizeBytes" }
        require(!favoritePending || decision == ReviewDecision.KEPT) {
            "a pending favorite must be kept, not $decision"
        }
        require(!reopened || decision != ReviewDecision.TRASHED) { "a trashed entry cannot be reopened" }
    }
}

/** Vrai si cette entrée retire son média de la pile : une décision prise et non rouverte (ADR 0026). */
val ReviewEntry.isSorted: Boolean get() = !reopened
