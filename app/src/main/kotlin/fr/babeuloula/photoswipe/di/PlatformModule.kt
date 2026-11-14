package fr.babeuloula.photoswipe.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.babeuloula.photoswipe.platform.PlatformFeatures

/** Fournit les fonctions d'Android disponibles sur ce téléphone (PS-181), remplaçables dans les tests. */
@Module
@InstallIn(SingletonComponent::class)
object PlatformModule {
    @Provides
    fun providePlatformFeatures(): PlatformFeatures = PlatformFeatures.current()
}
