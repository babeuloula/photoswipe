package fr.babeuloula.photoswipe.reminder

import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import fr.babeuloula.photoswipe.MainActivity
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
        assertEquals("3 memories to sort", notification.extras.getString(Notification.EXTRA_TITLE))
        val tap = shadowOf(notification.contentIntent).savedIntent
        assertEquals(MainActivity::class.java.name, tap.component?.className)
        assertEquals(day, MainActivity.deckDayOf(tap))
        assertTrue(notification.flags and Notification.FLAG_AUTO_CANCEL != 0)
        assertEquals("Daily reminder", manager.getNotificationChannel(AndroidReminderNotifier.CHANNEL_ID).name)
    }

    @Test
    @Config(qualifiers = "fr")
    fun `writes the reminder in French with its plural`() {
        notifier.notify(1, day)
        notifier.notify(4, day)

        assertEquals("4 souvenirs à trier", posted().single().extras.getString(Notification.EXTRA_TITLE))
    }

    @Test
    fun `posts nothing without the notification permission`() {
        allowed = false

        notifier.notify(3, day)

        assertTrue(posted().isEmpty())
    }
}
