package fr.babeuloula.photoswipe.domain.fixtures

import fr.babeuloula.photoswipe.domain.model.ConfirmedDeletion
import fr.babeuloula.photoswipe.domain.model.DecisionRecord
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.ReviewDecision
import fr.babeuloula.photoswipe.domain.model.ReviewEntry
import java.time.Instant
import java.time.LocalDate

/** Année de revue par défaut de [aReviewEntry]. */
const val DEFAULT_REVIEW_YEAR: Int = 2026

/** Instant de décision par défaut de [aReviewEntry] : fixe, pour des tests déterministes. */
val DEFAULT_DECIDED_AT: Instant = Instant.parse("2026-07-15T18:00:00Z")

/**
 * Construit une [ReviewEntry] valide, dont le test ne précise que ce qui compte pour lui.
 *
 * Par défaut : le média 1 gardé pendant l'année de revue [DEFAULT_REVIEW_YEAR], sans favori en
 * attente. Un favori en attente impose la décision [ReviewDecision.KEPT] (invariant du modèle).
 */
fun aReviewEntry(
    mediaId: Long = 1,
    reviewYear: Int = DEFAULT_REVIEW_YEAR,
    decision: ReviewDecision = ReviewDecision.KEPT,
    sizeBytes: Long = DEFAULT_SIZE_BYTES,
    decidedAt: Instant = DEFAULT_DECIDED_AT,
    favoritePending: Boolean = false,
    reopened: Boolean = false,
): ReviewEntry =
    ReviewEntry(
        mediaId = MediaId(mediaId),
        reviewYear = reviewYear,
        decision = decision,
        sizeBytes = sizeBytes,
        decidedAt = decidedAt,
        favoritePending = favoritePending,
        reopened = reopened,
    )

/**
 * Construit un [DecisionRecord] valide : par défaut, la ligne 1 du journal, qui garde le média
 * [mediaId] le 15 juillet de l'année de revue [DEFAULT_REVIEW_YEAR], sans entrée remplacée.
 */
fun aDecisionRecord(
    mediaId: Long = 1,
    day: LocalDate = LocalDate.of(DEFAULT_REVIEW_YEAR, 7, 15),
    id: Long = 1,
    decision: ReviewDecision = ReviewDecision.KEPT,
    favoritePending: Boolean = false,
    previous: ReviewEntry? = null,
): DecisionRecord =
    DecisionRecord(
        id = id,
        day = day,
        made = aReviewEntry(mediaId, day.year, decision, favoritePending = favoritePending),
        previous = previous,
    )

/**
 * Construit une [ConfirmedDeletion] valide : par défaut, le média [mediaId] du 15 juillet de
 * l'année de revue [DEFAULT_REVIEW_YEAR], confirmé à l'instant [DEFAULT_DECIDED_AT].
 */
fun aConfirmedDeletion(
    mediaId: Long = 1,
    day: LocalDate? = LocalDate.of(DEFAULT_REVIEW_YEAR, 7, 15),
    sizeBytes: Long = DEFAULT_SIZE_BYTES,
    confirmedAt: Instant = DEFAULT_DECIDED_AT,
): ConfirmedDeletion = ConfirmedDeletion(MediaId(mediaId), day, sizeBytes, confirmedAt)
