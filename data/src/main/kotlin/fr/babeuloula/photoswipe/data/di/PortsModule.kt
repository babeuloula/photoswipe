package fr.babeuloula.photoswipe.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.babeuloula.photoswipe.data.activity.RoomActivityHistory
import fr.babeuloula.photoswipe.data.deletion.RoomDeletionHistory
import fr.babeuloula.photoswipe.data.media.MediaStoreActionRequestFactory
import fr.babeuloula.photoswipe.data.reminder.AlarmManagerReminderScheduler
import fr.babeuloula.photoswipe.data.review.RoomDecisionJournal
import fr.babeuloula.photoswipe.data.review.RoomReviewRepository
import fr.babeuloula.photoswipe.data.settings.DataStoreSettingsRepository
import fr.babeuloula.photoswipe.data.statistics.DataStoreUsageStatisticsRepository
import fr.babeuloula.photoswipe.domain.port.ActivityHistory
import fr.babeuloula.photoswipe.domain.port.DecisionJournal
import fr.babeuloula.photoswipe.domain.port.DeletionHistory
import fr.babeuloula.photoswipe.domain.port.MediaActionRequestFactory
import fr.babeuloula.photoswipe.domain.port.ReminderScheduler
import fr.babeuloula.photoswipe.domain.port.ReviewRepository
import fr.babeuloula.photoswipe.domain.port.SettingsRepository
import fr.babeuloula.photoswipe.domain.port.UsageStatisticsRepository
import javax.inject.Singleton

/**
 * Lie chaque port du domaine à son implémentation Android (SPEC §5.5) : le domaine et l'UI ne
 * connaissent que les interfaces. `MediaSource` est lié dans `:app`, par variante : MediaStore en
 * release, MediaStore ou données de démo en debug (ADR 0015).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PortsModule {
    @Binds
    @Singleton
    abstract fun bindReviewRepository(implementation: RoomReviewRepository): ReviewRepository

    @Binds
    @Singleton
    abstract fun bindDecisionJournal(implementation: RoomDecisionJournal): DecisionJournal

    @Binds
    @Singleton
    abstract fun bindActivityHistory(implementation: RoomActivityHistory): ActivityHistory

    @Binds
    @Singleton
    abstract fun bindDeletionHistory(implementation: RoomDeletionHistory): DeletionHistory

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(implementation: DataStoreSettingsRepository): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindUsageStatisticsRepository(
        implementation: DataStoreUsageStatisticsRepository,
    ): UsageStatisticsRepository

    @Binds
    abstract fun bindReminderScheduler(implementation: AlarmManagerReminderScheduler): ReminderScheduler

    @Binds
    abstract fun bindMediaActionRequestFactory(
        implementation: MediaStoreActionRequestFactory,
    ): MediaActionRequestFactory
}
