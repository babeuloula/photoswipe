package fr.babeuloula.photoswipe.domain.port

import fr.babeuloula.photoswipe.domain.model.ConfirmedDeletion
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Historique des suppressions confirmées des derniers jours (ADR 0034), pour le résumé et l'accueil.
 * Implémenté dans `:data` sur Room ; il ne garde que ce qui sert à la fenêtre de revue.
 */
interface DeletionHistory {
    /** Ajoute [deletions] à l'historique. */
    suspend fun record(deletions: List<ConfirmedDeletion>)

    /**
     * Suppressions enregistrées, observées : le flux émet la liste actuelle dès la collecte, puis
     * après chaque écriture (ADR 0008).
     */
    fun observe(): Flow<List<ConfirmedDeletion>>

    /** Oublie les suppressions confirmées avant [instant], sorties de la fenêtre de revue. */
    suspend fun forgetConfirmedBefore(instant: Instant)
}
