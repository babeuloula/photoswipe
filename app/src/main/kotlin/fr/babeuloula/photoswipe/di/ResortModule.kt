package fr.babeuloula.photoswipe.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.babeuloula.photoswipe.domain.port.DecisionJournal
import fr.babeuloula.photoswipe.domain.port.MediaSource
import fr.babeuloula.photoswipe.domain.port.ReviewRepository
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import fr.babeuloula.photoswipe.domain.usecase.GetPreviousDecisions
import fr.babeuloula.photoswipe.domain.usecase.ReopenDay
import java.time.Clock

/** Fournit les use cases pour retrier un jour déjà trié (ADR 0026). */
@Module
@InstallIn(SingletonComponent::class)
object ResortModule {
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
}
