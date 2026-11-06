package fr.babeuloula.photoswipe.permission

import fr.babeuloula.photoswipe.domain.usecase.CleanOrphanEntries
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Nettoyage des orphelins (SPEC §6) : oublie les décisions des médias qui ne sont plus dans la
 * galerie, au démarrage et après chaque confirmation.
 *
 * L'accès est relu à chaque passage : `CleanOrphanEntries` ne supprime rien sans accès complet.
 * Un échec (base ou MediaStore indisponible) est ignoré : rien n'est perdu, le passage suivant
 * reprendra les mêmes orphelins. [guard] peut suspendre le nettoyage : en debug, tant que les
 * données de démo remplacent la galerie, les vraies décisions paraîtraient orphelines.
 */
@Singleton
class OrphanCleanup
    @Inject
    constructor(
        private val cleanOrphanEntries: CleanOrphanEntries,
        private val accessReader: MediaAccessReader,
        private val guard: OrphanCleanupGuard,
    ) {
        private val startupDone = AtomicBoolean(false)

        /** Premier passage du process, quand l'accès aux médias est accordé ; sans effet ensuite. */
        suspend fun runAtStartup() {
            if (startupDone.compareAndSet(false, true)) run()
        }

        /** Passage immédiat, par exemple après une confirmation. */
        suspend fun run() {
            if (!guard.allowsCleanup()) return
            runCatching { cleanOrphanEntries(accessReader.currentAccess()) }
        }
    }

/** Dit si le nettoyage des orphelins peut tourner ; lié par variante (`MediaSourceModule`). */
fun interface OrphanCleanupGuard {
    /** Vrai si la galerie lue est la vraie : ses absents sont de vrais orphelins. */
    fun allowsCleanup(): Boolean
}
