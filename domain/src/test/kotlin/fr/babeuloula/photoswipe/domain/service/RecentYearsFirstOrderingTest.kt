package fr.babeuloula.photoswipe.domain.service

import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.model.MediaItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class RecentYearsFirstOrderingTest {
    private val ordering = RecentYearsFirstOrdering()

    @Test
    fun `puts the most recent years first`() {
        val media =
            listOf(
                takenAt(id = 1, LocalDateTime.of(2020, 7, 15, 9, 0)),
                takenAt(id = 2, LocalDateTime.of(2025, 7, 15, 9, 0)),
                takenAt(id = 3, LocalDateTime.of(2023, 7, 15, 9, 0)),
            )

        assertEquals(listOf(2L, 3L, 1L), ids(ordering.order(media, FixedClock.PARIS)))
    }

    @Test
    fun `sorts media of the same year by ascending time`() {
        val media =
            listOf(
                takenAt(id = 1, LocalDateTime.of(2025, 7, 15, 18, 30)),
                takenAt(id = 2, LocalDateTime.of(2025, 7, 15, 8, 0)),
                takenAt(id = 3, LocalDateTime.of(2024, 7, 15, 23, 0)),
                takenAt(id = 4, LocalDateTime.of(2025, 7, 15, 12, 15)),
                takenAt(id = 5, LocalDateTime.of(2024, 7, 15, 6, 0)),
            )

        assertEquals(listOf(2L, 4L, 1L, 5L, 3L), ids(ordering.order(media, FixedClock.PARIS)))
    }

    @Test
    fun `shows February 28 before February 29 of the same leap year`() {
        val media =
            listOf(
                takenAt(id = 1, LocalDateTime.of(2024, 2, 29, 8, 0)),
                takenAt(id = 2, LocalDateTime.of(2024, 2, 28, 20, 0)),
            )

        assertEquals(listOf(2L, 1L), ids(ordering.order(media, FixedClock.PARIS)))
    }

    @Test
    fun `reads the year in the given time zone`() {
        val newYearInParis = Instant.parse("2024-12-31T23:30:00Z")
        val media =
            listOf(
                aMediaItem(id = 1, takenAt = Instant.parse("2024-12-31T12:00:00Z")),
                aMediaItem(id = 2, takenAt = newYearInParis),
            )

        assertEquals(listOf(2L, 1L), ids(ordering.order(media, FixedClock.PARIS)))
        assertEquals(listOf(1L, 2L), ids(ordering.order(media, ZoneId.of("UTC"))))
    }

    @Test
    fun `breaks ties on the same instant by media id`() {
        val sameTime = LocalDateTime.of(2025, 7, 15, 10, 0)
        val media = listOf(takenAt(id = 7, sameTime), takenAt(id = 3, sameTime), takenAt(id = 5, sameTime))

        assertEquals(listOf(3L, 5L, 7L), ids(ordering.order(media, FixedClock.PARIS)))
    }

    @Test
    fun `returns an empty list for an empty deck`() {
        assertEquals(emptyList<MediaItem>(), ordering.order(emptyList(), FixedClock.PARIS))
    }

    private fun takenAt(
        id: Long,
        dateTime: LocalDateTime,
    ): MediaItem = aMediaItem(id = id, takenAt = dateTime.atZone(FixedClock.PARIS).toInstant())

    private fun ids(media: List<MediaItem>): List<Long> = media.map { it.id.value }
}
