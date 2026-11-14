package fr.babeuloula.photoswipe.permission

import android.Manifest
import android.app.Application
import android.os.Build
import fr.babeuloula.photoswipe.domain.model.MediaAccess
import fr.babeuloula.photoswipe.platform.PlatformFeatures
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Lecture de l'accès aux médias à partir des permissions accordées (Robolectric, API 34 et Android 10). */
@RunWith(RobolectricTestRunner::class)
class AndroidMediaAccessReaderTest {
    private val application: Application = RuntimeEnvironment.getApplication()
    private val reader = AndroidMediaAccessReader(application, PlatformFeatures.current())

    @Test
    fun `reads no access without any media permission`() {
        assertEquals(MediaAccess.NONE, reader.currentAccess())
    }

    @Test
    fun `reads full access with the image and video permissions`() {
        shadowOf(
            application,
        ).grantPermissions(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)

        assertEquals(MediaAccess.FULL, reader.currentAccess())
    }

    @Test
    fun `reads partial access with the user selection only`() {
        shadowOf(application).grantPermissions(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)

        assertEquals(MediaAccess.PARTIAL, reader.currentAccess())
    }

    @Test
    fun `does not read full access with images only`() {
        shadowOf(application).grantPermissions(Manifest.permission.READ_MEDIA_IMAGES)

        assertEquals(MediaAccess.NONE, reader.currentAccess())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `reads full access with the storage permissions on android 10`() {
        shadowOf(application).grantPermissions(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )

        assertEquals(MediaAccess.FULL, reader.currentAccess())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `reads no access without the storage permission on android 10`() {
        assertEquals(MediaAccess.NONE, reader.currentAccess())
    }

    @Test
    fun `reads full access with the storage permission on android 12, given its features`() {
        shadowOf(application).grantPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)
        val android12 = AndroidMediaAccessReader(application, PlatformFeatures.forSdk(Build.VERSION_CODES.S))

        assertEquals(MediaAccess.FULL, android12.currentAccess())
    }
}
