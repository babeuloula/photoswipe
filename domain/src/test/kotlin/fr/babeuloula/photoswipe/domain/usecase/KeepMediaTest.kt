package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.KEPT
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.MARKED_FOR_DELETION
import fr.babeuloula.photoswipe.domain.model.ReviewEntry
import fr.babeuloula.photoswipe.domain.model.SessionDecision
import fr.babeuloula.photoswipe.domain.service.RecentYearsFirstOrdering
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime.NOON

class KeepMediaTest {
    private val clock = FixedClock.at(LocalDateTime.of(2026, 7, 15, 21, 30))
    private val repository = FakeReviewRepository()
    private val keepMedia = KeepMedia(repository, clock)

    @Test
    fun `records a kept decision for the media and the year of the target day`() =
        runTest {
            val media = aMediaItem(id = 42, sizeBytes = 3_500_000)

            keepMedia(media, day = LocalDate.of(2026, 7, 15))

            assertEquals(
                listOf(
                    ReviewEntry(
                        mediaId = media.id,
                        reviewYear = 2026,
                        decision = KEPT,
                        sizeBytes = 3_500_000,
                        decidedAt = clock.instant(),
                    ),
                ),
                repository.entries,
            )
        }

    @Test
    fun `uses the year of the target day when catching up on the previous year`() =
        runTest {
            keepMedia(aMediaItem(id = 1), day = LocalDate.of(2025, 12, 28))

            assertEquals(2025, repository.entries.single().reviewYear)
        }

    @Test
    fun `replaces a previous decision of the same review year`() =
        runTest {
            val repository =
                FakeReviewRepository(
                    listOf(aReviewEntry(mediaId = 1, reviewYear = 2026, decision = MARKED_FOR_DELETION)),
                )

            KeepMedia(repository, clock)(aMediaItem(id = 1), day = LocalDate.of(2026, 7, 15))

            assertEquals(KEPT, repository.entries.single().decision)
        }

    @Test
    fun `hides a kept media for the rest of the year and offers it again the next year`() =
        runTest {
            val media = takenOn(id = 1, LocalDate.of(2024, 7, 15))

            assertEquals(listOf(media), deck(listOf(media), LocalDate.of(2026, 7, 15)))

            keepMedia(media, day = LocalDate.of(2026, 7, 15))

            assertEquals(emptyList<MediaItem>(), deck(listOf(media), LocalDate.of(2026, 7, 15)))
            assertEquals(listOf(media), deck(listOf(media), LocalDate.of(2027, 7, 15)))

            keepMedia(media, day = LocalDate.of(2027, 7, 15))

            assertEquals(emptyList<MediaItem>(), deck(listOf(media), LocalDate.of(2027, 7, 15)))
            assertEquals(listOf(media), deck(listOf(media), LocalDate.of(2028, 7, 15)))
            assertEquals(listOf(2026, 2027), repository.entries.map { it.reviewYear })
        }

    @Test
    fun `returns the decision to stack for undo with the entry it replaced`() =
        runTest {
            val replaced = aReviewEntry(mediaId = 1, reviewYear = 2026, decision = MARKED_FOR_DELETION)
            val repository = FakeReviewRepository(listOf(replaced))
            val media = aMediaItem(id = 1)

            val decision = KeepMedia(repository, clock)(media, LocalDate.of(2026, 7, 15))

            assertEquals(SessionDecision(media, LocalDate.of(2026, 7, 15), previous = replaced), decision)
        }

    @Test
    fun `returns a decision without previous entry for a media to sort`() =
        runTest {
            val media = aMediaItem(id = 1)

            val decision = keepMedia(media, LocalDate.of(2026, 7, 15))

            assertEquals(SessionDecision(media, LocalDate.of(2026, 7, 15)), decision)
        }

    /** Paquet du jour [day], lu avec le même dépôt que [keepMedia]. */
    private suspend fun deck(
        media: List<MediaItem>,
        day: LocalDate,
    ): List<MediaItem> =
        GetDeckForDay(
            SameDayMatcher(),
            FakeMediaSource(media),
            repository,
            RecentYearsFirstOrdering(),
            FixedClock.at(day.atTime(NOON)),
        )(day)

    private fun takenOn(
        id: Long,
        date: LocalDate,
    ): MediaItem = aMediaItem(id = id, takenAt = LocalDateTime.of(date, NOON).atZone(FixedClock.PARIS).toInstant())
}
