package fr.babeuloula.photoswipe.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.babeuloula.photoswipe.data.media.MediaStoreMediaSource
import fr.babeuloula.photoswipe.domain.port.MediaSource
import javax.inject.Singleton

/** Variante release : la galerie est toujours lue dans MediaStore (ADR 0015). */
@Module
@InstallIn(SingletonComponent::class)
abstract class MediaSourceModule {
    @Binds
    @Singleton
    abstract fun bindMediaSource(implementation: MediaStoreMediaSource): MediaSource
}
