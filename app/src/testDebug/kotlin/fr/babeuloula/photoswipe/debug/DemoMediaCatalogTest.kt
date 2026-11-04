package fr.babeuloula.photoswipe.debug

import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.model.MediaType
import fr.babeuloula.photoswipe.domain.service.ReviewWindowProvider
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class DemoMediaCatalogTest {
    private val clock = FixedClock.at(LocalDateTime.of(2026, 11, 4, 10, 0))
    private val media = DemoMediaCatalog.generate(clock)

    @Test
    fun `gives memories from several years to each day of the review window`() =
        runTest {
            val source = FakeMediaSource(media)
            ReviewWindowProvider(clock).days().forEach { day ->
                val memories = source.mediaTakenIn(SameDayMatcher().rangesFor(day, clock.zone))
                val years = memories.mapTo(HashSet()) { it.takenAt.atZone(clock.zone).year }

                assertTrue("memories of $day", years.size >= DemoMediaCatalog.YEARS_BACK)
            }
        }

    @Test
    fun `includes several media taken the same day`() {
        val perDay = media.groupingBy { it.takenAt.atZone(clock.zone).toLocalDate() }.eachCount()

        assertTrue(perDay.values.any { it >= 2 })
    }

    @Test
    fun `includes a february 29 photo, a big video and a media dated by its addition`() {
        val days = media.map { it.takenAt.atZone(clock.zone) }

        assertTrue(days.any { it.toLocalDate() == LocalDate.of(2024, 2, 29) })
        assertTrue(media.any { it.type == MediaType.VIDEO && it.sizeBytes == DemoMediaCatalog.BIG_VIDEO_BYTES })
        assertTrue(days.any { it.toLocalTime().toSecondOfDay() == 0 })
    }

    @Test
    fun `gives every media its own id`() {
        assertEquals(media.size, media.map { it.id }.toSet().size)
    }

    @Test
    fun `follows the time zone of the clock`() {
        val tokyo =
            DemoMediaCatalog.generate(
                FixedClock.at(LocalDateTime.of(2026, 11, 4, 10, 0), ZoneId.of("Asia/Tokyo")),
            )

        assertEquals(media.size, tokyo.size)
    }
}
