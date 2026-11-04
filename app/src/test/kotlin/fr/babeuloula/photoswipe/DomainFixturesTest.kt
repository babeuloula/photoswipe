package fr.babeuloula.photoswipe

import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.MediaId
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/** Vérifie que les fixtures en mémoire de `:domain` sont utilisables par les tests de `:app` (SPEC §10). */
class DomainFixturesTest {
    @Test
    fun `domain builders, fakes and fixed clock are available to app tests`() {
        val source = FakeMediaSource(listOf(aMediaItem(id = 7)))
        val repository = FakeReviewRepository(listOf(aReviewEntry(mediaId = 7)))
        val clock = FixedClock.at(LocalDateTime.of(2026, 7, 15, 10, 0))

        assertEquals(listOf(MediaId(7)), source.media.map { it.id })
        assertEquals(listOf(MediaId(7)), repository.entries.map { it.mediaId })
        assertEquals(LocalDate.of(2026, 7, 15), LocalDate.now(clock))
    }
}
