package fr.babeuloula.photoswipe.reminder

import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate

/** [AndroidReminderNotifier] sur le gestionnaire de notifications de Robolectric. */
@RunWith(RobolectricTestRunner::class)
class AndroidReminderNotifierTest {
    private val application = RuntimeEnvironment.getApplication()
    private val manager = application.getSystemService(NotificationManager::class.java)
    private var allowed = true
    private val notifier = AndroidReminderNotifier(application) { allowed }

    private fun posted(): List<Notification> = shadowOf(manager).allNotifications

    @Test
    fun `reads no deck to open from an ordinary launch`() {
        assertNull(MainActivity.deckDayOf(Intent(application, MainActivity::class.java)))
        assertNull(MainActivity.deckDayOf(null))
    }

    private val day = LocalDate.of(2026, 11, 6)

    @Test
    fun `posts the reminder in its own channel and opens the deck of the day on tap`() {
        notifier.notify(3, day)

        val notification = posted().single()
        assertEquals(AndroidReminderNotifier.CHANNEL_ID, notification.channelId)
        assertEquals("3 memories to sort today", notification.extras.getString(Notification.EXTRA_TITLE))
        val tap = shadowOf(notification.contentIntent).savedIntent
        assertEquals(MainActivity::class.java.name, tap.component?.className)
        assertEquals(day, MainActivity.deckDayOf(tap))
        assertTrue(notification.flags and Notification.FLAG_AUTO_CANCEL != 0)
        assertEquals("Daily reminder", manager.getNotificationChannel(AndroidReminderNotifier.CHANNEL_ID).name)
    }

    @Test
    fun `offers to reschedule today's reminder from the notification`() {
        notifier.notify(3, day)

        val action = posted().single().actions.single()
        assertEquals("Reschedule", action.title.toString())
        val snooze = shadowOf(action.actionIntent).savedIntent
        assertEquals(SnoozeReminderActivity::class.java.name, snooze.component?.className)
    }

    @Test
    @Config(qualifiers = "fr")
    fun `writes the reminder in French with its plural`() {
        notifier.notify(1, day)
        notifier.notify(4, day)

        assertEquals("4 souvenirs à trier aujourd'hui", posted().single().extras.getString(Notification.EXTRA_TITLE))
    }

    @Test
    fun `cites the day and the years of the memories, in the app color`() {
        notifier.notify(3, day, years = 2016..2025)

        val notification = posted().single()
        assertEquals(
            "Photos and videos taken on November 6, from 2016 to 2025",
            notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString(),
        )
        assertEquals(application.getColor(R.color.notification_accent), notification.color)
    }

    @Test
    @Config(qualifiers = "fr")
    fun `cites a single year in French`() {
        notifier.notify(2, day, years = 2025..2025)

        assertEquals(
            "Des photos et vidéos prises un 6 novembre, en 2025",
            posted()
                .single()
                .extras
                .getCharSequence(Notification.EXTRA_TEXT)
                .toString(),
        )
    }

    @Test
    fun `keeps the general text when the years are unknown`() {
        notifier.notify(3, day)

        assertEquals(
            application.getString(R.string.reminder_text),
            posted()
                .single()
                .extras
                .getCharSequence(Notification.EXTRA_TEXT)
                .toString(),
        )
    }

    @Test
    fun `posts nothing without the notification permission`() {
        allowed = false

        notifier.notify(3, day)

        assertTrue(posted().isEmpty())
    }
}
