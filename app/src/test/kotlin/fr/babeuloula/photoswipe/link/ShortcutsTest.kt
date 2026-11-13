package fr.babeuloula.photoswipe.link

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.AdaptiveIconDrawable
import android.net.Uri
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.xmlpull.v1.XmlPullParser

/** Raccourcis de l'appui long sur l'icône et filtre des liens profonds (PS-171, ADR 0038). */
@RunWith(RobolectricTestRunner::class)
class ShortcutsTest {
    private val application = RuntimeEnvironment.getApplication()

    /** Un raccourci tel que `res/xml/shortcuts.xml` le déclare. */
    private data class Shortcut(
        val id: String,
        val icon: Int,
        val shortLabel: Int,
        val longLabel: Int,
        val action: String?,
        val data: String?,
        val targetPackage: String?,
        val targetClass: String?,
    )

    private val shortcuts: List<Shortcut> by lazy {
        val parser = application.resources.getXml(R.xml.shortcuts)
        val found = mutableListOf<Shortcut>()
        var current: Shortcut? = null
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            when (parser.name) {
                "shortcut" -> {
                    current =
                        Shortcut(
                            id = parser.getAttributeValue(ANDROID, "shortcutId"),
                            icon = parser.getAttributeResourceValue(ANDROID, "icon", 0),
                            shortLabel = parser.getAttributeResourceValue(ANDROID, "shortcutShortLabel", 0),
                            longLabel = parser.getAttributeResourceValue(ANDROID, "shortcutLongLabel", 0),
                            action = null,
                            data = null,
                            targetPackage = null,
                            targetClass = null,
                        )
                }

                "intent" -> {
                    found +=
                        checkNotNull(current).copy(
                            action = parser.getAttributeValue(ANDROID, "action"),
                            data = parser.getAttributeValue(ANDROID, "data"),
                            targetPackage = parser.getAttributeValue(ANDROID, "targetPackage"),
                            targetClass = parser.getAttributeValue(ANDROID, "targetClass"),
                        )
                }
            }
        }
        found
    }

    @Test
    fun `declares the sort today and summary shortcuts on the launcher activity`() {
        val launcher =
            application.packageManager.getActivityInfo(
                ComponentName(application, MainActivity::class.java),
                PackageManager.GET_META_DATA,
            )

        assertEquals(R.xml.shortcuts, launcher.metaData.getInt("android.app.shortcuts"))
        assertEquals(listOf("sort_today", "summary"), shortcuts.map { it.id })
    }

    @Test
    fun `each shortcut opens its deep link in the main activity`() {
        assertEquals(
            listOf(LaunchRequest.TodayDeck, LaunchRequest.Summary),
            shortcuts.map { DeepLinks.parse(checkNotNull(it.data)) },
        )
        shortcuts.forEach { shortcut ->
            assertEquals(Intent.ACTION_VIEW, shortcut.action)
            assertEquals(application.packageName, shortcut.targetPackage)
            assertEquals(MainActivity::class.java.name, shortcut.targetClass)
        }
    }

    @Test
    fun `each shortcut has its labels and an adaptive icon`() {
        shortcuts.forEach { shortcut ->
            assertTrue(application.getString(shortcut.shortLabel).isNotBlank())
            assertTrue(application.getString(shortcut.longLabel).isNotBlank())
            val icon = application.getDrawable(shortcut.icon) as AdaptiveIconDrawable
            assertNotNull(icon.foreground)
            assertNotNull(icon.monochrome)
        }
    }

    @Test
    fun `the main activity receives the photoswipe links`() {
        val view = Intent(Intent.ACTION_VIEW, Uri.parse(DeepLinks.TODAY)).setPackage(application.packageName)

        val handlers = application.packageManager.queryIntentActivities(view, 0)

        assertEquals(listOf(MainActivity::class.java.name), handlers.map { it.activityInfo.name })
    }

    private companion object {
        const val ANDROID = "http://schemas.android.com/apk/res/android"
    }
}
