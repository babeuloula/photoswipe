package fr.babeuloula.photoswipe.data.reminder

import android.app.AlarmManager
import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager
import java.time.LocalDateTime
import java.time.LocalTime

/** Rappel à l'heure pile sur l'AlarmManager de Robolectric (PS-125). */
@RunWith(RobolectricTestRunner::class)
class AlarmManagerReminderSchedulerTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val clock = FixedClock.at(LocalDateTime.of(2026, 11, 7, 9, 0))
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private lateinit var workManager: WorkManager
    private lateinit var scheduler: AlarmManagerReminderScheduler

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        workManager = WorkManager.getInstance(context)
        scheduler = AlarmManagerReminderScheduler(context, clock, WorkManagerReminderScheduler({ workManager }, clock))
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
    }

    @After
    fun tearDown() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
    }

    private fun alarms() = shadowOf(alarmManager).scheduledAlarms

    private fun millis(dateTime: LocalDateTime): Long = dateTime.atZone(clock.zone).toInstant().toEpochMilli()

    private fun work(name: String): List<WorkInfo> =
        workManager.getWorkInfosForUniqueWork(name).get().filter { it.state == WorkInfo.State.ENQUEUED }

    @Test
    fun `rings the daily reminder at the exact minute, even while the phone sleeps`() {
        scheduler.schedule(LocalTime.of(18, 0))

        val alarm = alarms().single()
        assertEquals(millis(LocalDateTime.of(2026, 11, 7, 18, 0)), alarm.triggerAtMs)
        assertEquals(AlarmManager.RTC_WAKEUP, alarm.getType())
        assertTrue(alarm.isAllowWhileIdle)
        assertTrue(work(WorkManagerReminderScheduler.WORK_NAME).isEmpty())
    }

    @Test
    fun `rings tomorrow when today's time has passed, and replaces the previous alarm`() {
        scheduler.schedule(LocalTime.of(18, 0))

        scheduler.schedule(LocalTime.of(8, 30))

        assertEquals(millis(LocalDateTime.of(2026, 11, 8, 8, 30)), alarms().single().triggerAtMs)
    }

    @Test
    fun `rings at the exact millisecond, then tomorrow once the reminder went off`() {
        clock.setTo(LocalDateTime.of(2026, 11, 7, 8, 57, 30, 123_456_789))
        scheduler.schedule(LocalTime.of(8, 58))
        assertEquals(millis(LocalDateTime.of(2026, 11, 7, 8, 58)), alarms().single().triggerAtMs)

        // L'alarme sonne : le rappel quotidien est replanifié pour le lendemain, pas pour aujourd'hui.
        clock.setTo(LocalDateTime.of(2026, 11, 7, 8, 58))
        scheduler.schedule(LocalTime.of(8, 58))

        assertEquals(millis(LocalDateTime.of(2026, 11, 8, 8, 58)), alarms().single().triggerAtMs)
    }

    @Test
    fun `rings today's snoozed reminder beside the daily one`() {
        scheduler.schedule(LocalTime.of(18, 0))

        scheduler.scheduleOnce(LocalDateTime.of(2026, 11, 7, 9, 5))

        assertEquals(
            setOf(millis(LocalDateTime.of(2026, 11, 7, 18, 0)), millis(LocalDateTime.of(2026, 11, 7, 9, 5))),
            alarms().map { it.triggerAtMs }.toSet(),
        )
    }

    @Test
    fun `cancels both alarms`() {
        scheduler.schedule(LocalTime.of(18, 0))
        scheduler.scheduleOnce(LocalDateTime.of(2026, 11, 7, 9, 5))

        scheduler.cancel()

        assertTrue(alarms().isEmpty())
    }

    @Test
    fun `falls back on WorkManager without the exact alarm permission`() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)

        scheduler.schedule(LocalTime.of(18, 0))
        scheduler.scheduleOnce(LocalDateTime.of(2026, 11, 7, 9, 5))

        assertTrue(alarms().isEmpty())
        assertEquals(1, work(WorkManagerReminderScheduler.WORK_NAME).size)
        assertEquals(1, work(WorkManagerReminderScheduler.SNOOZE_WORK_NAME).size)
    }

    @Test
    fun `moves from WorkManager to an exact alarm once the permission is given`() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        scheduler.schedule(LocalTime.of(18, 0))

        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        scheduler.schedule(LocalTime.of(18, 0))

        assertEquals(1, alarms().size)
        assertTrue(work(WorkManagerReminderScheduler.WORK_NAME).isEmpty())
    }
}
