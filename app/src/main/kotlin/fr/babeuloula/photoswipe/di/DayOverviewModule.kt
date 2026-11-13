package fr.babeuloula.photoswipe.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.babeuloula.photoswipe.domain.port.MediaSource
import fr.babeuloula.photoswipe.domain.port.PhotoAnalyzer
import fr.babeuloula.photoswipe.domain.port.PhotoMeasureCache
import fr.babeuloula.photoswipe.domain.port.ReviewRepository
import fr.babeuloula.photoswipe.domain.port.SettingsRepository
import fr.babeuloula.photoswipe.domain.service.DeckOrdering
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import fr.babeuloula.photoswipe.domain.usecase.GetDayOverview
import fr.babeuloula.photoswipe.domain.usecase.GetQualityHints
import fr.babeuloula.photoswipe.domain.usecase.GetSimilarMemories
import fr.babeuloula.photoswipe.domain.usecase.KeepMedia
import fr.babeuloula.photoswipe.domain.usecase.MarkForDeletion
import fr.babeuloula.photoswipe.domain.usecase.SortInBatch
import fr.babeuloula.photoswipe.domain.usecase.UndoLastDecision
import java.time.Clock

/**
 * Fournit les use cases qui regardent tous les souvenirs d'un jour : sa vue d'ensemble (PS-139) et
 * ses photos prises à la suite (PS-140) et son tri par lot (PS-159), à part de ceux du tri (`DomainModule`).
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
    fun provideSortInBatch(
        keepMedia: KeepMedia,
        markForDeletion: MarkForDeletion,
        undoLastDecision: UndoLastDecision,
    ): SortInBatch = SortInBatch(keepMedia, markForDeletion, undoLastDecision)

    @Provides
    fun provideGetSimilarMemories(
        sameDayMatcher: SameDayMatcher,
        mediaSource: MediaSource,
        clock: Clock,
    ): GetSimilarMemories = GetSimilarMemories(sameDayMatcher, mediaSource, clock)

    @Provides
    fun provideGetQualityHints(
        analyzer: PhotoAnalyzer,
        cache: PhotoMeasureCache,
        settingsRepository: SettingsRepository,
    ): GetQualityHints = GetQualityHints(analyzer, cache, settingsRepository)
}
