package fr.babeuloula.photoswipe.domain.fixtures

import fr.babeuloula.photoswipe.domain.model.ManageMediaTip
import fr.babeuloula.photoswipe.domain.model.ReminderSettings
import fr.babeuloula.photoswipe.domain.model.ThemeMode
import fr.babeuloula.photoswipe.domain.model.Tutorial
import fr.babeuloula.photoswipe.domain.port.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [SettingsRepository] en mémoire : [ReminderSettings.DEFAULT] tant que rien n'est enregistré,
 * comme DataStore ; [observeReminderSettings] émet la valeur actuelle puis chaque nouvelle valeur.
 *
 * Il en va de même du thème ([ThemeMode.DEFAULT] au départ), du conseil « Gestion des médias » et de
 * la version du tutoriel vue ([Tutorial.NEVER_SEEN] au départ).
 *
 * @param reminder réglages présents au départ.
 * @param themeMode thème présent au départ.
 * @param manageMediaTip état du conseil « Gestion des médias » au départ.
 * @param tutorialVersionSeen version du tutoriel vue au départ.
 */
class FakeSettingsRepository(
    reminder: ReminderSettings = ReminderSettings.DEFAULT,
    themeMode: ThemeMode = ThemeMode.DEFAULT,
    manageMediaTip: ManageMediaTip = ManageMediaTip.DEFAULT,
    tutorialVersionSeen: Int = Tutorial.NEVER_SEEN,
) : SettingsRepository {
    /** Version du tutoriel vue, pour les vérifications du test. */
    var tutorialVersionSeen: Int = tutorialVersionSeen
        private set

    private val storedTip = MutableStateFlow(manageMediaTip)

    /** Conseil « Gestion des médias » enregistré, pour les vérifications du test. */
    val manageMediaTip: ManageMediaTip get() = storedTip.value

    private val stored = MutableStateFlow(reminder)
    private val storedTheme = MutableStateFlow(themeMode)

    /** Thème enregistré, pour les vérifications du test. */
    val themeMode: ThemeMode get() = storedTheme.value

    /** Réglages du rappel enregistrés, pour les vérifications du test. */
    val reminder: ReminderSettings get() = stored.value

    override suspend fun reminderSettings(): ReminderSettings = stored.value

    override fun observeReminderSettings(): Flow<ReminderSettings> = stored.asStateFlow()

    override suspend fun saveReminderSettings(settings: ReminderSettings) {
        stored.value = settings
    }

    override fun observeThemeMode(): Flow<ThemeMode> = storedTheme.asStateFlow()

    override suspend fun saveThemeMode(mode: ThemeMode) {
        storedTheme.value = mode
    }

    override fun observeManageMediaTip(): Flow<ManageMediaTip> = storedTip.asStateFlow()

    override suspend fun saveManageMediaTip(tip: ManageMediaTip) {
        storedTip.value = tip
    }

    override suspend fun tutorialVersionSeen(): Int = tutorialVersionSeen

    override suspend fun saveTutorialVersionSeen(version: Int) {
        tutorialVersionSeen = version
    }
}
