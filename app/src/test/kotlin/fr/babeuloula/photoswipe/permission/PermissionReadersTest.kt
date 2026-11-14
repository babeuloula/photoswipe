package fr.babeuloula.photoswipe.permission

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.os.Build
import fr.babeuloula.photoswipe.platform.PlatformFeatures
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

/**
 * Lecture des permissions et accès qui n'existent pas sur toutes les versions d'Android
 * (PS-181) : sur Android 10, rien n'est demandé et rien ne plante.
 */
@RunWith(RobolectricTestRunner::class)
class PermissionReadersTest {
    private val application: Application = RuntimeEnvironment.getApplication()
    private val notificationManager = application.getSystemService(NotificationManager::class.java)

    @After
    fun tearDown() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
    }

    @Test
    fun `reads the notification permission from android 13`() {
        val reader = AndroidNotificationPermissionReader(application, PlatformFeatures.current())
        assertFalse(reader.isGranted())

        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        assertTrue(reader.isGranted())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `allows notifications on android 10 until they are turned off in the settings`() {
        val reader = AndroidNotificationPermissionReader(application, PlatformFeatures.current())
        assertTrue(reader.isGranted())

        shadowOf(notificationManager).setNotificationsEnabled(false)

        assertFalse(reader.isGranted())
    }

    @Test
    fun `reads the exact alarm permission from android 12`() {
        val reader = AndroidExactAlarmPermissionReader(application, PlatformFeatures.current())
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        assertFalse(reader.isGranted())

        ShadowAlarmManager.setCanScheduleExactAlarms(true)

        assertTrue(reader.isGranted())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `allows exact alarms on android 10, where they need no permission`() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)

        assertTrue(AndroidExactAlarmPermissionReader(application, PlatformFeatures.current()).isGranted())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `never grants media management on android 10, where it does not exist`() {
        assertFalse(AndroidMediaManagementReader(application, PlatformFeatures.current()).isGranted())
    }

    @Test
    fun `never grants media management when the phone does not offer it`() {
        val reader = AndroidMediaManagementReader(application, PlatformFeatures.forSdk(Build.VERSION_CODES.R))

        assertFalse(reader.isGranted())
    }
}
