package fr.babeuloula.photoswipe.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.babeuloula.photoswipe.data.media.MediaStoreMediaSource
import fr.babeuloula.photoswipe.data.review.RoomReviewRepository
import fr.babeuloula.photoswipe.data.settings.DataStoreSettingsRepository
import fr.babeuloula.photoswipe.domain.port.MediaSource
import fr.babeuloula.photoswipe.domain.port.ReviewRepository
import fr.babeuloula.photoswipe.domain.port.SettingsRepository
import javax.inject.Singleton

/**
 * Lie chaque port du domaine à son implémentation Android (SPEC §5.5) : le domaine et l'UI ne
 * connaissent que les interfaces. `MediaActionRequestFactory` et `ReminderScheduler` seront liés
 * avec leurs implémentations (epics 7 et 8).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PortsModule {
    @Binds
    @Singleton
    abstract fun bindReviewRepository(implementation: RoomReviewRepository): ReviewRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(implementation: DataStoreSettingsRepository): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindMediaSource(implementation: MediaStoreMediaSource): MediaSource
}
