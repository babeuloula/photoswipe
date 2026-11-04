package fr.babeuloula.photoswipe.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.babeuloula.photoswipe.domain.port.MediaSource
import fr.babeuloula.photoswipe.domain.port.ReviewRepository
import fr.babeuloula.photoswipe.domain.service.DeckOrdering
import fr.babeuloula.photoswipe.domain.service.RecentYearsFirstOrdering
import fr.babeuloula.photoswipe.domain.service.ReviewWindowProvider
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import fr.babeuloula.photoswipe.domain.usecase.GetDeckForDay
import fr.babeuloula.photoswipe.domain.usecase.GetReviewDays
import fr.babeuloula.photoswipe.domain.usecase.GetSessionSummary
import java.time.Clock

/**
 * Fournit les services et use cases du domaine. `:domain` reste du Kotlin pur, sans annotation
 * d'injection : c'est ici qu'ils sont construits avec leurs dépendances (SPEC §5.5).
 */
@Module
@InstallIn(SingletonComponent::class)
object DomainModule {
    @Provides
    fun provideSameDayMatcher(): SameDayMatcher = SameDayMatcher()

    @Provides
    fun provideReviewWindowProvider(clock: Clock): ReviewWindowProvider = ReviewWindowProvider(clock)

    @Provides
    fun provideGetReviewDays(
        windowProvider: ReviewWindowProvider,
        sameDayMatcher: SameDayMatcher,
        mediaSource: MediaSource,
        reviewRepository: ReviewRepository,
        clock: Clock,
    ): GetReviewDays = GetReviewDays(windowProvider, sameDayMatcher, mediaSource, reviewRepository, clock)

    @Provides
    fun provideGetSessionSummary(reviewRepository: ReviewRepository): GetSessionSummary =
        GetSessionSummary(reviewRepository)

    @Provides
    fun provideDeckOrdering(): DeckOrdering = RecentYearsFirstOrdering()

    @Provides
    fun provideGetDeckForDay(
        sameDayMatcher: SameDayMatcher,
        mediaSource: MediaSource,
        reviewRepository: ReviewRepository,
        deckOrdering: DeckOrdering,
        clock: Clock,
    ): GetDeckForDay = GetDeckForDay(sameDayMatcher, mediaSource, reviewRepository, deckOrdering, clock)
}
