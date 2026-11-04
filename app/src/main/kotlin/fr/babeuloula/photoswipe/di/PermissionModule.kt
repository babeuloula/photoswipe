package fr.babeuloula.photoswipe.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.babeuloula.photoswipe.permission.AndroidMediaAccessReader
import fr.babeuloula.photoswipe.permission.MediaAccessReader

/** Fournit la lecture de l'accès aux médias, remplaçable par un faux dans les tests. */
@Module
@InstallIn(SingletonComponent::class)
abstract class PermissionModule {
    @Binds
    abstract fun bindMediaAccessReader(implementation: AndroidMediaAccessReader): MediaAccessReader
}
