package fr.babeuloula.photoswipe.data.database

import fr.babeuloula.photoswipe.data.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Vide toute la base Room (décisions et journal d'annulation). Utilisé seulement par les outils
 * développeur de la variante debug (SPEC §10) ; R8 le retire de la release, qui ne s'en sert pas.
 */
class DatabaseCleaner
    @Inject
    constructor(
        private val database: PhotoSwipeDatabase,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        /** Efface toutes les tables, hors du thread principal. */
        suspend fun clearAll() {
            withContext(ioDispatcher) { database.clearAllTables() }
        }
    }
