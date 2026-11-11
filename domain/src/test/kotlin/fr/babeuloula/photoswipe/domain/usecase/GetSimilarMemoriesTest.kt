package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.SimilarGroup
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class GetSimilarMemoriesTest {
    private val today = LocalDate.of(2026, 5, 14)
    private val clock = FixedClock.at(today.atTime(10, 0))

    private fun takenAt(
        id: Long,
        time: LocalDateTime,
    ) = aMediaItem(id = id, takenAt = time.atZone(clock.zone).toInstant())

    @Test
    fun `finds the photos of the day taken one after another, whatever their year`() =
        runTest {
            val gallery =
                FakeMediaSource(
                    listOf(
                        takenAt(1, LocalDateTime.of(2023, 5, 14, 9, 0, 0)),
                        takenAt(2, LocalDateTime.of(2023, 5, 14, 9, 0, 12)),
                        takenAt(3, LocalDateTime.of(2020, 5, 14, 18, 0)),
                        takenAt(4, LocalDateTime.of(2023, 5, 15, 9, 0, 20)),
                    ),
                )

            val similar = GetSimilarMemories(SameDayMatcher(), gallery, clock)(today)

            assertEquals(mapOf(MediaId(1) to SimilarGroup(1, 2), MediaId(2) to SimilarGroup(2, 2)), similar)
        }
}
