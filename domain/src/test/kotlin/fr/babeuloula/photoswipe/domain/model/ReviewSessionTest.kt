package fr.babeuloula.photoswipe.domain.model

import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class ReviewSessionTest {
    private val day = LocalDate.of(2026, 7, 15)

    @Test
    fun `starts empty with nothing to undo`() {
        val session = ReviewSession()

        assertTrue(session.isEmpty)
        assertNull(session.lastDecision)
    }

    @Test
    fun `puts the most recent decision on top of the stack`() {
        val first = SessionDecision(aMediaItem(id = 1), day)
        val second = SessionDecision(aMediaItem(id = 2), day)

        val session = ReviewSession().record(first).record(second)

        assertFalse(session.isEmpty)
        assertEquals(second, session.lastDecision)
        assertEquals(first, session.withoutLastDecision().lastDecision)
    }

    @Test
    fun `stays empty when removing the last decision of an empty session`() {
        assertEquals(ReviewSession(), ReviewSession().withoutLastDecision())
    }

    @Test
    fun `gives the year of the target day as review year`() {
        assertEquals(2025, SessionDecision(aMediaItem(id = 1), LocalDate.of(2025, 12, 28)).reviewYear)
    }

    @Test
    fun `rejects a previous entry of another media or another review year`() {
        assertThrows<IllegalArgumentException> {
            SessionDecision(aMediaItem(id = 1), day, previous = aReviewEntry(mediaId = 2, reviewYear = 2026))
        }
        assertThrows<IllegalArgumentException> {
            SessionDecision(aMediaItem(id = 1), day, previous = aReviewEntry(mediaId = 1, reviewYear = 2025))
        }
    }
}
