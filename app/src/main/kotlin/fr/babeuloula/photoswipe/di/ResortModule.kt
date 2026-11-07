package fr.babeuloula.photoswipe.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.babeuloula.photoswipe.domain.port.DecisionJournal
import fr.babeuloula.photoswipe.domain.port.MediaActionRequestFactory
import fr.babeuloula.photoswipe.domain.port.MediaSource
import fr.babeuloula.photoswipe.domain.port.ReviewRepository
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import fr.babeuloula.photoswipe.domain.usecase.DropPendingFavorite
import fr.babeuloula.photoswipe.domain.usecase.FavoriteMedia
import fr.babeuloula.photoswipe.domain.usecase.GetMarkedDeck
import fr.babeuloula.photoswipe.domain.usecase.GetPreviousDecisions
import fr.babeuloula.photoswipe.domain.usecase.KeepMedia
import fr.babeuloula.photoswipe.domain.usecase.MarkForDeletion
import fr.babeuloula.photoswipe.domain.usecase.ReopenDay
import fr.babeuloula.photoswipe.domain.usecase.RequestUnfavorite
import fr.babeuloula.photoswipe.domain.usecase.ReviewMarkedMedia
import java.time.Clock

/**
 * Fournit les use cases pour revenir sur des décisions : retrier un jour déjà trié (ADR 0026), revoir
 * les médias marqués depuis le résumé (PS-112), retirer un favori (PS-124).
 */
@Module
@InstallIn(SingletonComponent::class)
object ResortModule {
    @Provides
    fun provideRequestUnfavorite(requestFactory: MediaActionRequestFactory): RequestUnfavorite =
        RequestUnfavorite(requestFactory)

    @Provides
    fun provideDropPendingFavorite(reviewRepository: ReviewRepository): DropPendingFavorite =
        DropPendingFavorite(reviewRepository)

    @Provides
    fun provideGetPreviousDecisions(reviewRepository: ReviewRepository): GetPreviousDecisions =
        GetPreviousDecisions(reviewRepository)

    @Provides
    fun provideReopenDay(
        sameDayMatcher: SameDayMatcher,
        mediaSource: MediaSource,
        reviewRepository: ReviewRepository,
        journal: DecisionJournal,
        clock: Clock,
    ): ReopenDay = ReopenDay(sameDayMatcher, mediaSource, reviewRepository, journal, clock)

    @Provides
    fun provideGetMarkedDeck(
        reviewRepository: ReviewRepository,
        mediaSource: MediaSource,
        sameDayMatcher: SameDayMatcher,
        clock: Clock,
    ): GetMarkedDeck = GetMarkedDeck(reviewRepository, mediaSource, sameDayMatcher, clock)

    @Provides
    fun provideReviewMarkedMedia(
        keepMedia: KeepMedia,
        markForDeletion: MarkForDeletion,
        favoriteMedia: FavoriteMedia,
    ): ReviewMarkedMedia = ReviewMarkedMedia(keepMedia, markForDeletion, favoriteMedia)
}
