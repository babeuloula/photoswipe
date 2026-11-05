package fr.babeuloula.photoswipe.di

import android.content.Context
import coil3.ImageLoader
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Fournit le chargeur d'images Coil de l'application (SPEC §7). Une seule instance, portée par Hilt
 * et non par le singleton global de Coil : son cache mémoire est partagé par toutes les cartes
 * (ADR 0016). Aucun module réseau : les médias sont des URI `content://` locales. Le décodeur
 * vidéo tire une image de chaque vidéo pour sa miniature (PS-50).
 */
@Module
@InstallIn(SingletonComponent::class)
object ImageModule {
    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
    ): ImageLoader =
        ImageLoader
            .Builder(context)
            .components { add(VideoFrameDecoder.Factory()) }
            .crossfade(true)
            .build()
}
