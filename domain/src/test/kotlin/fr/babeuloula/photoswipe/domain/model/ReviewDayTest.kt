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

    @Test
    fun `counts every memory of the day by default as left to sort`() {
        assertEquals(3, ReviewDay(LocalDate.of(2026, 7, 15), remainingCount = 3).memoryCount)
    }

    @Test
    fun `rejects fewer memories than media left to sort`() {
        assertThrows<IllegalArgumentException> {
            ReviewDay(
                LocalDate.of(2026, 7, 15),
                remainingCount = 2,
                memoryCount = 1,
            )
        }
    }
}
