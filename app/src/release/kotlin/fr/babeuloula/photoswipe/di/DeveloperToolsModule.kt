package fr.babeuloula.photoswipe.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.babeuloula.photoswipe.ui.settings.DeveloperToolsEntry

/** Variante release : aucun outil développeur, les réglages n'en montrent pas l'entrée. */
@Module
@InstallIn(SingletonComponent::class)
object DeveloperToolsModule {
    @Provides
    fun provideDeveloperToolsEntry(): DeveloperToolsEntry = DeveloperToolsEntry { null }
}
