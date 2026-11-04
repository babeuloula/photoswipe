package fr.babeuloula.photoswipe.domain.service

import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.MediaType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.time.ZoneId
import java.time.ZoneOffset

class SameDayMatcherTest {
    private val matcher = SameDayMatcher()

    @Test
    fun `matches media taken on the same month and day in an earlier year`() {
        val media = mediaTakenOn(id = 1, date = LocalDateTime.of(2025, 11, 4, 9, 30))

        assertTrue(matcher.matches(media, target = LocalDate.of(2026, 11, 4), zone = PARIS))
    }

    @Test
    fun `matches media from every earlier year`() {
        val media =
            listOf(
                mediaTakenOn(id = 1, date = LocalDateTime.of(2025, 12, 28, 10, 0)),
                mediaTakenOn(id = 2, date = LocalDateTime.of(2019, 12, 28, 23, 59)),
                mediaTakenOn(id = 3, date = LocalDateTime.of(2008, 12, 28, 0, 0)),
            )

        val selected = matcher.select(media, target = LocalDate.of(2026, 12, 28), zone = PARIS)

        assertEquals(media, selected)
    }

    @Test
    fun `excludes media taken during the year of the target day`() {
        val media = mediaTakenOn(id = 1, date = LocalDateTime.of(2026, 11, 4, 8, 0))

        assertFalse(matcher.matches(media, target = LocalDate.of(2026, 11, 4), zone = PARIS))
    }

    @Test
    fun `excludes media dated after the year of the target day`() {
        val media = mediaTakenOn(id = 1, date = LocalDateTime.of(2027, 11, 4, 8, 0))

        assertFalse(matcher.matches(media, target = LocalDate.of(2026, 11, 4), zone = PARIS))
    }

    @Test
    fun `excludes media taken on another day or another month`() {
        val media =
            listOf(
                mediaTakenOn(id = 1, date = LocalDateTime.of(2025, 11, 3, 12, 0)),
                mediaTakenOn(id = 2, date = LocalDateTime.of(2025, 11, 5, 12, 0)),
                mediaTakenOn(id = 3, date = LocalDateTime.of(2025, 10, 4, 12, 0)),
            )

        val selected = matcher.select(media, target = LocalDate.of(2026, 11, 4), zone = PARIS)

        assertTrue(selected.isEmpty())
    }

    @Test
    fun `keeps only matching media, in their original order`() {
        val match2024 = mediaTakenOn(id = 1, date = LocalDateTime.of(2024, 11, 4, 18, 0))
        val sameYear = mediaTakenOn(id = 2, date = LocalDateTime.of(2026, 11, 4, 7, 0))
        val otherDay = mediaTakenOn(id = 3, date = LocalDateTime.of(2023, 6, 14, 7, 0))
        val match2021 = mediaTakenOn(id = 4, date = LocalDateTime.of(2021, 11, 4, 7, 0))

        val selected =
            matcher.select(
                listOf(match2024, sameYear, otherDay, match2021),
                target = LocalDate.of(2026, 11, 4),
                zone = PARIS,
            )

        assertEquals(listOf(match2024, match2021), selected)
    }

    @Test
    fun `returns nothing for a day without media`() {
        val selected = matcher.select(emptyList(), target = LocalDate.of(2026, 11, 4), zone = PARIS)

        assertTrue(selected.isEmpty())
    }

    @Test
    fun `uses the given time zone to find the local day of the shot`() {
        // 27 décembre 2025 à 23 h 30 UTC : déjà le 28 décembre à Paris (UTC+1 en hiver).
        val media = mediaTakenAt(id = 1, instant = Instant.parse("2025-12-27T23:30:00Z"))
        val target = LocalDate.of(2026, 12, 28)

        assertTrue(matcher.matches(media, target, zone = PARIS))
        assertFalse(matcher.matches(media, target, zone = ZoneOffset.UTC))
    }

    @Test
    fun `matches media of earlier years across the new year`() {
        // Exemple du SPEC §3.1 : le 2 janvier 2027, le rattrapage du 28 décembre 2026
        // retient le 28 décembre 2025 mais pas le 28 décembre 2026.
        val from2025 = mediaTakenOn(id = 1, date = LocalDateTime.of(2025, 12, 28, 15, 0))
        val from2026 = mediaTakenOn(id = 2, date = LocalDateTime.of(2026, 12, 28, 15, 0))

        val selected =
            matcher.select(
                listOf(from2025, from2026),
                target = LocalDate.of(2026, 12, 28),
                zone = PARIS,
            )

        assertEquals(listOf(from2025), selected)
    }

    @Test
    fun `moves leap day media to February 28 on non-leap years`() {
        val leapDay = mediaTakenOn(id = 1, date = LocalDateTime.of(2024, 2, 29, 12, 0))

        assertTrue(matcher.matches(leapDay, target = LocalDate.of(2027, 2, 28), zone = PARIS))
    }

    @Test
    fun `keeps February 28 media alongside leap day media on non-leap years`() {
        val leapDay = mediaTakenOn(id = 1, date = LocalDateTime.of(2024, 2, 29, 12, 0))
        val feb28 = mediaTakenOn(id = 2, date = LocalDateTime.of(2025, 2, 28, 12, 0))

        val selected =
            matcher.select(listOf(leapDay, feb28), target = LocalDate.of(2027, 2, 28), zone = PARIS)

        assertEquals(listOf(leapDay, feb28), selected)
    }

