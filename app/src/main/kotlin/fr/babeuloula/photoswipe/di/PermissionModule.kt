package fr.babeuloula.photoswipe.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.babeuloula.photoswipe.permission.AndroidMediaAccessReader
import fr.babeuloula.photoswipe.permission.AndroidNotificationPermissionReader
import fr.babeuloula.photoswipe.permission.MediaAccessReader
import fr.babeuloula.photoswipe.permission.NotificationPermissionReader

/** Fournit la lecture de l'accès aux médias et de la permission de notification, remplaçables par des faux dans les tests. */
@Module
@InstallIn(SingletonComponent::class)
abstract class PermissionModule {
    @Binds
    abstract fun bindMediaAccessReader(implementation: AndroidMediaAccessReader): MediaAccessReader

    @Binds
    abstract fun bindNotificationPermissionReader(
        implementation: AndroidNotificationPermissionReader,
    ): NotificationPermissionReader
}
