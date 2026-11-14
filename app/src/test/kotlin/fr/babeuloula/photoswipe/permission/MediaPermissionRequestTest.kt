package fr.babeuloula.photoswipe.permission

import android.Manifest
import android.os.Build
import fr.babeuloula.photoswipe.platform.PlatformFeatures
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Permissions demandées selon la version d'Android (PS-181). */
@RunWith(RobolectricTestRunner::class)
class MediaPermissionRequestTest {
    @Test
    fun `requests full access and lets the user share only a selection on android 14`() {
        assertEquals(
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            ),
            MEDIA_PERMISSION_REQUEST.toList(),
        )
    }

    @Test
    fun `requests images and videos without a selection on android 13`() {
        assertEquals(
            listOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO),
            mediaPermissionRequest(PlatformFeatures.forSdk(Build.VERSION_CODES.TIRAMISU)).toList(),
        )
    }

    @Test
    fun `requests the storage permission from android 10 to 12`() {
        listOf(Build.VERSION_CODES.Q, Build.VERSION_CODES.R, Build.VERSION_CODES.S_V2).forEach { sdk ->
            assertEquals(
                listOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                mediaPermissionRequest(PlatformFeatures.forSdk(sdk)).toList(),
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `requests the storage permission on an android 10 phone`() {
        assertEquals(listOf(Manifest.permission.READ_EXTERNAL_STORAGE), MEDIA_PERMISSION_REQUEST.toList())
        assertEquals(listOf(Manifest.permission.READ_EXTERNAL_STORAGE), MEDIA_PERMISSIONS.toList())
    }
}
