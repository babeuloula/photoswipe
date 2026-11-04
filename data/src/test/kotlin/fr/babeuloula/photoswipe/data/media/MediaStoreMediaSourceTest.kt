package fr.babeuloula.photoswipe.data.media

import android.provider.MediaStore
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.MediaType
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
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

            val selection =
                provider.selections
                    .single()
                    .orEmpty()
                    .lowercase()
            listOf("date(", "datetime(", "strftime(", "julianday(").forEach { function ->
                assertFalse("selection uses $function", function in selection)
            }
        }

    @Test
    fun `returns nothing for no range`() =
        runTest {
            provider.addImage(id = 1, dateTaken = july15th2025.toEpochMilli())

            assertEquals(emptyList<MediaItem>(), source.mediaTakenIn(emptyList()))
            assertEquals(emptyList<String?>(), provider.selections)
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
}
