package fr.babeuloula.photoswipe.ui.deck

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.AndroidComposeTestRule

/**
 * Recréation d'écran simulée, comme `StateRestorationTester` (inutilisable quand `MainActivity` a
 * déjà son contenu) : l'état `rememberSaveable` est enregistré, le contenu retiré, puis recomposé
 * à partir de cet état, au même endroit de la composition.
 */
internal class SavedStateRestorer {
    private var registry by mutableStateOf(SaveableStateRegistry(null) { true })
    private var shown by mutableStateOf(true)

    /** Contenu à recréer. */
    @Composable
    fun Restorable(content: @Composable () -> Unit) {
        if (shown) {
            CompositionLocalProvider(LocalSaveableStateRegistry provides registry) { content() }
        }
    }

    /** Enregistre l'état, retire le contenu et le recompose depuis cet état. */
    fun saveAndRestore(rule: AndroidComposeTestRule<*, *>) {
        rule.runOnUiThread {
            val saved = registry.performSave()
            shown = false
            registry = SaveableStateRegistry(saved) { true }
        }
        rule.waitForIdle()
        rule.runOnUiThread { shown = true }
        rule.waitForIdle()
    }
}
