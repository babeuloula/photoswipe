package fr.babeuloula.photoswipe.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.babeuloula.photoswipe.domain.usecase.GetReviewDays
import fr.babeuloula.photoswipe.domain.usecase.GetTodayWidget
import fr.babeuloula.photoswipe.widget.AppWidgetPlacedWidgets
import fr.babeuloula.photoswipe.widget.PlacedWidgets
import fr.babeuloula.photoswipe.widget.ThisDayWidgetUpdates
import fr.babeuloula.photoswipe.widget.WidgetRefresher

/** Fournit l'état du widget « Ce jour-là » et la planification de ses mises à jour (PS-170). */
@Module
@InstallIn(SingletonComponent::class)
abstract class WidgetModule {
    @Binds
    abstract fun bindWidgetRefresher(implementation: ThisDayWidgetUpdates): WidgetRefresher

    @Binds
    abstract fun bindPlacedWidgets(implementation: AppWidgetPlacedWidgets): PlacedWidgets

    companion object {
        @Provides
        fun provideGetTodayWidget(getReviewDays: GetReviewDays): GetTodayWidget = GetTodayWidget(getReviewDays)
    }
}
