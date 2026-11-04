package fr.babeuloula.photoswipe.architecture

import com.lemonappdev.konsist.api.Konsist
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Vérifie que les règles d'architecture détectent bien les violations, sur des fichiers
 * d'exemple (`src/test/resources/fixtures`) qui ne sont jamais compilés. Sans ces tests,
 * une règle mal écrite laisserait passer silencieusement n'importe quel import.
 */
class ArchitectureRulesTest {
    @Test
    fun `flags android, room, compose, google android, data and app imports in the domain`() {
        val scope = Konsist.scopeFromFile("$FIXTURES/domain/ForbiddenDomainImports.kt")

        val violations = scope.forbiddenDomainImports().map { it.name }.toSet()

        assertEquals(
            setOf(
                "android.content.Context",
                "androidx.room.Entity",
                "androidx.compose.runtime.Composable",
                "com.google.android.material.snackbar.Snackbar",
                "fr.babeuloula.photoswipe.data.MediaStoreMediaSource",
                "fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme",
            ),
            violations,
        )
    }

    @Test
    fun `allows pure kotlin, java and domain imports in the domain`() {
        val scope = Konsist.scopeFromFile("$FIXTURES/domain/AllowedDomainImports.kt")

        assertEquals(emptyList<String>(), scope.forbiddenDomainImports().map { it.name })
    }

    @Test
    fun `flags data imports in app files outside the di package`() {
        val scope =
            Konsist.scopeFromFiles(
                listOf(
                    "$FIXTURES/app/ScreenImportingData.kt",
                    "$FIXTURES/app/LookalikeDiPackageImportingData.kt",
                ),
            )

        val violations = scope.dataImportsOutsideDi().map { it.containingFile.name }.toSet()

        assertEquals(setOf("ScreenImportingData", "LookalikeDiPackageImportingData"), violations)
    }

    @Test
    fun `allows data imports in the app di package`() {
        val scope = Konsist.scopeFromFile("$FIXTURES/app/DataModuleImportingData.kt")

        assertEquals(emptyList<String>(), scope.dataImportsOutsideDi().map { it.name })
    }

    private companion object {
        // Chemin relatif à la racine du projet, comme l'attend Konsist.
        const val FIXTURES = "architecture-tests/src/test/resources/fixtures"
    }
}
