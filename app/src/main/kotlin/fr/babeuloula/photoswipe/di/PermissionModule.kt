package fr.babeuloula.photoswipe.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.babeuloula.photoswipe.permission.AndroidExactAlarmPermissionReader
import fr.babeuloula.photoswipe.permission.AndroidMediaAccessReader
import fr.babeuloula.photoswipe.permission.AndroidMediaManagementReader
import fr.babeuloula.photoswipe.permission.AndroidNotificationPermissionReader
import fr.babeuloula.photoswipe.permission.ExactAlarmPermissionReader
import fr.babeuloula.photoswipe.permission.MediaAccessReader
import fr.babeuloula.photoswipe.permission.MediaManagementReader
import fr.babeuloula.photoswipe.permission.NotificationPermissionReader

/**
 * Fournit la lecture de l'accès aux médias, de l'accès « Gestion des médias » et de la permission
 * de notification et de l'autorisation des alarmes exactes, remplaçables par des faux dans les tests.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PermissionModule {
    @Binds
    abstract fun bindMediaAccessReader(implementation: AndroidMediaAccessReader): MediaAccessReader

    @Binds
    abstract fun bindMediaManagementReader(implementation: AndroidMediaManagementReader): MediaManagementReader

    @Binds
    abstract fun bindNotificationPermissionReader(
        implementation: AndroidNotificationPermissionReader,
    ): NotificationPermissionReader

    @Binds
    abstract fun bindExactAlarmPermissionReader(
        implementation: AndroidExactAlarmPermissionReader,
    ): ExactAlarmPermissionReader
}
