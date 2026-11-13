package fr.babeuloula.photoswipe.domain.fixtures

import fr.babeuloula.photoswipe.domain.model.CatchUpWindow
import fr.babeuloula.photoswipe.domain.model.ManageMediaTip
import fr.babeuloula.photoswipe.domain.model.RatingInvitation
import fr.babeuloula.photoswipe.domain.model.ReminderSettings
import fr.babeuloula.photoswipe.domain.model.StreakState
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
 * @param ratingInvitation invitation à noter l'application au départ.
 * @param animationsEnabled animations actives au départ.
 * @param pureBlack noir pur du thème sombre au départ.
 * @param whatsNewVersionSeen dernière version des nouveautés vue au départ, `null` pour aucune.
 * @param catchUpWindow durée de la fenêtre de rattrapage au départ.
 */
class FakeSettingsRepository(
    reminder: ReminderSettings = ReminderSettings.DEFAULT,
    themeMode: ThemeMode = ThemeMode.DEFAULT,
    manageMediaTip: ManageMediaTip = ManageMediaTip.DEFAULT,
    tutorialVersionSeen: Int = Tutorial.NEVER_SEEN,
    ratingInvitation: RatingInvitation = RatingInvitation.DEFAULT,
    animationsEnabled: Boolean = true,
    pureBlack: Boolean = false,
    whatsNewVersionSeen: String? = null,
    catchUpWindow: CatchUpWindow = CatchUpWindow.DEFAULT,
) : SettingsRepository {
    private val storedCatchUpWindow = MutableStateFlow(catchUpWindow)

    /** Durée de la fenêtre de rattrapage enregistrée, pour les vérifications du test. */
    val catchUpWindow: CatchUpWindow get() = storedCatchUpWindow.value

    private val storedAnimations = MutableStateFlow(animationsEnabled)
    private val storedPureBlack = MutableStateFlow(pureBlack)

    /** Noir pur du thème sombre enregistré, pour les vérifications du test. */
    val pureBlack: Boolean get() = storedPureBlack.value

    /** Animations actives enregistrées, pour les vérifications du test. */
    val animationsEnabled: Boolean get() = storedAnimations.value

    /** Invitation à noter l'application enregistrée, pour les vérifications du test. */
    var ratingInvitation: RatingInvitation = ratingInvitation
        private set

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

    override fun observeAnimationsEnabled(): Flow<Boolean> = storedAnimations.asStateFlow()

    override suspend fun saveAnimationsEnabled(enabled: Boolean) {
        storedAnimations.value = enabled
    }

    override fun observePureBlack(): Flow<Boolean> = storedPureBlack.asStateFlow()

    override suspend fun savePureBlack(enabled: Boolean) {
        storedPureBlack.value = enabled
    }

    override fun observeCatchUpWindow(): Flow<CatchUpWindow> = storedCatchUpWindow.asStateFlow()

    override suspend fun saveCatchUpWindow(window: CatchUpWindow) {
        storedCatchUpWindow.value = window
    }

    override fun observeManageMediaTip(): Flow<ManageMediaTip> = storedTip.asStateFlow()

    override suspend fun saveManageMediaTip(tip: ManageMediaTip) {
        storedTip.value = tip
    }

    override suspend fun tutorialVersionSeen(): Int = tutorialVersionSeen

    override suspend fun saveTutorialVersionSeen(version: Int) {
        tutorialVersionSeen = version
    }

    override suspend fun ratingInvitation(): RatingInvitation = ratingInvitation

    override suspend fun saveRatingInvitation(invitation: RatingInvitation) {
        ratingInvitation = invitation
    }

    /** Série de jours enregistrée, pour les vérifications du test. */
    var streakState: StreakState = StreakState()
        private set

    override suspend fun streakState(): StreakState = streakState

    override suspend fun saveStreakState(state: StreakState) {
        streakState = state
    }

    /** Dernière version des nouveautés vue, pour les vérifications du test. */
    var whatsNewVersionSeen: String? = whatsNewVersionSeen
        private set

    override suspend fun whatsNewVersionSeen(): String? = whatsNewVersionSeen

    override suspend fun saveWhatsNewVersionSeen(version: String) {
        whatsNewVersionSeen = version
    }

    private val storedQualityHints = MutableStateFlow(true)

    /** Signalement des photos peut-être ratées enregistré, pour les vérifications du test. */
    val qualityHints: Boolean get() = storedQualityHints.value

    override fun observeQualityHints(): Flow<Boolean> = storedQualityHints.asStateFlow()

    override suspend fun saveQualityHints(enabled: Boolean) {
        storedQualityHints.value = enabled
    }
}
