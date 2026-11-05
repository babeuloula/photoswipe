package fr.babeuloula.photoswipe.ui.deck

import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class MediaCardUiStateTest {
    private val paris = FixedClock.PARIS

    private fun takenAt(dateTime: LocalDateTime) = dateTime.atZone(paris).toInstant()

    @Test
    fun `keeps the media identity, type and size`() {
        val media =
            aMediaItem(id = 7, type = MediaType.VIDEO, sizeBytes = 45_000_000, duration = Duration.ofSeconds(32))

        val card = MediaCardUiState.of(media, LocalDate.of(2026, 5, 14), paris)

        assertEquals(MediaId(7), card.id)
        assertEquals(media.uri, card.uri)
        assertEquals(MediaType.VIDEO, card.type)
        assertEquals(45_000_000L, card.sizeBytes)
        assertEquals(Duration.ofSeconds(32), card.duration)
    }

    @Test
    fun `counts the years from the day of the deck`() {
        val media = aMediaItem(takenAt = takenAt(LocalDateTime.of(2023, 5, 14, 18, 0)))

        val card = MediaCardUiState.of(media, LocalDate.of(2026, 5, 14), paris)

        assertEquals(LocalDate.of(2023, 5, 14), card.takenOn)
        assertEquals(3, card.yearsAgo)
    }

    @Test
    fun `counts the years from a caught up day, not from today`() {
        val media = aMediaItem(takenAt = takenAt(LocalDateTime.of(2025, 12, 31, 12, 0)))

        val card = MediaCardUiState.of(media, LocalDate.of(2026, 12, 31), paris)

        assertEquals(1, card.yearsAgo)
    }

    @Test
    fun `keeps February 29 as the date of a leap day media shown on February 28`() {
        val media = aMediaItem(takenAt = takenAt(LocalDateTime.of(2024, 2, 29, 12, 0)))

        val card = MediaCardUiState.of(media, LocalDate.of(2026, 2, 28), paris)

        assertEquals(LocalDate.of(2024, 2, 29), card.takenOn)
        assertEquals(2, card.yearsAgo)
    }

    @Test
    fun `dates the media in the device time zone`() {
        val media = aMediaItem(takenAt = takenAt(LocalDateTime.of(2023, 5, 14, 23, 30)))

        val card = MediaCardUiState.of(media, LocalDate.of(2026, 5, 14), ZoneId.of("Asia/Tokyo"))

        assertEquals(LocalDate.of(2023, 5, 15), card.takenOn)
    }
}
