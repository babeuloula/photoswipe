package fr.babeuloula.photoswipe.reminder

import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Le manifeste abonne [ReminderRescheduleReceiver] aux changements d'heure et de fuseau. */
@RunWith(RobolectricTestRunner::class)
class ReminderRescheduleReceiverTest {
    private val application = RuntimeEnvironment.getApplication()

    private fun receiversFor(action: String): List<String> =
        application.packageManager
            .queryBroadcastReceivers(
                Intent(action).setPackage(application.packageName),
                PackageManager.ResolveInfoFlags.of(0),
            ).map { it.activityInfo.name }

    @Test
    fun `listens to time and time zone changes`() {
        val receiver = ReminderRescheduleReceiver::class.java.name

        assertEquals(listOf(receiver), receiversFor(Intent.ACTION_TIME_CHANGED))
        assertEquals(listOf(receiver), receiversFor(Intent.ACTION_TIMEZONE_CHANGED))
    }

    @Test
    fun `reschedules after a reboot and when the exact alarm permission changes`() {
        val receiver = ReminderRescheduleReceiver::class.java.name

        assertEquals(listOf(receiver), receiversFor(Intent.ACTION_BOOT_COMPLETED))
        assertEquals(
            listOf(receiver),
            receiversFor(AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED),
        )
    }

    @Test
    fun `receives the exact reminder alarms`() {
        val names =
            application.packageManager
                .queryBroadcastReceivers(
                    Intent().setClassName(application, ALARM_RECEIVER),
                    PackageManager.ResolveInfoFlags.of(0),
                ).map { it.activityInfo.name }

        assertEquals(listOf(ALARM_RECEIVER), names)
    }

    private companion object {
        /** Récepteur des alarmes, dans `:data` (`:app` n'importe `:data` que depuis `di`). */
        const val ALARM_RECEIVER = "fr.babeuloula.photoswipe.data.reminder.ReminderAlarmReceiver"
    }
}
