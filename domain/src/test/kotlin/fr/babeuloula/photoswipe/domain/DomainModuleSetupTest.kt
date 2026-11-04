package fr.babeuloula.photoswipe.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Garde-fou du squelette : le module domaine doit rester du Kotlin JVM pur (SPEC §5.1).
 * Le test d'architecture complet arrivera avec PS-7.
 */
class DomainModuleSetupTest {
    @Test
    fun `android framework is not on the domain classpath`() {
        assertThrows<ClassNotFoundException> {
            Class.forName("android.content.Context")
        }
    }
}
