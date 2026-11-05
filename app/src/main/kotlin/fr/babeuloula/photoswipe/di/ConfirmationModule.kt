package fr.babeuloula.photoswipe.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.babeuloula.photoswipe.domain.port.DecisionJournal
import fr.babeuloula.photoswipe.domain.port.MediaActionRequestFactory
import fr.babeuloula.photoswipe.domain.port.MediaSource
import fr.babeuloula.photoswipe.domain.port.ReviewRepository
import fr.babeuloula.photoswipe.domain.usecase.GetSessionSummary
import fr.babeuloula.photoswipe.domain.usecase.PrepareFavorites
import fr.babeuloula.photoswipe.domain.usecase.PrepareTrash
import fr.babeuloula.photoswipe.domain.usecase.RecordFavoriteResult
import fr.babeuloula.photoswipe.domain.usecase.RecordTrashResult

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
    fun providePrepareFavorites(
        reviewRepository: ReviewRepository,
        mediaSource: MediaSource,
        requestFactory: MediaActionRequestFactory,
    ): PrepareFavorites = PrepareFavorites(reviewRepository, mediaSource, requestFactory)

    @Provides
    fun provideRecordFavoriteResult(reviewRepository: ReviewRepository): RecordFavoriteResult =
        RecordFavoriteResult(reviewRepository)
}
