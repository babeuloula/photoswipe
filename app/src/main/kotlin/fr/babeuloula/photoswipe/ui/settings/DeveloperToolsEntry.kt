package fr.babeuloula.photoswipe.ui.settings

import android.content.Context
import android.content.Intent

/**
 * Entrée « Outils développeur » des réglages (SPEC §4.5 et §10), liée par variante : en debug,
 * elle ouvre les outils ; en release, il n'y en a pas et la ligne n'apparaît pas.
 */
fun interface DeveloperToolsEntry {
    /** Intent des outils développeur, `null` si la variante n'en a pas. */
    fun intent(context: Context): Intent?
}
