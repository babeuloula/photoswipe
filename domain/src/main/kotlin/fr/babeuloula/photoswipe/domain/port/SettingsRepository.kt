package fr.babeuloula.photoswipe.domain.port

import fr.babeuloula.photoswipe.domain.model.CatchUpWindow
import fr.babeuloula.photoswipe.domain.model.ManageMediaTip
import fr.babeuloula.photoswipe.domain.model.RatingInvitation
import fr.babeuloula.photoswipe.domain.model.ReminderSettings
import fr.babeuloula.photoswipe.domain.model.StreakState
import fr.babeuloula.photoswipe.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Préférences de l'utilisateur (SPEC §5.2). Implémenté dans `:data` sur DataStore.
 *
 * La langue n'y figure pas : elle passe par le réglage de langue par application d'Android
 * (SPEC §4).
 */
@Suppress("TooManyFunctions") // Une lecture et une écriture par réglage enregistré.
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
     * Animations de l'application (PS-146), observées par l'activité et les réglages : la valeur
     * actuelle dès la collecte (`true` si rien n'est enregistré), puis chaque nouvelle valeur. Les
     * couper allège l'application sur un téléphone ancien.
     */
    fun observeAnimationsEnabled(): Flow<Boolean>

    /** Enregistre si les animations de l'application sont actives. */
    suspend fun saveAnimationsEnabled(enabled: Boolean)

    /**
     * Noir pur du thème sombre (PS-176), observé par l'activité et les réglages : la valeur actuelle
     * dès la collecte (`false` si rien n'est enregistré), puis chaque nouvelle valeur. Les fonds
     * passent au noir sur un écran OLED ; le thème clair n'en tient pas compte.
     */
    fun observePureBlack(): Flow<Boolean>

    /** Enregistre si le thème sombre est en noir pur. */
    suspend fun savePureBlack(enabled: Boolean)

    /**
     * Célébration de fin de pile (PS-174), observée par l'activité et les réglages : la valeur
     * actuelle dès la collecte (`false` si rien n'est enregistré), puis chaque nouvelle valeur. Active,
     * des confettis saluent la dernière carte triée de la pile d'un jour.
     */
    fun observeCelebrationEnabled(): Flow<Boolean>

    /** Enregistre si la fin d'une pile est célébrée. */
    suspend fun saveCelebrationEnabled(enabled: Boolean)

    /**
     * « Secouer pour annuler » (PS-175), observé par l'activité et les réglages : la valeur actuelle
     * dès la collecte (`false` si rien n'est enregistré, pour ne rien annuler sans le vouloir dans les
     * transports), puis chaque nouvelle valeur.
     */
    fun observeShakeToUndo(): Flow<Boolean>

    /** Enregistre si secouer le téléphone annule la dernière décision. */
    suspend fun saveShakeToUndo(enabled: Boolean)

    /**
     * Durée de la fenêtre de rattrapage (PS-169), observée par la fenêtre de revue et les réglages :
     * la valeur actuelle dès la collecte ([CatchUpWindow.DEFAULT] si rien n'est enregistré), puis
     * chaque nouvelle valeur.
     */
    fun observeCatchUpWindow(): Flow<CatchUpWindow>

    /** Enregistre la durée de la fenêtre de rattrapage. */
    suspend fun saveCatchUpWindow(window: CatchUpWindow)

    /**
     * Conseil « Gestion des médias » de l'accueil : la valeur actuelle dès la collecte
     * ([ManageMediaTip.DEFAULT] si rien n'est enregistré), puis chaque nouvelle valeur.
     */
    fun observeManageMediaTip(): Flow<ManageMediaTip>

    /** Enregistre l'état du conseil « Gestion des médias ». */
    suspend fun saveManageMediaTip(tip: ManageMediaTip)

    /**
     * Version du tutoriel déjà vue (SPEC §4), [fr.babeuloula.photoswipe.domain.model.Tutorial.NEVER_SEEN]
     * tant qu'il n'a jamais été vu.
     */
    suspend fun tutorialVersionSeen(): Int

    /** Enregistre la version du tutoriel vue. */
    suspend fun saveTutorialVersionSeen(version: Int)

    /** Invitation à noter l'application (PS-121), [RatingInvitation.DEFAULT] tant que rien n'est enregistré. */
    suspend fun ratingInvitation(): RatingInvitation

    /** Enregistre l'invitation à noter l'application. */
    suspend fun saveRatingInvitation(invitation: RatingInvitation)

    /** Série de jours enregistrée (PS-155) ; un état vide tant que l'accueil ne l'a jamais comptée. */
    suspend fun streakState(): StreakState

    /** Enregistre la série de jours [state]. */
    suspend fun saveStreakState(state: StreakState)

    /**
     * Dernière version dont les nouveautés ont été vues (PS-173), écrite `X.Y.Z` ; `null` tant que
     * rien n'est enregistré (première installation, ou version qui ne l'enregistrait pas encore).
     */
    suspend fun whatsNewVersionSeen(): String?

    /** Enregistre la dernière version dont les nouveautés ont été vues. */
    suspend fun saveWhatsNewVersionSeen(version: String)

    /**
     * Vrai si la pile signale les photos peut-être floues ou très sombres (PS-164) ; vrai tant que
     * rien n'est enregistré.
     */
    fun observeQualityHints(): Flow<Boolean>

    /** Enregistre le choix de signaler ou non les photos peut-être ratées. */
    suspend fun saveQualityHints(enabled: Boolean)
}
