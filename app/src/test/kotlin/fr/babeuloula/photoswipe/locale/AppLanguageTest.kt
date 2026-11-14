package fr.babeuloula.photoswipe.locale

import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import android.provider.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.Locale

/** Langue de l'application et réglage d'Android 13+ (Robolectric, API 34). */
@RunWith(RobolectricTestRunner::class)
class AppLanguageTest {
    private val application = RuntimeEnvironment.getApplication()

    @Test
    fun `follows the phone language until a language is chosen for the app`() {
        assertNull(appLanguage(application))

        application.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList(Locale.FRENCH)

        assertEquals(Locale.FRENCH, appLanguage(application))
    }

    @Test
    fun `names each language in itself`() {
        assertEquals("Français", Locale.FRENCH.nativeName())
        assertEquals("English", Locale.ENGLISH.nativeName())
    }

    @Test
    fun `opens the app language settings of PhotoSwipe`() {
        val intent = appLanguageSettingsIntent(application)

        assertEquals(Settings.ACTION_APP_LOCALE_SETTINGS, intent.action)
        assertEquals("package:${application.packageName}", intent.dataString)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `follows the phone language on android 10, which has no app language setting`() {
        assertNull(appLanguage(application))
    }
}
