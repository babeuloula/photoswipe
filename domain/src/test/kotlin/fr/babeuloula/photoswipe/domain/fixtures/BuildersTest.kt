package fr.babeuloula.photoswipe.domain.fixtures

import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaType
import fr.babeuloula.photoswipe.domain.model.ReviewDecision
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Duration

class BuildersTest {
    @Test
    fun `builds a deterministic image by default`() {
        val media = aMediaItem()

        assertEquals(MediaId(1), media.id)
        assertEquals(MediaType.IMAGE, media.type)
        assertEquals(DEFAULT_TAKEN_AT, media.takenAt)
        assertEquals(DEFAULT_SIZE_BYTES, media.sizeBytes)
        assertNull(media.duration)
        assertEquals(aMediaItem(), media)
    }

    @Test
    fun `derives the content uri from the id and the media type`() {
        assertEquals("content://media/external/images/media/42", aMediaItem(id = 42).uri)
        assertEquals(
            "content://media/external/video/media/7",
            aMediaItem(id = 7, type = MediaType.VIDEO, duration = Duration.ofSeconds(30)).uri,
        )
    }

    @Test
    fun `builds a kept review entry without pending favorite by default`() {
        val entry = aReviewEntry()

        assertEquals(MediaId(1), entry.mediaId)
        assertEquals(DEFAULT_REVIEW_YEAR, entry.reviewYear)
        assertEquals(ReviewDecision.KEPT, entry.decision)
        assertEquals(DEFAULT_DECIDED_AT, entry.decidedAt)
        assertFalse(entry.favoritePending)
    }
}
