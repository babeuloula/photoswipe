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
import java.time.Clock

/** Fournit le use case de la vue d'ensemble d'un jour (PS-139), à part de ceux du tri (`DomainModule`). */
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
}
