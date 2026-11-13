package fr.babeuloula.photoswipe.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.babeuloula.photoswipe.data.media.ContentMotionPhotoReader
import fr.babeuloula.photoswipe.domain.port.MotionPhotoReader

/** Lie la reconnaissance des photos animées (PS-167) à la lecture de leur fichier. */
@Module
@InstallIn(SingletonComponent::class)
abstract class MotionPhotoModule {
    @Binds
    abstract fun bindMotionPhotoReader(implementation: ContentMotionPhotoReader): MotionPhotoReader
}
