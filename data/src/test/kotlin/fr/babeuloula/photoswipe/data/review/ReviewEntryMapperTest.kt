package fr.babeuloula.photoswipe.data.review

import fr.babeuloula.photoswipe.data.database.ReviewEntryEntity
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.KEPT
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.MARKED_FOR_DELETION
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class ReviewEntryMapperTest {
    @Test
    fun `maps every field of a domain entry to a row and back`() {
        val entry =
            aReviewEntry(
                mediaId = 42,
                reviewYear = 2025,
                decision = KEPT,
                sizeBytes = 3_500_000,
                decidedAt = Instant.parse("2025-07-15T18:30:12.345Z"),
                favoritePending = true,
            )

        val entity = entry.toEntity()

        assertEquals(
            ReviewEntryEntity(
                mediaId = 42,
                reviewYear = 2025,
                decision = KEPT,
                favoritePending = true,
                sizeBytes = 3_500_000,
                decidedAt = Instant.parse("2025-07-15T18:30:12.345Z").toEpochMilli(),
            ),
            entity,
        )
        assertEquals(entry, entity.toDomain())
    }

    @Test
    fun `keeps the decision instant to the millisecond`() {
        val entry =
            aReviewEntry(decision = MARKED_FOR_DELETION, decidedAt = Instant.parse("2026-07-15T18:00:00.123456789Z"))

        assertEquals(Instant.parse("2026-07-15T18:00:00.123Z"), entry.toEntity().toDomain().decidedAt)
    }
}
