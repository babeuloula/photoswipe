package fr.babeuloula.photoswipe.data.deletion

import androidx.room.Room
import fr.babeuloula.photoswipe.data.database.PhotoSwipeDatabase
import fr.babeuloula.photoswipe.domain.fixtures.aConfirmedDeletion
import fr.babeuloula.photoswipe.domain.model.ConfirmedDeletion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.time.Instant

/** [RoomDeletionHistory] sur une vraie base Room en mémoire (Robolectric). */
@RunWith(RobolectricTestRunner::class)
class RoomDeletionHistoryTest {
    private val database =
        Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), PhotoSwipeDatabase::class.java).build()
    private val history = RoomDeletionHistory(database.confirmedDeletionDao())

    @After
    fun close() {
        database.close()
    }

    @Test
    fun `reads back the recorded deletions, with or without their day, in order`() =
        runTest {
            val deletions =
                listOf(
                    aConfirmedDeletion(mediaId = 2, sizeBytes = 1_500),
                    aConfirmedDeletion(mediaId = 1, day = null, confirmedAt = Instant.parse("2026-07-15T19:30:00Z")),
                )

            history.record(deletions)
            history.record(emptyList())

            assertEquals(deletions, history.observe().first())
        }

    @Test
    fun `forgets only the deletions confirmed before the given instant`() =
        runTest {
            val limit = Instant.parse("2026-07-10T00:00:00Z")
            history.record(
                listOf(
                    aConfirmedDeletion(mediaId = 1, confirmedAt = limit.minusMillis(1)),
                    aConfirmedDeletion(mediaId = 2, confirmedAt = limit),
                ),
            )

            history.forgetConfirmedBefore(limit)

            assertEquals(
                listOf(2L),
                history
                    .observe()
                    .first()
                    .map(ConfirmedDeletion::mediaId)
                    .map { it.value },
            )
        }
}
