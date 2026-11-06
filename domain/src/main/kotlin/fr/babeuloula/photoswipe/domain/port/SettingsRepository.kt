package fr.babeuloula.photoswipe.domain.port

import fr.babeuloula.photoswipe.domain.model.ManageMediaTip
import fr.babeuloula.photoswipe.domain.model.ReminderSettings
import fr.babeuloula.photoswipe.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Préférences de l'utilisateur (SPEC §5.2). Implémenté dans `:data` sur DataStore.
 *
 * La langue n'y figure pas : elle passe par le réglage de langue par application d'Android
 * (SPEC §4). Les réglages du tutoriel arriveront avec l'epic 11.
 */
interface SettingsRepository {
    /** Réglages du rappel quotidien, [ReminderSettings.DEFAULT] tant que rien n'a été enregistré. */
    suspend fun reminderSettings(): ReminderSettings

    /**
     * Réglages du rappel quotidien, observés pour l'écran des réglages : le flux émet la valeur
     * actuelle dès la collecte ([ReminderSettings.DEFAULT] si rien n'est enregistré), puis chaque
     * nouvelle valeur enregistrée.
     */
    fun observeReminderSettings(): Flow<ReminderSettings>

    /** Enregistre les réglages du rappel quotidien. */
    suspend fun saveReminderSettings(settings: ReminderSettings)

    /**
     * Thème de l'application, observé par l'activité et les réglages : le flux émet la valeur
     * actuelle dès la collecte ([ThemeMode.DEFAULT] si rien n'est enregistré), puis chaque
     * nouvelle valeur enregistrée.
     */
    fun observeThemeMode(): Flow<ThemeMode>

    /** Enregistre le thème de l'application. */
    suspend fun saveThemeMode(mode: ThemeMode)

    /**
     * Conseil « Gestion des médias » de l'accueil : la valeur actuelle dès la collecte
     * ([ManageMediaTip.DEFAULT] si rien n'est enregistré), puis chaque nouvelle valeur.
     */
    fun observeManageMediaTip(): Flow<ManageMediaTip>

    /** Enregistre l'état du conseil « Gestion des médias ». */
    suspend fun saveManageMediaTip(tip: ManageMediaTip)
}
