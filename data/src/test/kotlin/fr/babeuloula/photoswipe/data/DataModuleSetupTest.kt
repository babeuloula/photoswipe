package fr.babeuloula.photoswipe.data

import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Vérifie que le module données est bien une bibliothèque Android : il doit voir le framework
 * pour implémenter MediaStore, Room, etc. (SPEC §5.3).
 */
class DataModuleSetupTest {
    @Test
    fun `android framework is on the data classpath`() {
        assertNotNull(Class.forName("android.content.Context"))
    }
}
