package fr.babeuloula.photoswipe.domain.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class SessionSummaryTest {
    @Test
    fun `accepts an empty session`() {
        assertDoesNotThrow { aSummary() }
    }

    @Test
    fun `rejects a negative kept count`() {
        assertThrows<IllegalArgumentException> { aSummary(keptCount = -1) }
    }

    @Test
    fun `rejects a negative marked for deletion count`() {
        assertThrows<IllegalArgumentException> { aSummary(markedForDeletionCount = -1) }
    }

    @Test
    fun `rejects a negative favorite count`() {
        assertThrows<IllegalArgumentException> { aSummary(favoriteCount = -1) }
    }

    @Test
    fun `rejects negative estimated freed bytes`() {
        assertThrows<IllegalArgumentException> { aSummary(estimatedFreedBytes = -1) }
    }

    private fun aSummary(
        keptCount: Int = 0,
        markedForDeletionCount: Int = 0,
        favoriteCount: Int = 0,
        estimatedFreedBytes: Long = 0,
    ) = SessionSummary(
        keptCount = keptCount,
        markedForDeletionCount = markedForDeletionCount,
        favoriteCount = favoriteCount,
        estimatedFreedBytes = estimatedFreedBytes,
    )
}
