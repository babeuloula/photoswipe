package fr.babeuloula.photoswipe.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class ReviewDayTest {
    @Test
    fun `review year is the year of the target day`() {
        val day = ReviewDay(date = LocalDate.of(2026, 12, 28), remainingCount = 3)

        assertEquals(2026, day.reviewYear)
    }

    @Test
    fun `accepts a day with nothing left to review`() {
        assertDoesNotThrow { ReviewDay(date = LocalDate.of(2026, 11, 4), remainingCount = 0) }
    }

    @Test
    fun `rejects a negative remaining count`() {
        assertThrows<IllegalArgumentException> {
            ReviewDay(date = LocalDate.of(2026, 11, 4), remainingCount = -1)
        }
    }
}
