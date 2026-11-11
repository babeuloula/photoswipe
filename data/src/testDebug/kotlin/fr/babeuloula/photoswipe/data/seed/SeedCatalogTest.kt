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

    /** Jours de la fenêtre, d'aujourd'hui (rang 0) au plus ancien. */
    private val window = (0 until SeedCatalog.WINDOW_DAYS).map { MonthDay.from(today.minusDays(it.toLong())) }

    @Test
    fun `seeds photos of several previous years for today and the 6 previous days`() {
        val photos = plan.filter { it.type == MediaType.IMAGE }

        assertEquals(window.toSet(), photos.map { MonthDay.from(it.takenAt) }.toSet())
        window.filterIndexed { daysAgo, _ -> daysAgo != SeedCatalog.SINGLE_PHOTO_DAY }.forEach { day ->
            val years = photos.filter { MonthDay.from(it.takenAt) == day }.map { today.year - it.takenAt.year }
            assertEquals((1..SeedCatalog.YEARS_BACK).toSet(), years.toSet())
        }
    }

    @Test
    fun `seeds a burst of photos taken a few seconds apart today`() {
        val today = plan.filter { it.type == MediaType.IMAGE && MonthDay.from(it.takenAt) == MonthDay.from(this.today) }
        val burst = today.filter { it.takenAt.hour == 15 }.map { it.takenAt.second }

        assertEquals(listOf(0, 4, 8), burst.sorted())
    }

    @Test
    fun `seeds a single photo and no video two days ago, so that deleting it empties the day`() {
        val day = MonthDay.from(today.minusDays(SeedCatalog.SINGLE_PHOTO_DAY.toLong()))

        val media = plan.filter { MonthDay.from(it.takenAt) == day }

        assertEquals(listOf(MediaType.IMAGE), media.map { it.type })
    }

    @Test
    fun `seeds one video of a previous year for each day of the window but the single photo one`() {
        val videos = plan.filter { it.type == MediaType.VIDEO }

        assertEquals(SeedCatalog.WINDOW_DAYS - 1, videos.size)
        assertEquals(SeedCatalog.WINDOW_DAYS - 1, videos.map { MonthDay.from(it.takenAt) }.toSet().size)
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
        assertTrue("photoswipe-seed-2025-11-06-100000.jpg" in names)
        assertTrue("photoswipe-seed-2025-11-06-193000.mp4" in names)
    }
}
