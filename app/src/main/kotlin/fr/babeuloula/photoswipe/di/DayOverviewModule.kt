package fr.babeuloula.photoswipe.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.babeuloula.photoswipe.domain.port.MediaSource
import fr.babeuloula.photoswipe.domain.port.ReviewRepository
import fr.babeuloula.photoswipe.domain.service.DeckOrdering
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import fr.babeuloula.photoswipe.domain.usecase.GetDayOverview
import fr.babeuloula.photoswipe.domain.usecase.GetSimilarMemories
import java.time.Clock

/**
 * Fournit les use cases qui regardent tous les souvenirs d'un jour : sa vue d'ensemble (PS-139) et
 * ses photos prises à la suite (PS-140), à part de ceux du tri (`DomainModule`).
 */
@Module
@InstallIn(SingletonComponent::class)
object DayOverviewModule {
    @Provides
    fun provideGetDayOverview(
        sameDayMatcher: SameDayMatcher,
        mediaSource: MediaSource,
        reviewRepository: ReviewRepository,
        deckOrdering: DeckOrdering,
        clock: Clock,
    ): GetDayOverview = GetDayOverview(sameDayMatcher, mediaSource, reviewRepository, deckOrdering, clock)

    @Provides
    fun provideGetSimilarMemories(
        sameDayMatcher: SameDayMatcher,
        mediaSource: MediaSource,
        clock: Clock,
    ): GetSimilarMemories = GetSimilarMemories(sameDayMatcher, mediaSource, clock)
}
