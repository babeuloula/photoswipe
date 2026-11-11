package fr.babeuloula.photoswipe.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.babeuloula.photoswipe.domain.port.DecisionJournal
import fr.babeuloula.photoswipe.domain.port.DeletionHistory
import fr.babeuloula.photoswipe.domain.port.MediaSource
import fr.babeuloula.photoswipe.domain.port.ReviewRepository
import fr.babeuloula.photoswipe.domain.service.DeckOrdering
import fr.babeuloula.photoswipe.domain.service.RecentYearsFirstOrdering
import fr.babeuloula.photoswipe.domain.service.ReviewWindowProvider
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import fr.babeuloula.photoswipe.domain.usecase.FavoriteMedia
import fr.babeuloula.photoswipe.domain.usecase.GetDaySession
import fr.babeuloula.photoswipe.domain.usecase.GetDeckForDay
import fr.babeuloula.photoswipe.domain.usecase.GetReviewDays
import fr.babeuloula.photoswipe.domain.usecase.KeepMedia
import fr.babeuloula.photoswipe.domain.usecase.MarkForDeletion
import fr.babeuloula.photoswipe.domain.usecase.UndoLastDecision
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

    // Un paramètre par dépendance du use case, que Hilt fournit : rien à regrouper ici.
    @Suppress("LongParameterList")
    @Provides
    fun provideGetReviewDays(
        windowProvider: ReviewWindowProvider,
        sameDayMatcher: SameDayMatcher,
        mediaSource: MediaSource,
        reviewRepository: ReviewRepository,
        history: DeletionHistory,
        clock: Clock,
    ): GetReviewDays = GetReviewDays(windowProvider, sameDayMatcher, mediaSource, reviewRepository, history, clock)

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

    @Provides
    fun provideKeepMedia(
        reviewRepository: ReviewRepository,
        journal: DecisionJournal,
        clock: Clock,
    ): KeepMedia = KeepMedia(reviewRepository, journal, clock)

    @Provides
    fun provideMarkForDeletion(
        reviewRepository: ReviewRepository,
        journal: DecisionJournal,
        clock: Clock,
    ): MarkForDeletion = MarkForDeletion(reviewRepository, journal, clock)

    @Provides
    fun provideUndoLastDecision(
        reviewRepository: ReviewRepository,
        journal: DecisionJournal,
    ): UndoLastDecision = UndoLastDecision(reviewRepository, journal)

    @Provides
    fun provideFavoriteMedia(
        reviewRepository: ReviewRepository,
        journal: DecisionJournal,
        clock: Clock,
    ): FavoriteMedia = FavoriteMedia(reviewRepository, journal, clock)

    @Provides
    fun provideGetDaySession(
        journal: DecisionJournal,
        reviewRepository: ReviewRepository,
        mediaSource: MediaSource,
        windowProvider: ReviewWindowProvider,
    ): GetDaySession = GetDaySession(journal, reviewRepository, mediaSource, windowProvider)
}
