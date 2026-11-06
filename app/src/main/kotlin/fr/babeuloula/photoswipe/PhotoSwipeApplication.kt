package fr.babeuloula.photoswipe

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application Android : racine du graphe de dépendances Hilt (SPEC §5.5). Elle fournit aussi la
 * configuration de WorkManager, pour que la tâche du rappel reçoive ses dépendances par Hilt :
 * l'initialisation automatique de WorkManager est retirée du manifeste.
 */
@HiltAndroidApp
class PhotoSwipeApplication :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
}
