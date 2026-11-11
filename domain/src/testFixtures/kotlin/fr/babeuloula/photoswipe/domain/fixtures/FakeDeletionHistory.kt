package fr.babeuloula.photoswipe.domain.fixtures

import fr.babeuloula.photoswipe.domain.model.ConfirmedDeletion
import fr.babeuloula.photoswipe.domain.port.DeletionHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant

/**
 * [DeletionHistory] en mémoire : les suppressions dans l'ordre d'enregistrement, observées comme
 * dans Room (liste actuelle à la collecte, puis après chaque écriture).
 */
class FakeDeletionHistory(
    initial: List<ConfirmedDeletion> = emptyList(),
) : DeletionHistory {
    private val state = MutableStateFlow(initial)

    /** Suppressions enregistrées, pour les vérifications du test. */
    val deletions: List<ConfirmedDeletion> get() = state.value

    override suspend fun record(deletions: List<ConfirmedDeletion>) {
        state.value += deletions
    }

    override fun observe(): Flow<List<ConfirmedDeletion>> = state.asStateFlow()

    override suspend fun forgetConfirmedBefore(instant: Instant) {
        state.value = state.value.filterNot { it.confirmedAt.isBefore(instant) }
    }
}
