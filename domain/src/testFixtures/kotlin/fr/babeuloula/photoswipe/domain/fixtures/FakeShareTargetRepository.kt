package fr.babeuloula.photoswipe.domain.fixtures

import fr.babeuloula.photoswipe.domain.port.ShareTargetRepository

/** [ShareTargetRepository] en mémoire. */
class FakeShareTargetRepository(
    target: String? = null,
) : ShareTargetRepository {
    /** Dernière application retenue, pour les vérifications du test. */
    var target: String? = target
        private set

    override suspend fun lastTarget(): String? = target

    override suspend fun saveLastTarget(component: String) {
        target = component
    }
}
