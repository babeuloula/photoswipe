package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.MediaAccess
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.KEPT
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.MARKED_FOR_DELETION
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.TRASHED
import fr.babeuloula.photoswipe.domain.model.ReviewEntry
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class CleanOrphanEntriesTest {
    private val gallery = FakeMediaSource((1L..4L).map { aMediaItem(id = it) })

    @Test
    fun `keeps every entry when no media has disappeared`() =
        runTest {
            val entries = listOf(aReviewEntry(mediaId = 1), aReviewEntry(mediaId = 2, decision = MARKED_FOR_DELETION))
            val repository = FakeReviewRepository(entries)

            assertEquals(0, CleanOrphanEntries(repository, gallery)(MediaAccess.FULL))
            assertEquals(entries, repository.entries)
        }

    @Test
    fun `deletes the entries of every review year of the media that have disappeared`() =
        runTest {
            val repository =
                FakeReviewRepository(
                    listOf(
                        aReviewEntry(mediaId = 1, reviewYear = 2025),
                        aReviewEntry(mediaId = 1, reviewYear = 2026),
                        aReviewEntry(mediaId = 2),
                        aReviewEntry(mediaId = 3, decision = MARKED_FOR_DELETION),
                    ),
                )
            gallery.remove(listOf(MediaId(1), MediaId(3)))

            assertEquals(2, CleanOrphanEntries(repository, gallery)(MediaAccess.FULL))
            assertEquals(listOf(MediaId(2)), repository.entries.map { it.mediaId })
        }

    @Test
    fun `deletes the entry of a media sent to the system trash`() =
        runTest {
            val repository =
                FakeReviewRepository(
                    listOf(aReviewEntry(mediaId = 1, decision = TRASHED), aReviewEntry(mediaId = 2, decision = KEPT)),
                )
            gallery.remove(MediaId(1))

            CleanOrphanEntries(repository, gallery)(MediaAccess.FULL)

            assertEquals(listOf(MediaId(2)), repository.entries.map { it.mediaId })
        }

    @Test
    fun `finds nothing more to clean on a second pass`() =
        runTest {
            val repository = FakeReviewRepository(listOf(aReviewEntry(mediaId = 1), aReviewEntry(mediaId = 2)))
            gallery.remove(MediaId(1))
            val clean = CleanOrphanEntries(repository, gallery)

            clean(MediaAccess.FULL)
            val remaining = repository.entries

            assertEquals(0, clean(MediaAccess.FULL))
            assertEquals(remaining, repository.entries)
        }

    @Test
    fun `does nothing when no entry is stored`() =
        runTest {
            assertEquals(0, CleanOrphanEntries(FakeReviewRepository(), gallery)(MediaAccess.FULL))
        }

    @ParameterizedTest
    @EnumSource(value = MediaAccess::class, names = ["PARTIAL", "NONE"])
    fun `deletes nothing without full access to the gallery`(access: MediaAccess) =
        runTest {
            val entries = listOf(aReviewEntry(mediaId = 1), aReviewEntry(mediaId = 9))
            val repository = FakeReviewRepository(entries)

            assertEquals(0, CleanOrphanEntries(repository, gallery)(access))
            assertEquals(entries, repository.entries)
        }
}
