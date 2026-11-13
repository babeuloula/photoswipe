package fr.babeuloula.photoswipe.tile

import android.Manifest
import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.link.LaunchRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/** Tuile « PhotoSwipe » des réglages rapides (PS-172) : déclaration, appui, verrouillage. */
@RunWith(RobolectricTestRunner::class)
class SortTodayTileServiceTest {
    private val application = RuntimeEnvironment.getApplication()

    /** Tuile qui retient l'intent lancé au lieu de le confier au service système. */
    class RecordingTileService : SortTodayTileService() {
        val started = mutableListOf<Intent>()

        override fun collapseAndStart(intent: Intent) {
            started += intent
        }
    }

    private val service = Robolectric.buildService(RecordingTileService::class.java).create().get()

    @Test
    fun `a tap opens the deck of today through its link`() {
        service.onClick()

        val intent = service.started.single()
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(MainActivity::class.java.name, intent.component?.className)
        assertEquals(LaunchRequest.TodayDeck, MainActivity.launchRequestOf(intent))
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun `asks to unlock the phone before opening the deck`() {
        shadowOf(service).setLocked(true)

        service.onClick()

        // L'ombre de Robolectric déverrouille puis lance l'action demandée.
        assertFalse(service.isLocked)
        assertEquals(1, service.started.size)
    }

    @Test
    fun `shows an inactive tile without any counter`() {
        service.onStartListening()

        val tile = service.qsTile
        assertEquals(Tile.STATE_INACTIVE, tile.state)
        assertEquals(application.getString(R.string.tile_subtitle), tile.subtitle)
    }

    @Test
    fun `is declared as a quick settings tile with the notification icon`() {
        val query = Intent(TileService.ACTION_QS_TILE).setPackage(application.packageName)

        val info =
            application.packageManager
                .queryIntentServices(query, 0)
                .single()
                .serviceInfo

        assertEquals(SortTodayTileService::class.java.name, info.name)
        assertEquals(Manifest.permission.BIND_QUICK_SETTINGS_TILE, info.permission)
        assertEquals(R.drawable.ic_notification, info.icon)
        assertEquals(R.string.app_name, info.labelRes)
        assertTrue(info.exported)
    }
}