    @Test
    fun `does not show leap day media on March 1 of non-leap years`() {
        val leapDay = mediaTakenOn(id = 1, date = LocalDateTime.of(2024, 2, 29, 12, 0))

        assertFalse(matcher.matches(leapDay, target = LocalDate.of(2027, 3, 1), zone = PARIS))
    }

    @Test
    fun `shows leap day media on February 29 of leap years`() {
        val leapDay = mediaTakenOn(id = 1, date = LocalDateTime.of(2024, 2, 29, 12, 0))

        assertTrue(matcher.matches(leapDay, target = LocalDate.of(2028, 2, 29), zone = PARIS))
    }

    @Test
    fun `does not show leap day media on February 28 of leap years`() {
        val leapDay = mediaTakenOn(id = 1, date = LocalDateTime.of(2024, 2, 29, 12, 0))
        val feb28 = mediaTakenOn(id = 2, date = LocalDateTime.of(2025, 2, 28, 12, 0))

        val selected =
            matcher.select(listOf(leapDay, feb28), target = LocalDate.of(2028, 2, 28), zone = PARIS)

        assertEquals(listOf(feb28), selected)
    }

    @Test
    fun `gives one midnight-to-midnight range per earlier year since 1970`() {
        val ranges = matcher.rangesFor(target = LocalDate.of(2026, 11, 4), zone = PARIS)

        assertEquals(2026 - SameDayMatcher.EARLIEST_YEAR, ranges.size)
        assertEquals(midnight(1970, 11, 4)..<midnight(1970, 11, 5), ranges.first())
        assertEquals(midnight(2025, 11, 4)..<midnight(2025, 11, 5), ranges.last())
    }

    @Test
    fun `follows daylight saving time so a range may last 23 hours`() {
        val ranges = matcher.rangesFor(target = LocalDate.of(2026, 3, 29), zone = PARIS)

        val springForward2020 = ranges.single { it.start == midnight(2020, 3, 29) }
        assertEquals(Duration.ofHours(23), Duration.between(springForward2020.start, springForward2020.endExclusive))
    }

    @Test
    fun `extends February 28 ranges of leap years to February 29 when the target year is not leap`() {
        val ranges = matcher.rangesFor(target = LocalDate.of(2027, 2, 28), zone = PARIS)

        assertTrue(midnight(2024, 2, 28)..<midnight(2024, 3, 1) in ranges)
        assertTrue(midnight(2025, 2, 28)..<midnight(2025, 3, 1) in ranges)
        assertEquals(2027 - SameDayMatcher.EARLIEST_YEAR, ranges.size)
    }

    @Test
    fun `keeps February 28 to one day when the target year is leap`() {
        val ranges = matcher.rangesFor(target = LocalDate.of(2028, 2, 28), zone = PARIS)

        assertTrue(midnight(2024, 2, 28)..<midnight(2024, 2, 29) in ranges)
    }

    @Test
    fun `gives February 29 ranges for leap years only`() {
        val ranges = matcher.rangesFor(target = LocalDate.of(2028, 2, 29), zone = PARIS)

        val leapYears = (SameDayMatcher.EARLIEST_YEAR until 2028).filter { Year.isLeap(it.toLong()) }
        assertEquals(leapYears.map { midnight(it, 2, 29)..<midnight(it, 3, 1) }, ranges)
    }

    @Test
    fun `gives no range when no earlier year is searched`() {
        assertEquals(emptyList<Any>(), matcher.rangesFor(LocalDate.of(SameDayMatcher.EARLIEST_YEAR, 6, 1), PARIS))
    }

    @Test
    fun `ranges cover exactly the media that match, hour by hour over two years`() {
        val targets =
            listOf(
                LocalDate.of(2026, 11, 4),
                LocalDate.of(2026, 3, 29),
                LocalDate.of(2026, 10, 25),
                LocalDate.of(2027, 1, 1),
                LocalDate.of(2027, 2, 28),
                LocalDate.of(2028, 2, 28),
                LocalDate.of(2028, 2, 29),
            )
        val instants =
            generateSequence(midnight(2023, 1, 1)) { it.plus(Duration.ofHours(1)) }
                .takeWhile { it < midnight(2025, 1, 1) }
                .toList()

        targets.forEach { target ->
            val ranges = matcher.rangesFor(target, PARIS)
            val mismatches =
                instants.filter { instant ->
                    ranges.any { instant in it } != matcher.matches(mediaTakenAt(1, instant), target, PARIS)
                }
            assertEquals(emptyList<Instant>(), mismatches, "target $target")
        }
    }

    private fun midnight(
        year: Int,
        month: Int,
        day: Int,
    ): Instant = LocalDate.of(year, month, day).atStartOfDay(PARIS).toInstant()

    private fun mediaTakenOn(
        id: Long,
        date: LocalDateTime,
    ): MediaItem = mediaTakenAt(id, date.atZone(PARIS).toInstant())

    private fun mediaTakenAt(
        id: Long,
        instant: Instant,
    ): MediaItem =
        MediaItem(
            id = MediaId(id),
            uri = "content://media/external/images/media/$id",
            type = MediaType.IMAGE,
            takenAt = instant,
            sizeBytes = 1_024,
        )

    private companion object {
        val PARIS: ZoneId = ZoneId.of("Europe/Paris")
    }
}
