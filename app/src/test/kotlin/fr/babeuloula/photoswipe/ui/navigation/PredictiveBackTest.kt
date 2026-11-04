package fr.babeuloula.photoswipe.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Retour prédictif activé (SPEC §8) : l'attribut est lu dans le manifeste, car Robolectric ne
 * l'expose pas (`ApplicationInfo.isOnBackInvokedCallbackEnabled` est une API cachée).
 */
class PredictiveBackTest {
    @Test
    fun `enables predictive back for the whole application`() {
        val manifest =
            DocumentBuilderFactory
                .newInstance()
                .apply { isNamespaceAware = true }
                .newDocumentBuilder()
                .parse(File("src/main/AndroidManifest.xml"))
        val application = manifest.getElementsByTagName("application").item(0) as org.w3c.dom.Element

        assertEquals("true", application.getAttributeNS(ANDROID_NAMESPACE, "enableOnBackInvokedCallback"))
    }

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
    }
}
