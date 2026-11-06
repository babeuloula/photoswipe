package fr.babeuloula.photoswipe.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.babeuloula.photoswipe.domain.port.SettingsRepository
import fr.babeuloula.photoswipe.domain.usecase.MarkTutorialSeen
import fr.babeuloula.photoswipe.domain.usecase.ShouldShowTutorial

/**
 * Fournit les use cases du tutoriel (SPEC §4), à part : un test qui démarre `MainActivity` sans
 * tutoriel désinstalle ce module et lie les siens (`@UninstallModules`, `@BindValue`).
 */
@Module
@InstallIn(SingletonComponent::class)
object TutorialModule {
    @Provides
    fun provideShouldShowTutorial(settingsRepository: SettingsRepository): ShouldShowTutorial =
        ShouldShowTutorial(settingsRepository)

    @Provides
    fun provideMarkTutorialSeen(settingsRepository: SettingsRepository): MarkTutorialSeen =
        MarkTutorialSeen(settingsRepository)
}
