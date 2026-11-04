package fr.babeuloula.photoswipe.domain.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class ReviewWindowProviderTest {
    @Test
    fun `returns today followed by the six previous days in the middle of the year`() {
        val provider = ReviewWindowProvider(clockAt(LocalDateTime.of(2026, 7, 15, 10, 0)))

        assertEquals(
            listOf(
                LocalDate.of(2026, 7, 15),
                LocalDate.of(2026, 7, 14),
                LocalDate.of(2026, 7, 13),
                LocalDate.of(2026, 7, 12),
                LocalDate.of(2026, 7, 11),
                LocalDate.of(2026, 7, 10),
                LocalDate.of(2026, 7, 9),
            ),
            provider.days(),
        )
    }

    @Test
    fun `spans two years when today is early January`() {
        val provider = ReviewWindowProvider(clockAt(LocalDateTime.of(2027, 1, 2, 8, 0)))

        assertEquals(
            listOf(
                LocalDate.of(2027, 1, 2),
                LocalDate.of(2027, 1, 1),
                LocalDate.of(2026, 12, 31),
                LocalDate.of(2026, 12, 30),
                LocalDate.of(2026, 12, 29),
                LocalDate.of(2026, 12, 28),
                LocalDate.of(2026, 12, 27),
            ),
            provider.days(),
        )
    }

    @Test
    fun `includes February 29 in a leap year`() {
        val provider = ReviewWindowProvider(clockAt(LocalDateTime.of(2028, 3, 2, 12, 0)))

        assertEquals(
            listOf(
                LocalDate.of(2028, 3, 2),
                LocalDate.of(2028, 3, 1),
                LocalDate.of(2028, 2, 29),
                LocalDate.of(2028, 2, 28),
                LocalDate.of(2028, 2, 27),
                LocalDate.of(2028, 2, 26),
                LocalDate.of(2028, 2, 25),
            ),
            provider.days(),
        )
    }

    @Test
    fun `goes from March 1 to February 28 in a non-leap year`() {
        val provider = ReviewWindowProvider(clockAt(LocalDateTime.of(2027, 3, 1, 12, 0)))

        assertEquals(
            listOf(
                LocalDate.of(2027, 3, 1),
                LocalDate.of(2027, 2, 28),
                LocalDate.of(2027, 2, 27),
                LocalDate.of(2027, 2, 26),
                LocalDate.of(2027, 2, 25),
                LocalDate.of(2027, 2, 24),
                LocalDate.of(2027, 2, 23),
            ),
            provider.days(),
        )
    }

    @Test
    fun `reads today in the time zone of the clock`() {
        // 23 h 30 à Paris le 31 décembre = 22 h 30 UTC : le jour local est encore le 31.
        val instant = LocalDateTime.of(2026, 12, 31, 23, 30).atZone(PARIS).toInstant()
        val parisProvider = ReviewWindowProvider(Clock.fixed(instant, PARIS))
        // Même instant à Tokyo : on est déjà le 1er janvier.
        val tokyoProvider = ReviewWindowProvider(Clock.fixed(instant, TOKYO))

        assertEquals(LocalDate.of(2026, 12, 31), parisProvider.days().first())
        assertEquals(LocalDate.of(2027, 1, 1), tokyoProvider.days().first())
    }

    @Test
    fun `reads the clock on every call`() {
        val clock = MutableClock(clockAt(LocalDateTime.of(2026, 11, 4, 23, 59)))
        val provider = ReviewWindowProvider(clock)

        assertEquals(LocalDate.of(2026, 11, 4), provider.days().first())

        clock.current = clockAt(LocalDateTime.of(2026, 11, 5, 0, 1))

        assertEquals(LocalDate.of(2026, 11, 5), provider.days().first())
    }

    @Test
    fun `has seven days by default`() {
        val provider = ReviewWindowProvider(clockAt(LocalDateTime.of(2026, 11, 4, 9, 0)))

        assertEquals(ReviewWindowProvider.DEFAULT_WINDOW_SIZE, provider.days().size)
        assertEquals(7, ReviewWindowProvider.DEFAULT_WINDOW_SIZE)
    }

    @Test
    fun `honours a custom window size`() {
        val provider = ReviewWindowProvider(clockAt(LocalDateTime.of(2026, 11, 4, 9, 0)), windowSize = 3)

        assertEquals(
            listOf(LocalDate.of(2026, 11, 4), LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 2)),
            provider.days(),
        )
    }

    @Test
    fun `rejects a window without any day`() {
        val clock = clockAt(LocalDateTime.of(2026, 11, 4, 9, 0))

        assertThrows<IllegalArgumentException> { ReviewWindowProvider(clock, windowSize = 0) }
    }

    /** Horloge dont on peut changer l'heure en cours de test. */
    private class MutableClock(
        var current: Clock,
    ) : Clock() {
        override fun getZone(): ZoneId = current.zone

        override fun withZone(zone: ZoneId): Clock = MutableClock(current.withZone(zone))

        override fun instant() = current.instant()
    }

    private companion object {
        val PARIS: ZoneId = ZoneId.of("Europe/Paris")
        val TOKYO: ZoneId = ZoneId.of("Asia/Tokyo")

        fun clockAt(dateTime: LocalDateTime): Clock = Clock.fixed(dateTime.atZone(PARIS).toInstant(), PARIS)
    }
}
