package fr.babeuloula.photoswipe.data.reminder

import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/** Délai jusqu'au prochain rappel, sur l'horloge fixe du domaine (fuseau de Paris). */
class ReminderDelayTest {
    @Test
    fun `waits until the reminder time later the same day`() {
        val clock = FixedClock.at(LocalDateTime.of(2026, 7, 15, 9, 30))

        assertEquals(Duration.ofHours(8).plusMinutes(30), delayUntilNext(LocalTime.of(18, 0), clock))
    }

    @Test
    fun `waits until the next day once the reminder time has passed`() {
        val clock = FixedClock.at(LocalDateTime.of(2026, 7, 15, 20, 0))

        assertEquals(Duration.ofHours(22), delayUntilNext(LocalTime.of(18, 0), clock))
    }

    @Test
    fun `waits a whole day when it is exactly the reminder time`() {
        val clock = FixedClock.at(LocalDateTime.of(2026, 7, 15, 18, 0))

        assertEquals(Duration.ofDays(1), delayUntilNext(LocalTime.of(18, 0), clock))
    }

    @Test
    fun `follows the switch to summer time`() {
        // Nuit du 28 au 29 mars 2026 à Paris : 2 h devient 3 h, la journée dure 23 heures.
        val clock = FixedClock.at(LocalDateTime.of(2026, 3, 28, 20, 0))

        assertEquals(Duration.ofHours(21), delayUntilNext(LocalTime.of(18, 0), clock))
    }
}
