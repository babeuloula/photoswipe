package fr.babeuloula.photoswipe.data.di

import android.content.ContentResolver
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fr.babeuloula.photoswipe.data.database.ConfirmedDeletionDao
import fr.babeuloula.photoswipe.data.database.DailyActivityDao
import fr.babeuloula.photoswipe.data.database.DecisionJournalDao
import fr.babeuloula.photoswipe.data.database.MIGRATION_2_3
import fr.babeuloula.photoswipe.data.database.PhotoSwipeDatabase
import fr.babeuloula.photoswipe.data.database.ReviewEntryDao
import javax.inject.Singleton

/**
 * Fournit les accès Android dont dépendent les implémentations de `:data` : la base Room, le
 * magasin DataStore et le `ContentResolver` de MediaStore.
 *
 * La base et le DataStore sont des singletons du graphe Hilt (et non des singletons statiques) :
 * Room comme DataStore exigent une seule instance par fichier.
 */
@Module
@InstallIn(SingletonComponent::class)
object StorageModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): PhotoSwipeDatabase =
        Room
            .databaseBuilder(context, PhotoSwipeDatabase::class.java, PhotoSwipeDatabase.NAME)
            .addMigrations(MIGRATION_2_3)
            .build()

    @Provides
    fun provideReviewEntryDao(database: PhotoSwipeDatabase): ReviewEntryDao = database.reviewEntryDao()

    @Provides
    fun provideDecisionJournalDao(database: PhotoSwipeDatabase): DecisionJournalDao = database.decisionJournalDao()

    @Provides
    fun provideConfirmedDeletionDao(database: PhotoSwipeDatabase): ConfirmedDeletionDao =
        database.confirmedDeletionDao()

    @Provides
    fun provideDailyActivityDao(database: PhotoSwipeDatabase): DailyActivityDao = database.dailyActivityDao()

    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create { context.preferencesDataStoreFile(SETTINGS_FILE) }

    @Provides
    fun provideContentResolver(
        @ApplicationContext context: Context,
    ): ContentResolver = context.contentResolver

    /** Nom du fichier DataStore des réglages. */
    private const val SETTINGS_FILE = "settings"

    /** WorkManager de l'application, initialisé par AndroidX Startup (tâche du rappel). */
    @Provides
    fun provideWorkManager(
        @ApplicationContext context: Context,
    ): WorkManager = WorkManager.getInstance(context)
}
