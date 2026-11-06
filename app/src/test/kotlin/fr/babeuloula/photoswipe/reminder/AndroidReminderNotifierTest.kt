package fr.babeuloula.photoswipe.reminder

import android.app.Notification
import android.app.NotificationManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** [AndroidReminderNotifier] sur le gestionnaire de notifications de Robolectric. */
@RunWith(RobolectricTestRunner::class)
class AndroidReminderNotifierTest {
    private val application = RuntimeEnvironment.getApplication()
    private val manager = application.getSystemService(NotificationManager::class.java)
    private var allowed = true
    private val notifier = AndroidReminderNotifier(application) { allowed }

    private fun posted(): List<Notification> = shadowOf(manager).allNotifications

    @Test
    fun `posts the reminder in its own channel and opens the app on tap`() {
        notifier.notify(3)

        val notification = posted().single()
        assertEquals(AndroidReminderNotifier.CHANNEL_ID, notification.channelId)
        assertEquals("3 memories to sort", notification.extras.getString(Notification.EXTRA_TITLE))
        assertNotNull(notification.contentIntent)
        assertTrue(notification.flags and Notification.FLAG_AUTO_CANCEL != 0)
        assertEquals("Daily reminder", manager.getNotificationChannel(AndroidReminderNotifier.CHANNEL_ID).name)
    }

    @Test
    @Config(qualifiers = "fr")
    fun `writes the reminder in French with its plural`() {
        notifier.notify(1)
        notifier.notify(4)

        assertEquals("4 souvenirs à trier", posted().single().extras.getString(Notification.EXTRA_TITLE))
    }

    @Test
    fun `posts nothing without the notification permission`() {
        allowed = false

        notifier.notify(3)

        assertTrue(posted().isEmpty())
    }
}
