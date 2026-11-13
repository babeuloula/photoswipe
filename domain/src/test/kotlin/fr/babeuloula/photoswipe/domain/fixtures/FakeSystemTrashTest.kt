package fr.babeuloula.photoswipe.domain.fixtures

import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.TrashedMedia
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

/** `FakeSystemTrash` respecte le contrat de son port. */
class FakeSystemTrashTest {
    private val purgeAt = Instant.parse("2026-08-14T10:00:00Z")

    @Test
    fun `returns only the requested media still in the trash`() =
        runTest {
            val trash = FakeSystemTrash(listOf(TrashedMedia(aMediaItem(id = 1), purgeAt)))
            trash.add(aMediaItem(id = 2))
            trash.add(aMediaItem(id = 3))

            assertEquals(
                listOf(TrashedMedia(aMediaItem(id = 1), purgeAt), TrashedMedia(aMediaItem(id = 3), null)),
                trash.trashed(setOf(MediaId(1), MediaId(3), MediaId(9))),
            )
        }

    @Test
    fun `no longer returns a media removed from the trash`() =
        runTest {
            val trash = FakeSystemTrash()
            trash.add(aMediaItem(id = 1))

            trash.remove(listOf(MediaId(1), MediaId(9)))

            assertEquals(emptyList<TrashedMedia>(), trash.trashed(setOf(MediaId(1))))
            assertEquals(emptyList<TrashedMedia>(), trash.media)
        }
}
