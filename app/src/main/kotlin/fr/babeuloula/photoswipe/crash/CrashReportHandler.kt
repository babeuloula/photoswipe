package fr.babeuloula.photoswipe.crash

import java.time.Clock

/**
 * Attrape les plantages non rattrapés (ADR 0033) : il enregistre leur rapport, puis passe la main
 * au gestionnaire d'avant, qui ferme l'application comme Android l'a toujours fait. Il n'envoie
 * rien : c'est au lancement suivant que la personne choisit d'envoyer le rapport ou non.
 *
 * @param environment ce que le rapport dit de l'application et du téléphone, lu au plantage.
 * @param next gestionnaire d'avant, celui d'Android.
 */
class CrashReportHandler(
    private val store: CrashReportStore,
    private val formatter: CrashReportFormatter,
    private val clock: Clock,
    private val environment: () -> CrashEnvironment,
    private val next: Thread.UncaughtExceptionHandler?,
) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(
        thread: Thread,
        throwable: Throwable,
    ) {
        // Rien ne doit empêcher Android de fermer l'application : une erreur du rapport est ignorée.
        runCatching { store.save(formatter.format(throwable, thread.name, clock.instant(), environment())) }
        next?.uncaughtException(thread, throwable)
    }

    companion object {
        /** Installe le gestionnaire devant celui d'Android, pour tous les fils de l'application. */
        fun install(
            store: CrashReportStore,
            formatter: CrashReportFormatter,
            clock: Clock,
            environment: () -> CrashEnvironment,
        ) {
            val next = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(CrashReportHandler(store, formatter, clock, environment, next))
        }
    }
}
