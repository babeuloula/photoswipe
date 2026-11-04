package fr.babeuloula.photoswipe.permission

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Permissions demandées sur Android 14 (API 34 de Robolectric). */
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
}
