package fr.babeuloula.photoswipe.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.babeuloula.photoswipe.domain.port.ReminderScheduler
import fr.babeuloula.photoswipe.domain.port.SettingsRepository
import fr.babeuloula.photoswipe.domain.usecase.UpdateReminderSettings

/** Fournit les use cases des réglages (SPEC §3.6), à part de ceux du tri (`DomainModule`). */
@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {
    @Provides
    fun provideUpdateReminderSettings(
        settingsRepository: SettingsRepository,
        reminderScheduler: ReminderScheduler,
    ): UpdateReminderSettings = UpdateReminderSettings(settingsRepository, reminderScheduler)
}
