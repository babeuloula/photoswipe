package fr.babeuloula.photoswipe.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.babeuloula.photoswipe.domain.port.ActivityHistory
import fr.babeuloula.photoswipe.domain.port.DecisionJournal
import fr.babeuloula.photoswipe.domain.port.DeletionHistory
import fr.babeuloula.photoswipe.domain.port.MediaActionRequestFactory
import fr.babeuloula.photoswipe.domain.port.ReviewRepository
import fr.babeuloula.photoswipe.domain.port.SystemTrash
import fr.babeuloula.photoswipe.domain.port.UsageStatisticsRepository
import fr.babeuloula.photoswipe.domain.usecase.GetTrash
import fr.babeuloula.photoswipe.domain.usecase.RecordRestoreResult
import fr.babeuloula.photoswipe.domain.usecase.RequestPermanentDeletion
import fr.babeuloula.photoswipe.domain.usecase.RequestRestore
import java.time.Clock

/** Fournit les use cases de la page « Corbeille » (PS-168). */
@Module
@InstallIn(SingletonComponent::class)
object TrashModule {
    @Provides
    fun provideGetTrash(
        history: DeletionHistory,
        systemTrash: SystemTrash,
    ): GetTrash = GetTrash(history, systemTrash)

    @Provides
    fun provideRequestRestore(requestFactory: MediaActionRequestFactory): RequestRestore =
        RequestRestore(requestFactory)

    @Provides
    fun provideRequestPermanentDeletion(requestFactory: MediaActionRequestFactory): RequestPermanentDeletion =
        RequestPermanentDeletion(requestFactory)

    // Un paramètre par dépendance du use case, que Hilt fournit : rien à regrouper ici.
    @Suppress("LongParameterList")
    @Provides
    fun provideRecordRestoreResult(
        history: DeletionHistory,
        reviewRepository: ReviewRepository,
        journal: DecisionJournal,
        statistics: UsageStatisticsRepository,
        activity: ActivityHistory,
        clock: Clock,
    ): RecordRestoreResult = RecordRestoreResult(history, reviewRepository, journal, statistics, activity, clock)
}
