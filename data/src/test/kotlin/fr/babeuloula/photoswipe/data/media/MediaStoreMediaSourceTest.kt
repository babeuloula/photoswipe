package fr.babeuloula.photoswipe.data.media

import android.content.ContentResolver
import android.provider.MediaStore
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.MediaType
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** [MediaStoreMediaSource] face à un faux MediaStore adossé à SQLite (Robolectric). */
@RunWith(RobolectricTestRunner::class)
class MediaStoreMediaSourceTest {
    private val provider = Robolectric.setupContentProvider(FakeMediaStoreProvider::class.java, MediaStore.AUTHORITY)
    private val source =
        MediaStoreMediaSource(RuntimeEnvironment.getApplication().contentResolver, UnconfinedTestDispatcher())
    private val paris = ZoneId.of("Europe/Paris")
    private val july15th2025 = Instant.parse("2025-07-15T10:00:00Z")

    @Test
    fun `reads the photos taken within the requested ranges`() =
        runTest {
            val start = Instant.parse("2025-07-14T22:00:00Z")
            val end = Instant.parse("2025-07-15T22:00:00Z")
            provider.addImage(id = 1, dateTaken = start.toEpochMilli())
            provider.addImage(id = 2, dateTaken = end.toEpochMilli() - 1)
            provider.addImage(id = 3, dateTaken = end.toEpochMilli())
            provider.addImage(id = 4, dateTaken = start.toEpochMilli() - 1)

            val media = source.mediaTakenIn(listOf(start..<end))

            assertEquals(listOf(1L, 2L), media.map { it.id.value }.sorted())
        }

    @Test
    fun `reads the photos of a day of every previous year with the ranges of the domain`() =
        runTest {
            provider.addImage(id = 1, dateTaken = Instant.parse("2025-07-15T10:00:00Z").toEpochMilli())
            provider.addImage(id = 2, dateTaken = Instant.parse("2019-07-15T21:30:00Z").toEpochMilli())
            provider.addImage(id = 3, dateTaken = Instant.parse("2019-07-15T22:30:00Z").toEpochMilli())
            provider.addImage(id = 4, dateTaken = Instant.parse("2026-07-15T10:00:00Z").toEpochMilli())

            val ranges = SameDayMatcher().rangesFor(LocalDate.of(2026, 7, 15), paris)

            assertEquals(listOf(1L, 2L), source.mediaTakenIn(ranges).map { it.id.value }.sorted())
        }

    @Test
    fun `maps a photo to a media item with its content uri, date and size`() =
        runTest {
            provider.addImage(id = 42, dateTaken = july15th2025.toEpochMilli(), size = 3_500_000)

            val media = source.mediaTakenIn(listOf(july15th2025..<july15th2025.plusSeconds(1)))

            assertEquals(
                listOf(
                    MediaItem(
                        id = MediaId(42),
                        uri = "content://media/external/images/media/42",
                        type = MediaType.IMAGE,
                        takenAt = july15th2025,
                        sizeBytes = 3_500_000,
                    ),
                ),
                media,
            )
        }

    @Test
    fun `sends a selection without any sql date function`() =
        runTest {
            source.mediaTakenIn(SameDayMatcher().rangesFor(LocalDate.of(2026, 7, 15), paris))

            val selections =
                provider.queries.map {
                    it
                        .getString(
                            ContentResolver.QUERY_ARG_SQL_SELECTION,
                        ).orEmpty()
                        .lowercase()
                }
            assertEquals(2, selections.size)
            listOf("date(", "datetime(", "strftime(", "julianday(").forEach { function ->
                selections.forEach { assertFalse("selection uses $function", function in it) }
            }
        }

    @Test
    fun `returns nothing for no range`() =
        runTest {
            provider.addImage(id = 1, dateTaken = july15th2025.toEpochMilli())

            assertEquals(emptyList<MediaItem>(), source.mediaTakenIn(emptyList()))
            assertEquals(0, provider.queries.size)
        }

