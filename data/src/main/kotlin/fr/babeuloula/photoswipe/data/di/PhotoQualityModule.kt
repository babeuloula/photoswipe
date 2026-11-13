package fr.babeuloula.photoswipe.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.babeuloula.photoswipe.data.quality.RoomPhotoMeasureCache
import fr.babeuloula.photoswipe.data.quality.ThumbnailPhotoAnalyzer
import fr.babeuloula.photoswipe.domain.port.PhotoAnalyzer
import fr.babeuloula.photoswipe.domain.port.PhotoMeasureCache

/** Lie la mesure des photos (PS-164) aux vignettes de MediaStore et à Room. */
@Module
@InstallIn(SingletonComponent::class)
abstract class PhotoQualityModule {
    @Binds
    abstract fun bindPhotoAnalyzer(implementation: ThumbnailPhotoAnalyzer): PhotoAnalyzer

    @Binds
    abstract fun bindPhotoMeasureCache(implementation: RoomPhotoMeasureCache): PhotoMeasureCache
}
