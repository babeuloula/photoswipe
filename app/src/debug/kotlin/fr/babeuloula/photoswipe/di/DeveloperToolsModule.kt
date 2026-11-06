package fr.babeuloula.photoswipe.di

import android.content.Intent
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.babeuloula.photoswipe.data.database.DatabaseCleaner
import fr.babeuloula.photoswipe.debug.DecisionsReset
import fr.babeuloula.photoswipe.debug.DeveloperToolsActivity
import fr.babeuloula.photoswipe.ui.settings.DeveloperToolsEntry

/**
 * Variante debug : l'entrée des réglages ouvre les outils développeur, et la remise à zéro des
 * décisions vide la base Room par `DatabaseCleaner` (c'est ici, dans `di`, que `:app` peut toucher à `:data`).
 */
@Module
@InstallIn(SingletonComponent::class)
object DeveloperToolsModule {
    @Provides
    fun provideDeveloperToolsEntry(): DeveloperToolsEntry =
        DeveloperToolsEntry { context -> Intent(context, DeveloperToolsActivity::class.java) }

    @Provides
    fun provideDecisionsReset(cleaner: DatabaseCleaner): DecisionsReset = DecisionsReset(cleaner::clearAll)
}
