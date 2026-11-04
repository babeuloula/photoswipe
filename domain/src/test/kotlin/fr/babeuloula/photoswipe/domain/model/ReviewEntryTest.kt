package fr.babeuloula.photoswipe.domain.model

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.Instant

class ReviewEntryTest {
    @Test
    fun `has no pending favorite by default`() {
        assertFalse(entry(ReviewDecision.KEPT).favoritePending)
    }

    @Test
    fun `accepts a pending favorite on a kept media`() {
        assertDoesNotThrow { entry(ReviewDecision.KEPT, favoritePending = true) }
    }

    @Test
    fun `rejects a pending favorite on a media marked for deletion`() {
        assertThrows<IllegalArgumentException> {
            entry(ReviewDecision.MARKED_FOR_DELETION, favoritePending = true)
        }
    }

    @Test
    fun `rejects a pending favorite on a trashed media`() {
        assertThrows<IllegalArgumentException> { entry(ReviewDecision.TRASHED, favoritePending = true) }
    }

    @Test
    fun `rejects a negative size`() {
        assertThrows<IllegalArgumentException> { entry(ReviewDecision.KEPT, sizeBytes = -1) }
    }

    private fun entry(
        decision: ReviewDecision,
        favoritePending: Boolean = false,
        sizeBytes: Long = 1_024,
    ) = ReviewEntry(
        mediaId = MediaId(42),
        reviewYear = 2026,
        decision = decision,
        sizeBytes = sizeBytes,
        decidedAt = Instant.parse("2026-11-04T18:00:00Z"),
        favoritePending = favoritePending,
    )
}
