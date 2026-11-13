package fr.babeuloula.photoswipe.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fr.babeuloula.photoswipe.domain.port.SettingsRepository
import fr.babeuloula.photoswipe.domain.usecase.CheckWhatsNew
import fr.babeuloula.photoswipe.domain.usecase.MarkWhatsNewSeen
import fr.babeuloula.photoswipe.domain.usecase.RecordFirstInstall
import fr.babeuloula.photoswipe.ui.whatsnew.WHATS_NEW
import fr.babeuloula.photoswipe.ui.whatsnew.WhatsNewActions

/**
 * Fournit les use cases des nouveautés (PS-173), sur la version installée (`versionName`) et la
 * liste [WHATS_NEW]. À part du tutoriel : un test qui remplace `TutorialModule` garde celui-ci.
 */
@Module
@InstallIn(SingletonComponent::class)
object WhatsNewModule {
    @Provides
    fun provideWhatsNewActions(
        settingsRepository: SettingsRepository,
        @ApplicationContext context: Context,
    ): WhatsNewActions {
        val version = installedVersionName(context)
        return WhatsNewActions(
            check = CheckWhatsNew(settingsRepository, version, WHATS_NEW.map { it.version }),
            markSeen = MarkWhatsNewSeen(settingsRepository, version),
            releases = WHATS_NEW,
        )
    }

    @Provides
    fun provideRecordFirstInstall(
        settingsRepository: SettingsRepository,
        @ApplicationContext context: Context,
    ): RecordFirstInstall = RecordFirstInstall(settingsRepository, installedVersionName(context))

    /** Nom de la version installée, tiré du dernier tag `vX.Y.Z` (`AppVersion`). */
    private fun installedVersionName(context: Context): String =
        context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName
            .orEmpty()
}
