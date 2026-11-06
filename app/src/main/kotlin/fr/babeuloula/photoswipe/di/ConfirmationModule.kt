package fr.babeuloula.photoswipe.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.babeuloula.photoswipe.domain.port.DecisionJournal
import fr.babeuloula.photoswipe.domain.port.MediaActionRequestFactory
import fr.babeuloula.photoswipe.domain.port.MediaSource
import fr.babeuloula.photoswipe.domain.port.ReviewRepository
import fr.babeuloula.photoswipe.domain.port.SettingsRepository
import fr.babeuloula.photoswipe.domain.usecase.CleanOrphanEntries
import fr.babeuloula.photoswipe.domain.usecase.GetPendingFavorites
import fr.babeuloula.photoswipe.domain.usecase.GetSessionSummary
import fr.babeuloula.photoswipe.domain.usecase.OfferManageMediaTip
import fr.babeuloula.photoswipe.domain.usecase.PrepareTrash
import fr.babeuloula.photoswipe.domain.usecase.RecordFavoriteResult
import fr.babeuloula.photoswipe.domain.usecase.RecordTrashResult
import fr.babeuloula.photoswipe.domain.usecase.RequestFavorite

/**
 * Fournit les use cases du résumé et de la confirmation de fin de session (SPEC §3.5), à part de
 * ceux du tri (`DomainModule`).
 */
@Module
@InstallIn(SingletonComponent::class)
object ConfirmationModule {
    @Provides
    fun provideGetSessionSummary(reviewRepository: ReviewRepository): GetSessionSummary =
        GetSessionSummary(reviewRepository)

    @Provides
    fun providePrepareTrash(
        reviewRepository: ReviewRepository,
        mediaSource: MediaSource,
        requestFactory: MediaActionRequestFactory,
    ): PrepareTrash = PrepareTrash(reviewRepository, mediaSource, requestFactory)

    @Provides
    fun provideRecordTrashResult(
        reviewRepository: ReviewRepository,
        journal: DecisionJournal,
    ): RecordTrashResult = RecordTrashResult(reviewRepository, journal)

    @Provides
    fun provideGetPendingFavorites(reviewRepository: ReviewRepository): GetPendingFavorites =
        GetPendingFavorites(reviewRepository)

    @Provides
    fun provideRequestFavorite(requestFactory: MediaActionRequestFactory): RequestFavorite =
        RequestFavorite(requestFactory)

    @Provides
    fun provideRecordFavoriteResult(reviewRepository: ReviewRepository): RecordFavoriteResult =
        RecordFavoriteResult(reviewRepository)

    @Provides
    fun provideCleanOrphanEntries(
        reviewRepository: ReviewRepository,
        mediaSource: MediaSource,
        journal: DecisionJournal,
    ): CleanOrphanEntries = CleanOrphanEntries(reviewRepository, mediaSource, journal)

    @Provides
    fun provideOfferManageMediaTip(settingsRepository: SettingsRepository): OfferManageMediaTip =
        OfferManageMediaTip(settingsRepository)
}