    @Test
    fun `falls back to the date added when the date taken is missing`() =
        runTest {
            provider.addImage(id = 1, dateTaken = null, dateAdded = july15th2025.epochSecond)
            provider.addImage(id = 2, dateTaken = 0, dateAdded = july15th2025.epochSecond)

            val media = source.mediaByIds(setOf(MediaId(1), MediaId(2)))

            assertEquals(listOf(july15th2025, july15th2025), media.map { it.takenAt })
        }

    @Test
    fun `reads the photos that still exist among many ids`() =
        runTest {
            (1L..1_200L).forEach { provider.addImage(id = it, dateTaken = july15th2025.toEpochMilli()) }

            val media = source.mediaByIds((1_000L..1_300L).mapTo(HashSet()) { MediaId(it) } + MediaId(1))

            assertEquals((listOf(1L) + (1_000L..1_200L)).sorted(), media.map { it.id.value }.sorted())
        }

    @Test
    fun `counts an unknown size as zero`() =
        runTest {
            provider.addImage(id = 1, dateTaken = july15th2025.toEpochMilli(), size = null)

            assertEquals(0L, source.mediaByIds(setOf(MediaId(1))).single().sizeBytes)
        }

    @Test
    fun `reads the videos with their duration and size`() =
        runTest {
            provider.addVideo(id = 7, dateTaken = july15th2025.toEpochMilli(), size = 80_000_000, duration = 12_500)
            provider.addImage(id = 8, dateTaken = july15th2025.toEpochMilli())

            val media = source.mediaTakenIn(listOf(july15th2025..<july15th2025.plusSeconds(1))).sortedBy { it.id.value }

            assertEquals(
                MediaItem(
                    id = MediaId(7),
                    uri = "content://media/external/video/media/7",
                    type = MediaType.VIDEO,
                    takenAt = july15th2025,
                    sizeBytes = 80_000_000,
                    duration = Duration.ofMillis(12_500),
                ),
                media.first(),
            )
            assertEquals(MediaType.IMAGE, media[1].type)
        }

    @Test
    fun `leaves the duration of a video unknown when it is missing`() =
        runTest {
            provider.addVideo(id = 1, dateTaken = july15th2025.toEpochMilli(), duration = null)

            assertNull(source.mediaByIds(setOf(MediaId(1))).single().duration)
        }

    @Test
    fun `finds a media without date taken within the ranges by its date added`() =
        runTest {
            val start = Instant.parse("2025-07-14T22:00:00Z")
            val end = Instant.parse("2025-07-15T22:00:00Z")
            provider.addImage(id = 1, dateTaken = null, dateAdded = start.epochSecond)
            provider.addVideo(id = 2, dateTaken = 0, dateAdded = end.epochSecond - 1)
            provider.addImage(id = 3, dateTaken = null, dateAdded = end.epochSecond)
            provider.addImage(
                id = 4,
                dateTaken = Instant.parse("2020-01-01T00:00:00Z").toEpochMilli(),
                dateAdded = start.epochSecond,
            )

            val media = source.mediaTakenIn(listOf(start..<end))

            assertEquals(listOf(1L, 2L), media.map { it.id.value }.sorted())
            assertEquals(start, media.single { it.id.value == 1L }.takenAt)
        }

    @Test
    fun `excludes the media already in the system trash`() =
        runTest {
            provider.addImage(id = 1, dateTaken = july15th2025.toEpochMilli(), trashed = true)
            provider.addVideo(id = 2, dateTaken = july15th2025.toEpochMilli(), trashed = true)
            provider.addImage(id = 3, dateTaken = july15th2025.toEpochMilli())

            assertEquals(
                listOf(3L),
                source.mediaTakenIn(listOf(july15th2025..<july15th2025.plusSeconds(1))).map { it.id.value },
            )
            assertEquals(listOf(3L), source.mediaByIds(setOf(MediaId(1), MediaId(2), MediaId(3))).map { it.id.value })
            provider.queries.forEach {
                assertEquals(
                    MediaStore.MATCH_EXCLUDE,
                    it.getInt(MediaStore.QUERY_ARG_MATCH_TRASHED),
                )
            }
        }
}
