package fr.babeuloula.photoswipe.domain.fixtures

import fr.babeuloula.photoswipe.domain.model.ConfirmedDeletion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

/** `FakeDeletionHistory` respecte le contrat de son port. */
class FakeDeletionHistoryTest {
    private val history = FakeDeletionHistory()

    @Test
    fun `starts empty and returns the deletions in the order they were recorded`() =
        runTest {
            assertEquals(emptyList<ConfirmedDeletion>(), history.observe().first())

            history.record(listOf(aConfirmedDeletion(mediaId = 2)))
            history.record(listOf(aConfirmedDeletion(mediaId = 1, day = null)))

            assertEquals(listOf(2L, 1L), history.observe().first().map { it.mediaId.value })
        }

    @Test
    fun `forgets only the deletions confirmed before the given instant`() =
        runTest {
            val limit = Instant.parse("2026-07-10T00:00:00Z")
            history.record(
                listOf(
                    aConfirmedDeletion(mediaId = 1, confirmedAt = limit.minusSeconds(1)),
                    aConfirmedDeletion(mediaId = 2, confirmedAt = limit),
                ),
            )

            history.forgetConfirmedBefore(limit)

            assertEquals(listOf(2L), history.observe().first().map { it.mediaId.value })
        }
}
