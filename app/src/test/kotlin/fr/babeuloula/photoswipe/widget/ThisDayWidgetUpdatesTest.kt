package fr.babeuloula.photoswipe.widget

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** [ThisDayWidgetUpdates] sur le WorkManager de test (Robolectric). */
@RunWith(RobolectricTestRunner::class)
class ThisDayWidgetUpdatesTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val clock = FixedClock.at(LocalDateTime.of(2026, 7, 15, 21, 30))
    private var placed = true
    private lateinit var workManager: WorkManager
    private lateinit var updates: ThisDayWidgetUpdates

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        workManager = WorkManager.getInstance(context)
        updates = ThisDayWidgetUpdates({ workManager }, clock) { placed }
    }

    private fun work(name: String): List<WorkInfo> = workManager.getWorkInfosForUniqueWork(name).get()

    @Test
    fun `schedules a daily update just after midnight`() {
        updates.scheduleDaily()

        val daily = work(ThisDayWidgetUpdates.DAILY_WORK_NAME).single()
        assertEquals(WorkInfo.State.ENQUEUED, daily.state)
        assertEquals(TimeUnit.DAYS.toMillis(1), daily.periodicityInfo!!.repeatIntervalMillis)
        assertEquals(Duration.ofMinutes(151).toMillis(), daily.initialDelayMillis)
    }

    @Test
    fun `refreshes the placed widgets after a sort`() {
        updates.refresh()

        val refresh = work(ThisDayWidgetUpdates.REFRESH_WORK_NAME).single()
        assertEquals(null, refresh.periodicityInfo)
        assertEquals(0L, refresh.initialDelayMillis)
    }

    @Test
    fun `does nothing after a sort when no widget is placed`() {
        placed = false

        updates.refresh()

        assertTrue(work(ThisDayWidgetUpdates.REFRESH_WORK_NAME).isEmpty())
    }

    @Test
    fun `cancels every update once the last widget is removed`() {
        updates.scheduleDaily()
        updates.refresh()

        updates.cancel()

        assertTrue(work(ThisDayWidgetUpdates.DAILY_WORK_NAME).all { it.state == WorkInfo.State.CANCELLED })
        assertTrue(work(ThisDayWidgetUpdates.REFRESH_WORK_NAME).all { it.state.isFinished })
    }
}
