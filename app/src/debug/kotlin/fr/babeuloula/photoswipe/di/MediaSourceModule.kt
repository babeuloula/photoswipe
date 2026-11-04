package fr.babeuloula.photoswipe.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.babeuloula.photoswipe.domain.port.MediaSource
import javax.inject.Singleton

/**
 * Variante debug : la galerie vient de MediaStore, ou des données de démo quand elles sont activées
 * dans les outils développeur (SPEC §10, ADR 0015).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MediaSourceModule {
    @Binds
    @Singleton
    abstract fun bindMediaSource(implementation: SwitchableMediaSource): MediaSource
}
