package fr.babeuloula.photoswipe.data.activity

import android.content.Context
import androidx.room.Room
import fr.babeuloula.photoswipe.data.database.PhotoSwipeDatabase
import fr.babeuloula.photoswipe.domain.model.DailyActivity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.time.LocalDate

/** [RoomActivityHistory] sur une vraie base Room en mémoire (Robolectric). */
@RunWith(RobolectricTestRunner::class)
class RoomActivityHistoryTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val database = Room.inMemoryDatabaseBuilder(context, PhotoSwipeDatabase::class.java).build()
    private val history = RoomActivityHistory(database.dailyActivityDao())
    private val july14 = LocalDate.of(2026, 7, 14)
    private val july15 = LocalDate.of(2026, 7, 15)

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `starts empty`() =
        runTest {
            assertEquals(emptyList<DailyActivity>(), history.since(LocalDate.of(1970, 1, 1)))
            assertNull(history.firstDay())
        }

    @Test
    fun `adds up the confirmed deletions of a day`() =
        runTest {
            history.recordTrashed(july15, count = 2, bytes = 1_500)
            history.recordTrashed(july15, count = 1, bytes = 500)

            assertEquals(listOf(DailyActivity(july15, trashedCount = 3, freedBytes = 2_000)), history.since(july15))
        }

    @Test
    fun `returns the days from the given one, oldest first, and the first recorded day`() =
        runTest {
            history.recordTrashed(july15, count = 1, bytes = 10)
            history.recordTrashed(july14, count = 2, bytes = 20)

            assertEquals(listOf(july14, july15), history.since(july14).map { it.day })
            assertEquals(listOf(july15), history.since(july15).map { it.day })
            assertEquals(july14, history.firstDay())
        }

    @Test
    fun `removes restored media from the day they were confirmed, never below zero`() =
        runTest {
            history.recordTrashed(july15, count = 3, bytes = 3_000)

            history.removeTrashed(july15, count = 1, bytes = 1_000)
            assertEquals(listOf(DailyActivity(july15, trashedCount = 2, freedBytes = 2_000)), history.since(july15))

            history.removeTrashed(july15, count = 5, bytes = 9_000)
            history.removeTrashed(july14, count = 1, bytes = 10)
            assertEquals(listOf(DailyActivity(july15)), history.since(july14))
        }
}
