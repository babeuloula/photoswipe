package fr.babeuloula.photoswipe.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/**
 * Fournit l'horloge du système. Le temps est toujours injecté, jamais lu en statique,
 * pour que la logique « même jour » reste testable (docs/architecture.md §3).
 */
@Module
@InstallIn(SingletonComponent::class)
object TimeModule {
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
