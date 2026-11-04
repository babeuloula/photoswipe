package fr.babeuloula.photoswipe.domain.fixtures

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class FixedClockTest {
    @Test
    fun `stays on the same instant until the test moves it`() {
        val clock = FixedClock(Instant.parse("2026-07-15T10:00:00Z"))

        assertEquals(Instant.parse("2026-07-15T10:00:00Z"), clock.instant())
        assertEquals(Instant.parse("2026-07-15T10:00:00Z"), clock.instant())
    }

    @Test
    fun `reads local date and time in its zone`() {
        val clock = FixedClock.at(LocalDateTime.of(2026, 12, 31, 23, 30))

        assertEquals(FixedClock.PARIS, clock.zone)
        assertEquals(LocalDateTime.of(2026, 12, 31, 23, 30), LocalDateTime.now(clock))
    }

    @Test
    fun `crosses midnight when advanced`() {
        val clock = FixedClock.at(LocalDateTime.of(2026, 12, 31, 23, 30))

        clock.advanceBy(Duration.ofMinutes(45))

        assertEquals(LocalDate.of(2027, 1, 1), LocalDate.now(clock))
    }

    @Test
    fun `can be set to a local date and time or an instant`() {
        val clock = FixedClock(Instant.EPOCH)

        clock.setTo(LocalDateTime.of(2028, 2, 29, 8, 0))
        assertEquals(LocalDateTime.of(2028, 2, 29, 8, 0), LocalDateTime.now(clock))

        clock.setTo(Instant.parse("2026-01-01T00:00:00Z"))
        assertEquals(Instant.parse("2026-01-01T00:00:00Z"), clock.instant())
    }

    @Test
    fun `returns an independent clock for another zone`() {
        val clock = FixedClock(Instant.parse("2026-07-15T10:00:00Z"))
        val tokyo = clock.withZone(ZoneId.of("Asia/Tokyo"))

        clock.advanceBy(Duration.ofHours(1))

        assertEquals(ZoneId.of("Asia/Tokyo"), tokyo.zone)
        assertEquals(Instant.parse("2026-07-15T10:00:00Z"), tokyo.instant())
        assertEquals(FixedClock.PARIS, clock.zone)
    }
}
