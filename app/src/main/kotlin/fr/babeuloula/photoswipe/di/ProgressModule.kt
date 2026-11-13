package fr.babeuloula.photoswipe.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.babeuloula.photoswipe.domain.port.ActivityHistory
import fr.babeuloula.photoswipe.domain.usecase.GetActivityReport
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
}
