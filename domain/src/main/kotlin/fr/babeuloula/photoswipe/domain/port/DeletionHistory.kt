package fr.babeuloula.photoswipe.domain.port

import fr.babeuloula.photoswipe.domain.model.ConfirmedDeletion
import fr.babeuloula.photoswipe.domain.model.MediaId
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Historique des suppressions confirmées des 30 derniers jours (ADR 0034), pour le résumé, l'accueil
 * et la page « Corbeille » (PS-168). Implémenté dans `:data` sur Room ; il ne garde que le temps
 * qu'un média passe dans la corbeille d'Android (`ConfirmedDeletion.TRASH_RETENTION`).
 */
interface DeletionHistory {
    /** Ajoute [deletions] à l'historique. */
    suspend fun record(deletions: List<ConfirmedDeletion>)

    /**
     * Suppressions enregistrées, observées : le flux émet la liste actuelle dès la collecte, puis
     * après chaque écriture (ADR 0008).
     */
    fun observe(): Flow<List<ConfirmedDeletion>>

    /** Oublie les suppressions confirmées avant [instant], déjà vidées de la corbeille. */
    suspend fun forgetConfirmedBefore(instant: Instant)

    /** Oublie toutes les suppressions des médias [mediaIds], restaurés depuis la corbeille. */
    suspend fun forget(mediaIds: Set<MediaId>)
}
