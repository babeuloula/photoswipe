package fr.babeuloula.photoswipe.platform

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Fonctions d'Android disponibles selon la version, d'Android 10 (notre `minSdk`) à Android 14 (PS-181). */
@RunWith(RobolectricTestRunner::class)
class PlatformFeaturesTest {
    @Test
    fun `offers nothing beyond the gallery on android 10`() {
        assertEquals(
            PlatformFeatures(
                systemTrash = false,
                galleryFavorites = false,
                mediaManagement = false,
                exactAlarmPermission = false,
                notificationPermission = false,
                appLanguageSetting = false,
                granularMediaPermissions = false,
                partialMediaAccess = false,
            ),
            PlatformFeatures.forSdk(Build.VERSION_CODES.Q),
        )
    }

    @Test
    fun `adds the gallery trash and favorites on android 11`() {
        val features = PlatformFeatures.forSdk(Build.VERSION_CODES.R)

        assertEquals(true, features.systemTrash)
        assertEquals(true, features.galleryFavorites)
        assertEquals(false, features.mediaManagement)
        assertEquals(false, features.exactAlarmPermission)
    }

    @Test
    fun `adds media management and the exact alarm permission on android 12`() {
        val features = PlatformFeatures.forSdk(Build.VERSION_CODES.S)

        assertEquals(true, features.mediaManagement)
        assertEquals(true, features.exactAlarmPermission)
        assertEquals(false, features.notificationPermission)
        assertEquals(false, features.appLanguageSetting)
        assertEquals(false, features.granularMediaPermissions)
    }

    @Test
    fun `adds the notification permission, app language and media permissions by type on android 13`() {
        val features = PlatformFeatures.forSdk(Build.VERSION_CODES.TIRAMISU)

        assertEquals(true, features.notificationPermission)
        assertEquals(true, features.appLanguageSetting)
        assertEquals(true, features.granularMediaPermissions)
        assertEquals(false, features.partialMediaAccess)
    }

    @Test
    fun `offers everything on android 14`() {
        val features = PlatformFeatures.forSdk(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)

        assertEquals(PlatformFeatures.forSdk(Build.VERSION_CODES.VANILLA_ICE_CREAM), features)
        assertEquals(true, features.partialMediaAccess)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `reads the features of the phone it runs on`() {
        assertEquals(PlatformFeatures.forSdk(Build.VERSION_CODES.Q), PlatformFeatures.current())
    }
}
