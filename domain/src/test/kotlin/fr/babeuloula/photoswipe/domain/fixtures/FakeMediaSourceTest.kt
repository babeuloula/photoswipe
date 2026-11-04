package fr.babeuloula.photoswipe.domain.fixtures

import fr.babeuloula.photoswipe.domain.model.MediaId
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class FakeMediaSourceTest {
    private val start = Instant.parse("2020-07-15T00:00:00Z")
    private val end = Instant.parse("2020-07-16T00:00:00Z")

    @Test
    fun `returns media taken from the start of a range up to its excluded end`() =
        runTest {
            val atStart = aMediaItem(id = 1, takenAt = start)
            val inside = aMediaItem(id = 2, takenAt = start.plusSeconds(3_600))
            val atEnd = aMediaItem(id = 3, takenAt = end)
            val before = aMediaItem(id = 4, takenAt = start.minusMillis(1))
            val source = FakeMediaSource(listOf(atStart, inside, atEnd, before))

            assertEquals(listOf(atStart, inside), source.mediaTakenIn(listOf(start..<end)))
        }

    @Test
    fun `returns a media only once when several ranges cover it`() =
        runTest {
            val media = aMediaItem(takenAt = start.plusSeconds(60))
            val source = FakeMediaSource(listOf(media))

            assertEquals(listOf(media), source.mediaTakenIn(listOf(start..<end, start..<end.plusSeconds(60))))
        }

    @Test
    fun `combines media from several ranges`() =
        runTest {
            val lastYear = aMediaItem(id = 1, takenAt = start)
            val twoYearsAgo = aMediaItem(id = 2, takenAt = Instant.parse("2019-07-15T12:00:00Z"))
            val source = FakeMediaSource(listOf(lastYear, twoYearsAgo))
            val ranges =
                listOf(
                    start..<end,
                    Instant.parse("2019-07-15T00:00:00Z")..<Instant.parse("2019-07-16T00:00:00Z"),
                )

            assertEquals(listOf(lastYear, twoYearsAgo), source.mediaTakenIn(ranges))
        }

    @Test
    fun `returns nothing without any range`() =
        runTest {
            assertEquals(emptyList<Any>(), FakeMediaSource(listOf(aMediaItem())).mediaTakenIn(emptyList()))
        }

    @Test
    fun `returns only the requested media that are still present`() =
        runTest {
            val source = FakeMediaSource(listOf(aMediaItem(id = 1), aMediaItem(id = 2), aMediaItem(id = 3)))

            source.remove(MediaId(2))

            assertEquals(
                listOf(aMediaItem(id = 1)),
                source.mediaByIds(setOf(MediaId(1), MediaId(2), MediaId(99))),
            )
        }

    @Test
    fun `replaces a media added again with the same id`() {
        val source = FakeMediaSource(listOf(aMediaItem(id = 1, sizeBytes = 10)))

        source.add(aMediaItem(id = 1, sizeBytes = 20), aMediaItem(id = 2))

        assertEquals(listOf(aMediaItem(id = 1, sizeBytes = 20), aMediaItem(id = 2)), source.media)
    }
}
