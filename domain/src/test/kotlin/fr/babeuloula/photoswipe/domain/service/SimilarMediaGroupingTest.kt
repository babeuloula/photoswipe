package fr.babeuloula.photoswipe.domain.service

import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.MediaType
import fr.babeuloula.photoswipe.domain.model.SimilarGroup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.time.Instant

class SimilarMediaGroupingTest {
    private val start = Instant.parse("2023-05-14T10:00:00Z")
    private val grouping = SimilarMediaGrouping()

    private fun photoAt(
        id: Long,
        seconds: Long,
        type: MediaType = MediaType.IMAGE,
    ): MediaItem =
        aMediaItem(
            id = id,
            type = type,
            takenAt = start.plusSeconds(seconds),
            duration = if (type == MediaType.VIDEO) Duration.ofSeconds(3) else null,
        )

    @Test
    fun `groups photos taken one after another, in the order they were taken`() {
        val groups = grouping.groups(listOf(photoAt(3, 50), photoAt(1, 0), photoAt(2, 25)))

        assertEquals(
            mapOf(
                MediaId(1) to SimilarGroup(1, 3),
                MediaId(2) to SimilarGroup(2, 3),
                MediaId(3) to SimilarGroup(3, 3),
            ),
            groups,
        )
    }

    @Test
    fun `starts a new group after a longer pause and leaves lone photos out`() {
        val groups =
            grouping.groups(
                listOf(photoAt(1, 0), photoAt(2, 10), photoAt(3, 300), photoAt(4, 1_000), photoAt(5, 1_030)),
            )

        assertEquals(setOf(MediaId(1), MediaId(2), MediaId(4), MediaId(5)), groups.keys)
        assertEquals(SimilarGroup(2, 2), groups[MediaId(5)])
    }

    @Test
    fun `leaves videos out of the groups`() {
        val groups = grouping.groups(listOf(photoAt(1, 0), photoAt(2, 5, MediaType.VIDEO), photoAt(3, 60)))

        assertEquals(emptyMap<MediaId, SimilarGroup>(), groups)
    }

    @Test
    fun `follows the configured gap`() {
        val groups = SimilarMediaGrouping(Duration.ofSeconds(5)).groups(listOf(photoAt(1, 0), photoAt(2, 10)))

        assertEquals(emptyMap<MediaId, SimilarGroup>(), groups)
    }

    @Test
    fun `rejects an invalid group`() {
        assertThrows<IllegalArgumentException> { SimilarGroup(1, 1) }
        assertThrows<IllegalArgumentException> { SimilarGroup(3, 2) }
    }
}
