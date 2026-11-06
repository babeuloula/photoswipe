package fr.babeuloula.photoswipe.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.babeuloula.photoswipe.permission.OrphanCleanupGuard

/** Variante release : la galerie est toujours la vraie, le nettoyage des orphelins tourne toujours. */
@Module
@InstallIn(SingletonComponent::class)
object OrphanCleanupGuardModule {
    @Provides
    fun provideOrphanCleanupGuard(): OrphanCleanupGuard = OrphanCleanupGuard { true }
}
