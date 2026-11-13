package fr.babeuloula.photoswipe.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.babeuloula.photoswipe.domain.port.ActivityHistory
import fr.babeuloula.photoswipe.domain.port.SettingsRepository
import fr.babeuloula.photoswipe.domain.usecase.GetActivityReport
import fr.babeuloula.photoswipe.domain.usecase.GetStreak
import fr.babeuloula.photoswipe.domain.usecase.UpdateStreak
import java.time.Clock

/** Fournit les use cases de la page « Progression » (epic 13), à part de ceux du tri (`DomainModule`). */
@Module
@InstallIn(SingletonComponent::class)
object ProgressModule {
    @Provides
    fun provideGetActivityReport(
        history: ActivityHistory,
        clock: Clock,
    ): GetActivityReport = GetActivityReport(history, clock)

    @Provides
    fun provideUpdateStreak(
        settingsRepository: SettingsRepository,
        clock: Clock,
    ): UpdateStreak = UpdateStreak(settingsRepository, clock)

    @Provides
    fun provideGetStreak(settingsRepository: SettingsRepository): GetStreak = GetStreak(settingsRepository)
}
