package fr.babeuloula.photoswipe.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.babeuloula.photoswipe.debug.DeveloperSettings
import fr.babeuloula.photoswipe.permission.OrphanCleanupGuard

/**
 * Variante debug : pas de nettoyage des orphelins tant que les données de démo remplacent la
 * galerie, sinon les vraies décisions paraîtraient orphelines et seraient effacées.
 */
@Module
@InstallIn(SingletonComponent::class)
object OrphanCleanupGuardModule {
    @Provides
    fun provideOrphanCleanupGuard(settings: DeveloperSettings): OrphanCleanupGuard =
        OrphanCleanupGuard { !settings.demoDataEnabled }
}
