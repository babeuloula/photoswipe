package fr.babeuloula.photoswipe.data.reminder

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
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** [WorkManagerReminderScheduler] sur le WorkManager de test (Robolectric). */
@RunWith(RobolectricTestRunner::class)
class WorkManagerReminderSchedulerTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val clock = FixedClock.at(LocalDateTime.of(2026, 7, 15, 9, 0))
    private lateinit var workManager: WorkManager
    private lateinit var scheduler: WorkManagerReminderScheduler

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        workManager = WorkManager.getInstance(context)
        scheduler = WorkManagerReminderScheduler({ workManager }, clock)
    }

    private fun reminderWork(): List<WorkInfo> =
        workManager.getWorkInfosForUniqueWork(WorkManagerReminderScheduler.WORK_NAME).get()

    @Test
    fun `schedules a daily task whose first run falls at the reminder time`() {
        scheduler.schedule(LocalTime.of(18, 0))

        val work = reminderWork().single()
        assertEquals(WorkInfo.State.ENQUEUED, work.state)
        assertEquals(TimeUnit.DAYS.toMillis(1), work.periodicityInfo!!.repeatIntervalMillis)
        assertEquals(Duration.ofHours(9).toMillis(), work.initialDelayMillis)
    }

    @Test
    fun `replaces the previous schedule when the time changes`() {
        scheduler.schedule(LocalTime.of(18, 0))

        scheduler.schedule(LocalTime.of(10, 30))

        val active = reminderWork().filter { it.state == WorkInfo.State.ENQUEUED }
        assertEquals(Duration.ofMinutes(90).toMillis(), active.single().initialDelayMillis)
    }

    @Test
    fun `cancels the reminder`() {
        scheduler.schedule(LocalTime.of(18, 0))

        scheduler.cancel()

        assertTrue(reminderWork().all { it.state == WorkInfo.State.CANCELLED })
    }

    private fun snoozeWork(): List<WorkInfo> =
        workManager.getWorkInfosForUniqueWork(WorkManagerReminderScheduler.SNOOZE_WORK_NAME).get()

    @Test
    fun `schedules today's snoozed reminder as a separate one-off task`() {
        scheduler.schedule(LocalTime.of(18, 0))

        scheduler.scheduleOnce(LocalDateTime.of(2026, 7, 15, 11, 30))

        val snoozed = snoozeWork().single()
        assertEquals(WorkInfo.State.ENQUEUED, snoozed.state)
        assertEquals(null, snoozed.periodicityInfo)
        assertEquals(Duration.ofMinutes(150).toMillis(), snoozed.initialDelayMillis)
        assertEquals(WorkInfo.State.ENQUEUED, reminderWork().single().state)
    }

    @Test
    fun `replaces the previous snooze`() {
        scheduler.scheduleOnce(LocalDateTime.of(2026, 7, 15, 11, 30))

        scheduler.scheduleOnce(LocalDateTime.of(2026, 7, 15, 10, 0))

        val active = snoozeWork().filter { it.state == WorkInfo.State.ENQUEUED }
        assertEquals(Duration.ofHours(1).toMillis(), active.single().initialDelayMillis)
    }

    @Test
    fun `cancels the snoozed reminder with the daily one`() {
        scheduler.schedule(LocalTime.of(18, 0))
        scheduler.scheduleOnce(LocalDateTime.of(2026, 7, 15, 11, 30))

        scheduler.cancel()

        assertTrue(snoozeWork().all { it.state == WorkInfo.State.CANCELLED })
        assertTrue(reminderWork().all { it.state == WorkInfo.State.CANCELLED })
    }
}
