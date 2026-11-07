package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakeReminderScheduler
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.LocalTime

class SnoozeReminderTodayTest {
    private val clock = FixedClock.at(LocalDateTime.of(2026, 11, 7, 18, 0, 30))
    private val scheduler = FakeReminderScheduler()
    private val snooze = SnoozeReminderToday(scheduler, clock)

    @Test
    fun `schedules a one-off reminder today at the chosen time`() {
        val result = snooze(LocalTime.of(21, 15))

        assertEquals(SnoozeReminderToday.Result.SCHEDULED, result)
        assertEquals(LocalDateTime.of(2026, 11, 7, 21, 15), scheduler.onceAt)
    }

    @Test
    fun `leaves the daily reminder untouched`() {
        scheduler.schedule(LocalTime.of(18, 0))

        snooze(LocalTime.of(21, 15))

        assertEquals(LocalTime.of(18, 0), scheduler.scheduledTime)
    }

    @Test
    fun `refuses a time already past today`() {
        val result = snooze(LocalTime.of(18, 0))

        assertEquals(SnoozeReminderToday.Result.TIME_PASSED, result)
        assertNull(scheduler.onceAt)
    }

    @Test
    fun `replaces the previous snooze`() {
        snooze(LocalTime.of(20, 0))

        snooze(LocalTime.of(22, 0))

        assertEquals(LocalDateTime.of(2026, 11, 7, 22, 0), scheduler.onceAt)
    }

    @Test
    fun `suggests one hour later, to the minute`() {
        assertEquals(LocalTime.of(19, 0), snooze.suggestedTime())
    }

    @Test
    fun `suggests the last minute of the day late in the evening`() {
        clock.setTo(LocalDateTime.of(2026, 11, 7, 23, 20))

        assertEquals(LocalTime.of(23, 59), snooze.suggestedTime())
    }
}
