package fr.babeuloula.photoswipe.domain.fixtures

import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaActionRequestFactory.FakeMediaActionRequest
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaActionRequestFactory.Kind
import fr.babeuloula.photoswipe.domain.model.MediaId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class FakeMediaActionRequestFactoryTest {
    @Test
    fun `splits the media into batches of at most the batch size in the order received`() {
        val media = listOf(5L, 1L, 4L, 2L, 3L).map { aMediaItem(id = it) }

        assertEquals(
            listOf(
                FakeMediaActionRequest(Kind.TRASH, ids(5, 1)),
                FakeMediaActionRequest(Kind.TRASH, ids(4, 2)),
                FakeMediaActionRequest(Kind.TRASH, ids(3)),
            ),
            FakeMediaActionRequestFactory(batchSize = 2).trashRequests(media),
        )
    }

    @Test
    fun `builds favorite requests the same way`() {
        assertEquals(
            listOf(FakeMediaActionRequest(Kind.FAVORITE, ids(1, 2))),
            FakeMediaActionRequestFactory().favoriteRequests(listOf(aMediaItem(id = 1), aMediaItem(id = 2))),
        )
    }

    @Test
    fun `builds restore and permanent deletion requests the same way`() {
        val media = listOf(aMediaItem(id = 1))

        assertEquals(
            listOf(FakeMediaActionRequest(Kind.RESTORE, ids(1))),
            FakeMediaActionRequestFactory().restoreRequests(media),
        )
        assertEquals(
            listOf(FakeMediaActionRequest(Kind.DELETE, ids(1))),
            FakeMediaActionRequestFactory().deleteRequests(media),
        )
    }

    @Test
    fun `returns no request for no media`() {
        assertEquals(emptyList<FakeMediaActionRequest>(), FakeMediaActionRequestFactory().trashRequests(emptyList()))
    }

    @Test
    fun `rejects a batch size that is not positive`() {
        assertThrows<IllegalArgumentException> { FakeMediaActionRequestFactory(batchSize = 0) }
    }

    private fun ids(vararg ids: Long): Set<MediaId> = ids.mapTo(LinkedHashSet()) { MediaId(it) }
}
