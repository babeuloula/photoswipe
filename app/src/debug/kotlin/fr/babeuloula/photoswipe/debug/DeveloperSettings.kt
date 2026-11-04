package fr.babeuloula.photoswipe.debug

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Réglages des outils développeur, propres à la variante debug (SPEC §10). Ils vivent dans un
 * fichier à part, que la variante release ne connaît pas.
 */
@Singleton
class DeveloperSettings
    @Inject
    constructor(
        @param:ApplicationContext context: Context,
    ) {
        private val preferences: SharedPreferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

        /** Vrai si la galerie est remplacée par les données de démo. */
        var demoDataEnabled: Boolean
            get() = preferences.getBoolean(DEMO_DATA, false)
            set(value) {
                preferences.edit().putBoolean(DEMO_DATA, value).apply()
            }

        private companion object {
            const val FILE = "developer_tools"
            const val DEMO_DATA = "demo_data_enabled"
        }
    }
