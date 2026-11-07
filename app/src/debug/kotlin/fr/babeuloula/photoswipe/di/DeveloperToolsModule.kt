package fr.babeuloula.photoswipe.di

import android.content.Intent
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.babeuloula.photoswipe.data.database.DatabaseCleaner
import fr.babeuloula.photoswipe.data.seed.MediaStoreSeeder
import fr.babeuloula.photoswipe.debug.DecisionsReset
import fr.babeuloula.photoswipe.debug.DeveloperToolsActivity
import fr.babeuloula.photoswipe.debug.GallerySeeder
import fr.babeuloula.photoswipe.domain.port.UsageStatisticsRepository
import fr.babeuloula.photoswipe.ui.settings.DeveloperToolsEntry

/**
 * Variante debug : l'entrée des réglages ouvre les outils développeur, la remise à zéro des
 * décisions vide la base Room par `DatabaseCleaner` et le seeder de la galerie passe par
 * `MediaStoreSeeder` (c'est ici, dans `di`, que `:app` peut toucher à `:data`).
 */
@Module
@InstallIn(SingletonComponent::class)
object DeveloperToolsModule {
    @Provides
    fun provideDeveloperToolsEntry(): DeveloperToolsEntry =
        DeveloperToolsEntry { context -> Intent(context, DeveloperToolsActivity::class.java) }

    @Provides
    fun provideDecisionsReset(
        cleaner: DatabaseCleaner,
        statistics: UsageStatisticsRepository,
    ): DecisionsReset =
        DecisionsReset {
            cleaner.clearAll()
            statistics.clear()
        }

    @Provides
    fun provideGallerySeeder(seeder: MediaStoreSeeder): GallerySeeder =
        object : GallerySeeder {
            override suspend fun seed(): Int = seeder.seed()

            override suspend fun removeSeeded(): Int = seeder.removeSeeded()
        }
}
