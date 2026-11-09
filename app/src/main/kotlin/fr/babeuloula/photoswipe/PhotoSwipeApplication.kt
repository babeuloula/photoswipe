package fr.babeuloula.photoswipe

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import fr.babeuloula.photoswipe.crash.CrashReportFormatter
import fr.babeuloula.photoswipe.crash.CrashReportHandler
import fr.babeuloula.photoswipe.crash.CrashReportStore
import fr.babeuloula.photoswipe.crash.crashEnvironment
import java.time.Clock
import javax.inject.Inject

/**
 * Application Android : racine du graphe de dépendances Hilt (SPEC §5.5). Elle fournit aussi la
 * configuration de WorkManager, pour que la tâche du rappel reçoive ses dépendances par Hilt :
 * l'initialisation automatique de WorkManager est retirée du manifeste.
 *
 * Au démarrage, elle installe [CrashReportHandler], qui garde le rapport d'un plantage pour que la
 * personne choisisse de l'envoyer au lancement suivant (ADR 0033).
 */
@HiltAndroidApp
class PhotoSwipeApplication :
    Application(),
    Configuration.Provider {
    /** Fabrique des tâches WorkManager injectées par Hilt (`@HiltWorker`). */
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    /** Rapport du dernier plantage. */
    @Inject
    lateinit var crashReportStore: CrashReportStore

    /** Format des rapports de plantage. */
    @Inject
    lateinit var crashReportFormatter: CrashReportFormatter

    /** Horloge, pour dater les plantages. */
    @Inject
    lateinit var clock: Clock

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        CrashReportHandler.install(crashReportStore, crashReportFormatter, clock) { crashEnvironment() }
    }
}
