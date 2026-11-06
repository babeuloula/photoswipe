package fr.babeuloula.photoswipe.data.seed

import fr.babeuloula.photoswipe.domain.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.MonthDay
import java.time.ZoneId

class SeedCatalogTest {
    private val today = LocalDate.of(2026, 11, 6)
    private val zone = ZoneId.of("Europe/Paris")
    private val plan = SeedCatalog.plan(today, zone)

    @Test
    fun `seeds photos of several previous years for today and the 6 previous days`() {
        val window = (0L until 7L).map { MonthDay.from(today.minusDays(it)) }.toSet()

        val photos = plan.filter { it.type == MediaType.IMAGE }

        assertEquals(window, photos.map { MonthDay.from(it.takenAt) }.toSet())
        window.forEach { day ->
            val years = photos.filter { MonthDay.from(it.takenAt) == day }.map { today.year - it.takenAt.year }
            assertEquals((1..SeedCatalog.YEARS_BACK).toList(), years.sorted())
        }
    }

    @Test
    fun `seeds one video of a previous year for each day of the window`() {
        val videos = plan.filter { it.type == MediaType.VIDEO }

        assertEquals(SeedCatalog.WINDOW_DAYS, videos.size)
        assertEquals(SeedCatalog.WINDOW_DAYS, videos.map { MonthDay.from(it.takenAt) }.toSet().size)
        assertTrue(videos.all { it.takenAt.year < today.year })
    }

    @Test
    fun `dates every media in the given time zone`() {
        assertTrue(plan.all { it.takenAt.zone == zone })
    }

    @Test
    fun `names every media file after its date and type`() {
        val names = plan.map { it.displayName }

        assertEquals(names.size, names.toSet().size)
        assertTrue("photoswipe-seed-2025-11-06-1000.jpg" in names)
        assertTrue("photoswipe-seed-2025-11-06-1930.mp4" in names)
    }
}
