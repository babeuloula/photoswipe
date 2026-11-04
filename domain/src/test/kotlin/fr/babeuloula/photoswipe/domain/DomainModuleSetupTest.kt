package fr.babeuloula.photoswipe.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Garde-fou du classpath : le module domaine doit rester du Kotlin JVM pur (SPEC §5.1).
 * Il complète les règles Konsist de `:architecture-tests`, qui vérifient les imports du code.
 */
class DomainModuleSetupTest {
    @Test
    fun `android framework is not on the domain classpath`() {
        assertThrows<ClassNotFoundException> {
            Class.forName("android.content.Context")
        }
    }
}
