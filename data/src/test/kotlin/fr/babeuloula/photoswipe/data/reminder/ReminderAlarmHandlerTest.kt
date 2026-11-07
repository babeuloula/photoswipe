package fr.babeuloula.photoswipe.data.reminder

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

/** Ce que fait une alarme du rappel (PS-125). */
class ReminderAlarmHandlerTest {
    private val calls = mutableListOf<String>()
    private var failing = false

    private val handler =
        ReminderAlarmHandler(
            sendReminder = {
                calls += "send"
                if (failing) throw IOException("gallery unavailable")
            },
            rescheduleDaily = { calls += "reschedule" },
            retryLater = { calls += "retry" },
        )

    @Test
    fun `sends the daily reminder, then schedules tomorrow's`() =
        runTest {
            handler.handle(ReminderAlarm.DAILY)

            assertEquals(listOf("send", "reschedule"), calls)
        }

    @Test
    fun `sends the snoozed reminder only once`() =
        runTest {
            handler.handle(ReminderAlarm.SNOOZE)

            assertEquals(listOf("send"), calls)
        }

    @Test
    fun `retries later through WorkManager when the gallery cannot be read, and still schedules tomorrow`() =
        runTest {
            failing = true

            handler.handle(ReminderAlarm.DAILY)

            assertEquals(listOf("send", "retry", "reschedule"), calls)
        }

    @Test
    fun `recognizes only the reminder alarms`() {
        assertEquals(ReminderAlarm.DAILY, ReminderAlarm.of(ReminderAlarm.DAILY.action))
        assertNull(ReminderAlarm.of("android.intent.action.BOOT_COMPLETED"))
    }
}
