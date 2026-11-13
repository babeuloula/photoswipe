package fr.babeuloula.photoswipe.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.babeuloula.photoswipe.data.media.MediaStoreSystemTrash
import fr.babeuloula.photoswipe.domain.port.SystemTrash

/** Lie la lecture de la corbeille d'Android à MediaStore (PS-168), dans toutes les variantes. */
@Module
@InstallIn(SingletonComponent::class)
abstract class SystemTrashModule {
    @Binds
    abstract fun bindSystemTrash(implementation: MediaStoreSystemTrash): SystemTrash
}
