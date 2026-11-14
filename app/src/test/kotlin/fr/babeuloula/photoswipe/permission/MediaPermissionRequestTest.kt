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
    fun `requests reading and writing the storage on android 10, which deletes without a trash`() {
        assertEquals(
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
            mediaPermissionRequest(PlatformFeatures.forSdk(Build.VERSION_CODES.Q)).toList(),
        )
    }

    @Test
    fun `requests only reading the storage on android 11 and 12`() {
        listOf(Build.VERSION_CODES.R, Build.VERSION_CODES.S_V2).forEach { sdk ->
            assertEquals(
                listOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                mediaPermissionRequest(PlatformFeatures.forSdk(sdk)).toList(),
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `requests the storage permission on an android 10 phone`() {
        val storage = listOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        assertEquals(storage, MEDIA_PERMISSION_REQUEST.toList())
        assertEquals(storage, MEDIA_PERMISSIONS.toList())
    }
}
