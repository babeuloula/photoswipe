package fr.babeuloula.photoswipe.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.container.KoScope
import com.lemonappdev.konsist.api.declaration.KoImportDeclaration
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Règles de dépendance entre modules appliquées au vrai code (SPEC §5.1, docs/architecture.md §1) :
 * les dépendances vont toujours vers `:domain`, et `:app` ne touche à `:data` que pour le câblage DI.
 */
class ModuleDependencyTest {
    @Test
    fun `domain imports nothing from android, room, compose or the outer modules`() {
        // Production et tests : un test du domaine qui aurait besoin d'Android serait déjà un signal.
        val domain = Konsist.scopeFromModule("domain")

        assertNoViolation(domain, domain.forbiddenDomainImports())
    }

    @Test
    fun `app uses data classes only from the di package`() {
        // Production seulement : un test peut légitimement remplacer une implémentation de :data.
        val app = Konsist.scopeFromProduction("app")

        assertNoViolation(app, app.dataImportsOutsideDi())
    }

    private fun assertNoViolation(
        scope: KoScope,
        violations: List<KoImportDeclaration>,
    ) {
        // Un scope vide (module renommé, chemin faux) rendrait la règle toujours vraie.
        assertTrue(scope.files.isNotEmpty()) { "The scanned module has no Kotlin file" }
        assertTrue(violations.isEmpty()) {
            violations.joinToString(prefix = "Forbidden imports:\n", separator = "\n") { it.location }
        }
    }
}
